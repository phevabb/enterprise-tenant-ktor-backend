package sms.services

import com.example.academics.repos.setTenantSchema
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import sms.dto.ClientSenderIdResponse
import sms.tables.SmsSenderIdsTable

object ClientSenderIdService {

    fun getLatestSenderIdByTenantCode(
        tenantCode: String
    ): ClientSenderIdResponse {

        val normalizedTenantCode =
            normalizeTenantCode(
                tenantCode
            )

        require(
            normalizedTenantCode.isNotBlank()
        ) {
            "Tenant code is required."
        }

        return transaction {

            setTenantSchema(
                "public"
            )

            val senderIdRow =
                SmsSenderIdsTable
                    .selectAll()
                    .filter { row ->

                        normalizeTenantCode(
                            row[SmsSenderIdsTable.tenantCode]
                        ) == normalizedTenantCode
                    }
                    .maxByOrNull { row ->
                        row[SmsSenderIdsTable.id]
                    }

            if (senderIdRow == null) {

                return@transaction ClientSenderIdResponse(
                    available = false,
                    tenantCode = normalizedTenantCode,
                    status = "not_requested"
                )
            }

            ClientSenderIdResponse(
                available = true,
                id = senderIdRow[SmsSenderIdsTable.id],
                tenantCode = senderIdRow[SmsSenderIdsTable.tenantCode],
                schoolName = senderIdRow[SmsSenderIdsTable.schoolName],
                senderId = senderIdRow[SmsSenderIdsTable.senderId],
                status = senderIdRow[SmsSenderIdsTable.status],
                rejectionReason = senderIdRow[SmsSenderIdsTable.rejectionReason],
                requestedAt = senderIdRow[SmsSenderIdsTable.requestedAt],
                approvedAt = senderIdRow[SmsSenderIdsTable.approvedAt]
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
}