package birthday.models

import java.time.LocalDate

data class BirthdayStudent(
    val studentId: Int,
    val accountId: Int,
    val fullName: String,
    val dateOfBirth: LocalDate?,
    val contactOfFather: String,
    val contactOfMother: String?,
)