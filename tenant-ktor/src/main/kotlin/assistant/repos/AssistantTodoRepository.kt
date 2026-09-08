package assistant.repos

import assistant.requests.AssistantTodoRequest
import assistant.responses.AssistantTodoResponse
import assistant.tables.AssistantTodoTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

object AssistantTodoRepository {


    fun update(
        id: Int,
        request: AssistantTodoRequest
    ): Boolean {

        return AssistantTodoTable.update(
            {
                AssistantTodoTable.id eq id
            }
        ) {

            it[title] =
                request.title

            it[description] =
                request.description

            it[dueDate] =
                request.dueDate

            it[updatedAt] =
                LocalDateTime.now().toString()

        } > 0
    }



    fun getAll(): List<AssistantTodoResponse> {

        return AssistantTodoTable
            .selectAll()
            .orderBy(
                AssistantTodoTable.id,
                SortOrder.DESC
            )
            .map { row ->

                AssistantTodoResponse(
                    id =
                        row[AssistantTodoTable.id].value,

                    title =
                        row[AssistantTodoTable.title],

                    description =
                        row[AssistantTodoTable.description],

                    completed =
                        row[AssistantTodoTable.completed],

                    dueDate =
                        row[AssistantTodoTable.dueDate],

                    createdAt =
                        row[AssistantTodoTable.createdAt],

                    updatedAt =
                        row[AssistantTodoTable.updatedAt]
                )
            }
    }

    fun getById(
        id: Int
    ): AssistantTodoResponse? {

        return AssistantTodoTable
            .selectAll()
            .where {
                AssistantTodoTable.id eq id
            }
            .singleOrNull()
            ?.let { row ->

                AssistantTodoResponse(
                    id =
                        row[AssistantTodoTable.id].value,

                    title =
                        row[AssistantTodoTable.title],

                    description =
                        row[AssistantTodoTable.description],

                    completed =
                        row[AssistantTodoTable.completed],

                    dueDate =
                        row[AssistantTodoTable.dueDate],

                    createdAt =
                        row[AssistantTodoTable.createdAt],

                    updatedAt =
                        row[AssistantTodoTable.updatedAt]
                )
            }
    }

    fun create(
        request: AssistantTodoRequest
    ): Int {

        return AssistantTodoTable
            .insertAndGetId {

                it[title] =
                    request.title

                it[description] =
                    request.description

                it[dueDate] =
                    request.dueDate

                it[createdAt] =
                    LocalDateTime.now().toString()

                it[updatedAt] =
                    LocalDateTime.now().toString()
            }
            .value
    }

    fun toggleCompleted(
        id: Int
    ): Boolean {

        val todo =
            AssistantTodoTable
                .selectAll()
                .where {
                    AssistantTodoTable.id eq id
                }
                .singleOrNull()
                ?: return false

        val current =
            todo[
                AssistantTodoTable.completed
            ]

        return AssistantTodoTable.update(
            {
                AssistantTodoTable.id eq id
            }
        ) {

            it[completed] =
                !current

            it[updatedAt] =
                LocalDateTime.now().toString()

        } > 0
    }

    fun delete(
        id: Int
    ): Boolean {

        return AssistantTodoTable.deleteWhere {
            AssistantTodoTable.id eq id
        } > 0
    }
}