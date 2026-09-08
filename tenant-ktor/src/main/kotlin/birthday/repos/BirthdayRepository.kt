package birthday.repos

import birthday.models.BirthdayStudent
import com.example.account.AccountTable
import com.example.student.StudentsTable
import org.jetbrains.exposed.sql.selectAll

object BirthdayRepository {

    fun getStudentsForBirthdayCheck():
            List<BirthdayStudent> {

        return StudentsTable
            .innerJoin(AccountTable)
            .selectAll()
            .map { row ->

                BirthdayStudent(
                    studentId =
                        row[StudentsTable.id].value,

                    accountId =
                        row[AccountTable.id].value,

                    fullName =
                        row[AccountTable.fullName],

                    dateOfBirth =
                        row[AccountTable.dateOfBirth],

                    contactOfFather =
                        row[StudentsTable.contactOfFather],

                    contactOfMother =
                        row[StudentsTable.contactOfMother]
                )
            }
    }
}