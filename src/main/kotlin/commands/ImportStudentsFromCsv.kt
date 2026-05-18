package com.example.commands



import com.example.config.DatabaseFactory
import com.example.student.dtos.requests.CreateStudentRequest
import com.example.student.dtos.requests.CreateUserPart
import com.example.student.services.StudentService
import java.io.File

object ImportStudentsFromCsv {

    // ✅ Mapping from CSV text → DB ID
    private val classMap: Map<String, Int> = mapOf(
        "kg 2" to 23,
        "kg 1" to 22,
        "nursery 2" to 21,
        "nursery 1" to 20,
        "creche" to 19,
        "class 3" to 18,
        "class 1" to 17,
        "class 6" to 15,
        "class 4" to 12,
        "class 2" to 10,
        "class 5" to 1
    )
    fun run() {
        DatabaseFactory.init()

        val file = File("student.csv")
        require(file.exists()) { "student.csv not found in project root" }

        val lines = file.readLines()
        require(lines.size > 1) { "CSV has no data rows" }

        lines
            .drop(1) // skip header
            .filter { it.isNotBlank() }
            .forEachIndexed { index, line ->

                val cols = line.split(",").map { it.trim() }
                require(cols.size >= 5) {
                    "Invalid CSV at line ${index + 2}"
                }

                val fullName = cols[0]
                val classText = cols[1].lowercase()
                val contactOfFather = cols[2]
                val contactOfMother = cols[3]
                val isDiscounted = cols[4].equals("yes", ignoreCase = true)

                val classId = classMap[classText]
                    ?: error("Unknown class '$classText' at line ${index + 2}")

                val request = CreateStudentRequest(
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

                StudentService.createStudent(request)
            }

        println("✅ Students imported successfully from student.csv")
    }
}


fun main() {
    ImportStudentsFromCsv.run()
}