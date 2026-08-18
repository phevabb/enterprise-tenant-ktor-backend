package com.example.notifications

import com.example.academics.repos.setTenantSchema
import com.example.fees.notifications.MnotifySmsRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import sms.dto.SmsSendResult
import sms.tables.SmsCampaignsTable
import sms.tables.SmsSenderIdsTable
import sms.tables.SmsWalletTransactionsTable
import sms.tables.SmsWalletsTable
import java.math.BigDecimal
import java.time.Instant

object SmsService {

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        explicitNulls = false
                    }
                )
            }
        }
    }

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    private val apiKey =
        "CelTN4i2JFPI2ZpknqYl0azod"

    private val endpoint =
        "https://api.mnotify.com/api/sms/quick"

    /**
     * Use this when you want to send SMS without blocking the route.
     *
     * Important:
     * This still checks sender ID, checks SMS balance, sends SMS,
     * and deducts SMS balance only if mNotify succeeds.
     */
    fun sendAsync(
        tenantCode: String,
        phone: String,
        message: String
    ) {

        scope.launch {

            val result =
                sendAndCharge(
                    tenantCode = tenantCode,
                    recipients = listOf(phone),
                    message = message
                )

            println(
                "SMS async result: $result"
            )
        }
    }

    /**
     * Preferred method for wallet-based SMS sending.
     */
    suspend fun sendAndCharge(
        tenantCode: String,
        recipients: List<String>,
        message: String
    ): SmsSendResult {

        if (apiKey.isBlank()) {

            return SmsSendResult(
                success = false,
                message = "SMS disabled: API key not configured."
            )
        }

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        val cleanedRecipients =
            recipients
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (normalizedTenantCode.isBlank()) {

            return SmsSendResult(
                success = false,
                message = "Tenant code is required."
            )
        }

        if (cleanedRecipients.isEmpty()) {

            return SmsSendResult(
                success = false,
                message = "No valid SMS recipients provided."
            )
        }

        if (message.isBlank()) {

            return SmsSendResult(
                success = false,
                message = "SMS message cannot be empty."
            )
        }

        val segmentCount =
            countSmsSegments(
                message
            )

        val recipientCount =
            cleanedRecipients.size

        val totalSmsCost =
            segmentCount * recipientCount

        val preCheck =
            validateSenderAndWallet(
                tenantCode = normalizedTenantCode,
                totalSmsCost = totalSmsCost
            )

        if (!preCheck.success) {

            println(
                "SMS skipped before mNotify. tenantCode=$normalizedTenantCode, reason=${preCheck.message}"
            )

            return preCheck
        }

        val approvedSenderId =
            preCheck.senderId
                ?: return SmsSendResult(
                    success = false,
                    message = "Approved sender ID could not be resolved. SMS was not sent."
                )

        val payload =
            MnotifySmsRequest(
                recipient = cleanedRecipients,
                sender = approvedSenderId,
                message = message
            )

        return try {

            val response: HttpResponse =
                client.post(
                    "$endpoint?key=$apiKey"
                ) {
                    contentType(
                        ContentType.Application.Json
                    )

                    setBody(
                        payload
                    )
                }

            val bodyText =
                response.bodyAsText()

            println(
                "mNotify SMS status=${response.status} body=$bodyText"
            )

            if (!response.status.isSuccess()) {

                saveFailedCampaign(
                    tenantCode = normalizedTenantCode,
                    senderId = approvedSenderId,
                    message = message,
                    recipientCount = recipientCount,
                    segmentCount = segmentCount,
                    totalSmsUsed = totalSmsCost,
                    failureReason = bodyText
                )

                return SmsSendResult(
                    success = false,
                    message = "SMS provider rejected the message.",
                    senderId = approvedSenderId,
                    recipientCount = recipientCount,
                    segmentCount = segmentCount,
                    totalSmsUsed = totalSmsCost,
                    providerResponse = bodyText
                )
            }

            debitWalletAfterSuccessfulSend(
                tenantCode = normalizedTenantCode,
                senderId = approvedSenderId,
                message = message,
                recipientCount = recipientCount,
                segmentCount = segmentCount,
                totalSmsCost = totalSmsCost,
                providerResponse = bodyText
            )

        } catch (e: Exception) {

            e.printStackTrace()

            saveFailedCampaign(
                tenantCode = normalizedTenantCode,
                senderId = approvedSenderId,
                message = message,
                recipientCount = recipientCount,
                segmentCount = segmentCount,
                totalSmsUsed = totalSmsCost,
                failureReason = e.message ?: "Unknown SMS sending error."
            )

            SmsSendResult(
                success = false,
                message = e.message ?: "Unable to send SMS.",
                senderId = approvedSenderId,
                recipientCount = recipientCount,
                segmentCount = segmentCount,
                totalSmsUsed = totalSmsCost
            )
        }

    }

    /**
     * Checks:
     * - approved sender ID exists
     * - wallet exists
     * - sms balance is enough
     */
    private fun validateSenderAndWallet(
        tenantCode: String,
        totalSmsCost: Int
    ): SmsSendResult {

        return transaction {

            setTenantSchema(
                "public"
            )

            val approvedSender =
                SmsSenderIdsTable
                    .selectAll()
                    .where {
                        (SmsSenderIdsTable.tenantCode eq tenantCode) and
                                (SmsSenderIdsTable.status eq "approved")
                    }
                    .limit(1)
                    .singleOrNull()

            if (approvedSender == null) {

                return@transaction SmsSendResult(
                    success = false,
                    message = "No approved sender ID found. SMS was not sent."
                )
            }

            val senderId =
                approvedSender[SmsSenderIdsTable.senderId]

            val wallet =
                SmsWalletsTable
                    .selectAll()
                    .where {
                        SmsWalletsTable.tenantCode eq tenantCode
                    }
                    .limit(1)
                    .singleOrNull()

            if (wallet == null) {

                return@transaction SmsSendResult(
                    success = false,
                    message = "SMS wallet not found. SMS was not sent.",
                    senderId = senderId
                )
            }

            val smsBalance =
                wallet[SmsWalletsTable.smsBalance]

            if (smsBalance < totalSmsCost) {

                return@transaction SmsSendResult(
                    success = false,
                    message = "Insufficient SMS balance. Required $totalSmsCost SMS units, available $smsBalance. SMS was not sent.",
                    senderId = senderId,
                    totalSmsUsed = totalSmsCost,
                    smsBalanceBefore = smsBalance,
                    smsBalanceAfter = smsBalance
                )
            }

            SmsSendResult(
                success = true,
                message = "SMS pre-check successful.",
                senderId = senderId,
                totalSmsUsed = totalSmsCost,
                smsBalanceBefore = smsBalance,
                smsBalanceAfter = smsBalance - totalSmsCost
            )
        }
    }

    /**
     * Deducts SMS balance after mNotify succeeds.
     *
     * Uses conditional update so balance cannot go negative
     * if two SMS sends happen at the same time.
     */
    private fun debitWalletAfterSuccessfulSend(
        tenantCode: String,
        senderId: String,
        message: String,
        recipientCount: Int,
        segmentCount: Int,
        totalSmsCost: Int,
        providerResponse: String
    ): SmsSendResult {

        val now =
            Instant.now().toString()

        val reference =
            "sms_send_${tenantCode}_${System.currentTimeMillis()}"

        return transaction {

            setTenantSchema(
                "public"
            )

            val wallet =
                SmsWalletsTable
                    .selectAll()
                    .where {
                        SmsWalletsTable.tenantCode eq tenantCode
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: return@transaction SmsSendResult(
                        success = false,
                        message = "SMS wallet not found after provider success.",
                        senderId = senderId
                    )

            val walletId =
                wallet[SmsWalletsTable.id]

            val oldCashBalance =
                wallet[SmsWalletsTable.cashBalance]

            val oldSmsBalance =
                wallet[SmsWalletsTable.smsBalance]

            val oldTotalSmsUsed =
                wallet[SmsWalletsTable.totalSmsUsed]

            val updatedRows =
                SmsWalletsTable.update(
                    {
                        (SmsWalletsTable.id eq walletId) and
                                (SmsWalletsTable.smsBalance greaterEq totalSmsCost)
                    }
                ) {

                    it[smsBalance] =
                        oldSmsBalance - totalSmsCost

                    it[totalSmsUsed] =
                        oldTotalSmsUsed + totalSmsCost

                    it[updatedAt] =
                        now
                }

            if (updatedRows <= 0) {

                saveFailedCampaign(
                    tenantCode = tenantCode,
                    senderId = senderId,
                    message = message,
                    recipientCount = recipientCount,
                    segmentCount = segmentCount,
                    totalSmsUsed = totalSmsCost,
                    failureReason = "Wallet balance changed before debit could complete."
                )

                return@transaction SmsSendResult(
                    success = false,
                    message = "SMS was sent but wallet debit failed because balance changed. Please review manually.",
                    senderId = senderId,
                    recipientCount = recipientCount,
                    segmentCount = segmentCount,
                    totalSmsUsed = totalSmsCost,
                    smsBalanceBefore = oldSmsBalance,
                    smsBalanceAfter = oldSmsBalance,
                    providerResponse = providerResponse
                )
            }

            val newSmsBalance =
                oldSmsBalance - totalSmsCost

            val campaignId =
                SmsCampaignsTable.insert {

                    it[SmsCampaignsTable.tenantCode] =
                        tenantCode

                    it[SmsCampaignsTable.senderId] =
                        senderId

                    it[SmsCampaignsTable.message] =
                        message

                    it[SmsCampaignsTable.recipientCount] =
                        recipientCount

                    it[SmsCampaignsTable.segmentCount] =
                        segmentCount

                    it[SmsCampaignsTable.totalCreditsUsed] =
                        totalSmsCost

                    it[SmsCampaignsTable.status] =
                        "sent"

                    it[SmsCampaignsTable.providerCampaignId] =
                        null

                    it[SmsCampaignsTable.createdAt] =
                        now
                } get SmsCampaignsTable.id

            SmsWalletTransactionsTable.insert {

                it[SmsWalletTransactionsTable.tenantCode] =
                    tenantCode

                it[type] =
                    "sms_debit"

                it[amountCash] =
                    null

                it[amountSms] =
                    totalSmsCost

                it[cashBalanceBefore] =
                    oldCashBalance

                it[cashBalanceAfter] =
                    oldCashBalance

                it[smsBalanceBefore] =
                    oldSmsBalance

                it[smsBalanceAfter] =
                    newSmsBalance

                it[description] =
                    "SMS campaign sent"

                it[SmsWalletTransactionsTable.reference] =
                    reference

                it[createdAt] =
                    now
            }

            SmsSendResult(
                success = true,
                message = "SMS sent successfully.",
                senderId = senderId,
                recipientCount = recipientCount,
                segmentCount = segmentCount,
                totalSmsUsed = totalSmsCost,
                smsBalanceBefore = oldSmsBalance,
                smsBalanceAfter = newSmsBalance,
                providerResponse = providerResponse
            )
        }
    }

    private fun saveFailedCampaign(
        tenantCode: String,
        senderId: String,
        message: String,
        recipientCount: Int,
        segmentCount: Int,
        totalSmsUsed: Int,
        failureReason: String
    ) {

        val now =
            Instant.now().toString()

        transaction {

            setTenantSchema(
                "public"
            )

            SmsCampaignsTable.insert {

                it[SmsCampaignsTable.tenantCode] =
                    tenantCode

                it[SmsCampaignsTable.senderId] =
                    senderId

                it[SmsCampaignsTable.message] =
                    message

                it[SmsCampaignsTable.recipientCount] =
                    recipientCount

                it[SmsCampaignsTable.segmentCount] =
                    segmentCount

                it[SmsCampaignsTable.totalCreditsUsed] =
                    totalSmsUsed

                it[SmsCampaignsTable.status] =
                    "failed"

                it[SmsCampaignsTable.providerCampaignId] =
                    null

                it[SmsCampaignsTable.createdAt] =
                    now
            }

            println(
                "Failed SMS campaign saved. tenantCode=$tenantCode, senderId=$senderId, reason=${failureReason.take(500)}"
            )
        }
    }

    fun countSmsSegments(
        message: String
    ): Int {

        if (message.isBlank()) {
            return 0
        }

        val limit =
            if (isGsmMessage(message)) {
                160
            } else {
                70
            }

        return kotlin.math.ceil(
            message.length.toDouble() / limit.toDouble()
        ).toInt()
    }

    private fun isGsmMessage(
        message: String
    ): Boolean {

        return message.all { character ->
            character.code in 0..127
        }
    }

    private fun normalizeTenantCode(
        tenantCode: String
    ): String {

        return tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )
    }

    fun close() {

        runCatching {
            client.close()
        }

        scope.cancel()
    }}