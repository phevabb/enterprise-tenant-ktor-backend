package sms.services


import com.example.student.repos.setTenantSchema
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import sms.dto.RequestSenderIdRequest
import sms.dto.SenderIdResponse
import sms.tables.SmsSenderIdsTable
import java.time.Instant





import com.example.academics.repos.setTenantSchema

import org.jetbrains.exposed.sql.ResultRow



class SenderIdAlreadyExistsException(
    message: String
) : RuntimeException(message)

object SmsSenderIdService {

    fun deleteSenderIdForTenant(
        id: Int,
        tenantCode: String
    ): Boolean {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        require(id > 0) {
            "Invalid sender ID request id."
        }

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        return transaction {



            val deletedRows =
                SmsSenderIdsTable.deleteWhere {
                    (SmsSenderIdsTable.id eq id) and
                            (SmsSenderIdsTable.tenantCode eq normalizedTenantCode)
                }

            deletedRows > 0
        }
    }

    fun requestSenderId(
        request: RequestSenderIdRequest
    ): SenderIdResponse {

        val normalizedTenantCode =
            normalizeTenantCode(
                request.tenantCode
            )

        val cleanedSenderId =
            request.senderId
                .trim()
                .replace("\\s+".toRegex(), "")
                .take(11)

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        require(
            cleanedSenderId.isNotBlank()
        ) {
            "Sender ID is required."
        }

        require(
            cleanedSenderId.length <= 11
        ) {
            "Sender ID must not be more than 11 characters."
        }

        val now =
            Instant.now().toString()

        return transaction {

            /*
             * SmsSenderIdsTable and TenantsTable are public tables.
             */


            val tenantRow =
                TenantsTable
                    .selectAll()
                    .where {
                        TenantsTable.tenantCode eq normalizedTenantCode
                    }
                    .limit(1)
                    .singleOrNull()
                    ?: error(
                        "Tenant not found for code: $normalizedTenantCode"
                    )

            val resolvedSchoolName =
                tenantRow[TenantsTable.schoolName]

            val existingPendingOrApproved =
                SmsSenderIdsTable
                    .selectAll()
                    .where {
                        (SmsSenderIdsTable.tenantCode eq normalizedTenantCode) and
                                (
                                        (SmsSenderIdsTable.status eq "pending") or
                                                (SmsSenderIdsTable.status eq "approved")
                                        )
                    }
                    .limit(1)
                    .singleOrNull()

            if (existingPendingOrApproved != null) {

                throw SenderIdAlreadyExistsException(
                    "Sender ID already exists. Delete previous one to create a new one."
                )
            }

            val insertedId =
                SmsSenderIdsTable.insert {

                    it[tenantCode] =
                        normalizedTenantCode

                    it[schoolName] =
                        resolvedSchoolName

                    it[senderId] =
                        cleanedSenderId

                    it[status] =
                        "pending"

                    it[rejectionReason] =
                        null

                    it[requestedAt] =
                        now

                    it[approvedAt] =
                        null

                    it[createdAt] =
                        now

                    it[updatedAt] =
                        now

                } get SmsSenderIdsTable.id

            findByIdInternal(
                insertedId
            ) ?: error(
                "Sender ID request created but could not be retrieved."
            )
        }
    }

    fun findById(
        id: Int
    ): SenderIdResponse? {

        return transaction {



            findByIdInternal(
                id
            )
        }
    }

    fun findLatestForTenant(
        tenantCode: String
    ): SenderIdResponse? {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        return transaction {



            SmsSenderIdsTable
                .selectAll()
                .where {
                    SmsSenderIdsTable.tenantCode eq normalizedTenantCode
                }
                .orderBy(
                    SmsSenderIdsTable.id,
                    SortOrder.DESC
                )
                .limit(1)
                .singleOrNull()
                ?.toSenderIdResponse()
        }
    }

    private fun findByIdInternal(
        id: Int
    ): SenderIdResponse? {

        return SmsSenderIdsTable
            .selectAll()
            .where {
                SmsSenderIdsTable.id eq id
            }
            .singleOrNull()
            ?.toSenderIdResponse()
    }

    private fun ResultRow.toSenderIdResponse(): SenderIdResponse {

        return SenderIdResponse(
            id = this[SmsSenderIdsTable.id],
            tenantCode = this[SmsSenderIdsTable.tenantCode],
            schoolName = this[SmsSenderIdsTable.schoolName],
            senderId = this[SmsSenderIdsTable.senderId],
            status = this[SmsSenderIdsTable.status],
            rejectionReason = this[SmsSenderIdsTable.rejectionReason],
            requestedAt = this[SmsSenderIdsTable.requestedAt],
            approvedAt = this[SmsSenderIdsTable.approvedAt],
            createdAt = this[SmsSenderIdsTable.createdAt],
            updatedAt = this[SmsSenderIdsTable.updatedAt]
        )
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


