package com.example.commands


import com.example.academics.repos.setTenantSchema
import com.example.config.DatabaseFactory
import com.example.student.dtos.requests.CreateUserPart
import com.example.student.services.StudentService
import java.io.File
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.tables.NewGradeClassTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ImportStudentsFromCsv {

    private fun loadClassMap(
        tenantSchema: String
    ): Map<String, Int> {

        return transaction {
            setTenantSchema(tenantSchema)


            NewGradeClassTable
                .selectAll()
                .associate {
                    it[NewGradeClassTable.name]
                        .trim()
                        .lowercase() to it[NewGradeClassTable.id].value
                }
        }
    }

    fun run(tenantSchema: String) {

        DatabaseFactory.init()

        val classMap = loadClassMap(tenantSchema)

        val file = File("student.csv")
        require(file.exists()) { "student.csv not found in project root" }

        val lines = file.readLines()

        println("===== IMPORT STUDENTS FROM CSV =====")
        println("tenantSchema = $tenantSchema")

        lines
            .drop(1)
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->

                val cols = line.split(",").map { it.trim() }

                val fullName = cols[0]
                val classText = cols[1].lowercase()

                val contactOfFather = cols[2]
                val contactOfMother = cols[3]

                val isDiscounted =
                    cols[4].equals("yes", ignoreCase = true)

                val classId = classMap[classText]
                    ?: error(
                        "Class '$classText' not found in tenant '$tenantSchema'"
                    )

                StudentService.createStudent(
                    tenantSchema = tenantSchema,
                    request = CreateStudentRequest(
                        user = CreateUserPart(
                            fullName = fullName,
                            role = "student"
                        ),
                        currentNewGradeClassId = classId,
                        family = null,
                        isDiscountedStudent = isDiscounted,
                        contactOfFather = contactOfFather,
                        contactOfMother = contactOfMother
                    )
                )

                println(
                    "Imported: $fullName -> $classText ($classId)"
                )
            }

        println("✅ Import completed for $tenantSchema")
    }
}
object TenantTransaction {

    fun <T> execute(
        tenantSchema: String,
        block: () -> T
    ): T = transaction {



        setTenantSchema(tenantSchema)


        block()
    }
}


//
//fun main() {
//    val tenant = call.currentTenant()
//    tenantSchema = tenant.tenantSchema
//
//    ImportStudentsFromCsv.run(tenantSchema)
//}
