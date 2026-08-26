package chat.database

import chat.tables.ChatConversationsTable
import chat.tables.ChatMessagesTable
import com.example.student.repos.setTenantSchema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun createChatTables(
    tenantSchema: String
) {
    require(tenantSchema.isNotBlank()) {
        "Tenant schema is required."
    }

    transaction {
        setTenantSchema(
            tenantSchema
        )

        SchemaUtils.createMissingTablesAndColumns(
            ChatConversationsTable,
            ChatMessagesTable
        )
    }
}


fun initializeTenantChatTables(
    tenantSchema: String
) {
    transaction {
        setTenantSchema(
            tenantSchema
        )

        SchemaUtils.createMissingTablesAndColumns(
            ChatConversationsTable,
            ChatMessagesTable
        )
    }

    println(
        "[ChatSchema] Chat tables initialized: " +
                "tenantSchema=$tenantSchema"
    )
}