package superadmin.service



import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import sms.dto.SenderIdResponse
import sms.tables.SmsSenderIdsTable
import java.time.Instant

object SmsSenderIdService {

    fun findAll(): List<SenderIdResponse> {

        return transaction {

            SmsSenderIdsTable
                .selectAll()
                .orderBy(
                    SmsSenderIdsTable.id,
                    SortOrder.DESC
                )
                .map { row ->
                    row.toSenderIdResponse()
                }
        }
    }

    fun approveSenderId(
        id: Int
    ): SenderIdResponse {

        require(id > 0) {
            "Invalid sender ID request id."
        }

        val now =
            Instant.now().toString()

        return transaction {

            val updatedRows =
                SmsSenderIdsTable.update(
                    {
                        SmsSenderIdsTable.id eq id
                    }
                ) {
                    it[status] = "approved"
                    it[approvedAt] = now
                    it[rejectionReason] = null
                    it[updatedAt] = now
                }

            if (updatedRows <= 0) {
                error("Sender ID request not found.")
            }

            findByIdInternal(id)
                ?: error("Sender ID request approved but could not be retrieved.")
        }
    }

    fun rejectSenderId(
        id: Int,
        rejectionReason: String
    ): SenderIdResponse {

        require(id > 0) {
            "Invalid sender ID request id."
        }

        require(rejectionReason.isNotBlank()) {
            "Rejection reason is required."
        }

        val now =
            Instant.now().toString()

        return transaction {

            val updatedRows =
                SmsSenderIdsTable.update(
                    {
                        SmsSenderIdsTable.id eq id
                    }
                ) {
                    it[status] = "rejected"
                    it[SmsSenderIdsTable.rejectionReason] =
                        rejectionReason.trim()
                    it[approvedAt] = null
                    it[updatedAt] = now
                }

            if (updatedRows <= 0) {
                error("Sender ID request not found.")
            }

            findByIdInternal(id)
                ?: error("Sender ID request rejected but could not be retrieved.")
        }
    }

    fun deleteSenderId(
        id: Int
    ): Boolean {

        require(id > 0) {
            "Invalid sender ID request id."
        }

        return transaction {

            val deletedRows =
                SmsSenderIdsTable.deleteWhere {
                    SmsSenderIdsTable.id eq id
                }

            deletedRows > 0
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

    private fun org.jetbrains.exposed.sql.ResultRow.toSenderIdResponse(): SenderIdResponse {

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
}