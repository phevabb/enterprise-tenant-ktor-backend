package tenant.repository

import com.example.academics.repos.setTenantSchema
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object GradeClassStudentCountRepository {

    fun countStudentsByClass(
        tenantSchema: String
    ): Map<Int, Int> {

        require(
            tenantSchema.isNotBlank()
        ) {
            "Tenant schema is required."
        }

        return transaction {

            setTenantSchema(
                tenantSchema
            )

            val classIds =
                StudentsTable
                    .selectAll()
                    .where {
                        StudentsTable.isGraduated eq false
                    }
                    .mapNotNull { row ->

                        row[
                            StudentsTable.currentNewGradeClass
                        ]?.value
                    }

            classIds
                .groupingBy { classId ->
                    classId
                }
                .eachCount()
        }
    }
}