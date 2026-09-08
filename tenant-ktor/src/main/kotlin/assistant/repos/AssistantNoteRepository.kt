package assistant.repos


import assistant.requests.AssistantNoteRequest
import assistant.responses.AssistantNoteResponse
import assistant.tables.AssistantNotesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

object AssistantNoteRepository {

    fun getAll(): List<AssistantNoteResponse> {

        return AssistantNotesTable
            .selectAll()
            .map { row ->

                AssistantNoteResponse(
                    id =
                        row[AssistantNotesTable.id]
                            .value,

                    title =
                        row[AssistantNotesTable.title],

                    content =
                        row[AssistantNotesTable.content],

                    createdAt =
                        row[AssistantNotesTable.createdAt],

                    updatedAt =
                        row[AssistantNotesTable.updatedAt]
                )
            }
    }

    fun getById(
        id: Int
    ): ResultRow? {

        return AssistantNotesTable
            .selectAll()
            .where {
                AssistantNotesTable.id eq id
            }
            .singleOrNull()
    }

    fun create(
        request: AssistantNoteRequest
    ): Int {

        return AssistantNotesTable
            .insertAndGetId {

                it[title] =
                    request.title

                it[content] =
                    request.content

                it[createdAt] =
                    LocalDateTime.now().toString()

                it[updatedAt] =
                    LocalDateTime.now().toString()
            }
            .value
    }

    fun update(
        id: Int,
        request: AssistantNoteRequest
    ): Boolean {

        return AssistantNotesTable.update(
            {
                AssistantNotesTable.id eq id
            }
        ) {

            it[title] =
                request.title

            it[content] =
                request.content

            it[updatedAt] =
                LocalDateTime.now().toString()
        } > 0
    }

    fun delete(
        id: Int
    ): Boolean {

        return AssistantNotesTable.deleteWhere {
            AssistantNotesTable.id eq id
        } > 0
    }
}