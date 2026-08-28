package chat.repositories


import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

fun <T> chatTenantTransaction(
    tenantSchema: String,
    block: Transaction.() -> T
): T {
    val normalizedTenantSchema =
        tenantSchema.trim()

    require(
        normalizedTenantSchema.matches(
            Regex(
                "^tenant_[a-zA-Z0-9_]+$"
            )
        )
    ) {
        "Invalid tenant schema: $normalizedTenantSchema"
    }

    return transaction {
        exec(
            """
            SET LOCAL search_path TO "$normalizedTenantSchema", public
            """.trimIndent()
        )

        exec(
            "SHOW search_path"
        ) { resultSet ->
            if (resultSet.next()) {
                println(
                    "[ChatTenantTransaction] " +
                            "searchPath=${resultSet.getString(1)}"
                )
            }
        }

        exec(
            """
            SELECT to_regclass('chat_conversations')
            """.trimIndent()
        ) { resultSet ->
            if (resultSet.next()) {
                println(
                    "[ChatTenantTransaction] " +
                            "chatConversationsTable=" +
                            resultSet.getString(1)
                )
            }
        }

        block()
    }
}