package birthday.services

import birthday.repos.BirthdayMessageLogRepository
import birthday.repos.BirthdayRepository
import com.example.notifications.SmsService
import com.example.tenant.tenantTransaction
import java.time.LocalDate

object BirthdayService {

    fun processBirthdays(
        tenantSchema: String,
        tenantCode: String,
        schoolName: String
    ) {

        tenantTransaction(
            tenantSchema
        ) {

            val settings =
                BirthdaySettingsService.get(
                    tenantSchema
                )

            if (settings == null) {

                println(
                    "Birthday settings not found"
                )

                return@tenantTransaction
            }

            if (!settings.enabled) {

                println(
                    "Birthday messages are disabled"
                )

                return@tenantTransaction
            }

            val today =
                LocalDate.now()

            println("====================================")
            println("BIRTHDAY JOB STARTED")
            println("Tenant Schema = $tenantSchema")
            println("School Name = $schoolName")
            println("Today = $today")
            println("====================================")

            val students =
                BirthdayRepository
                    .getStudentsForBirthdayCheck()

            println(
                "Total students loaded = ${students.size}"
            )

            students.forEach { student ->

                val dob =
                    student.dateOfBirth

                println(
                    "Checking ${student.fullName}, DOB=$dob"
                )

                if (
                    dob != null &&
                    dob.monthValue == today.monthValue &&
                    dob.dayOfMonth == today.dayOfMonth
                ) {

                    val currentYear =
                        today.year

                    if (
                        BirthdayMessageLogRepository.alreadySent(
                            studentId = student.studentId,
                            year = currentYear
                        )
                    ) {

                        println(
                            "Birthday SMS already sent for ${student.fullName} in $currentYear"
                        )

                        return@forEach
                    }

                    val message =
                        settings.birthdayMessage
                            .replace(
                                "{student_name}",
                                student.fullName
                            )
                            .replace(
                                "{school_name}",
                                schoolName
                            )

                    val recipients =
                        listOfNotNull(
                            student.contactOfFather,
                            student.contactOfMother
                        )
                            .distinct()

                    println(
                        "🎂 BIRTHDAY FOUND => ${student.fullName}"
                    )

                    println(
                        "Student ID => ${student.studentId}"
                    )

                    println(
                        "Father Contact => ${student.contactOfFather}"
                    )

                    println(
                        "Mother Contact => ${student.contactOfMother}"
                    )

                    println(
                        "DOB => $dob"
                    )

                    println(
                        "Recipients => $recipients"
                    )

                    println(
                        "Message => $message"
                    )

                    recipients.forEach { phone ->

                        println(
                            "SMS WOULD BE SENT TO => $phone"
                        )


                        SmsService.sendAsync(
                            phone = phone,
                            message = message,
                            tenantCode= tenantCode,
                        )
                    }

                    BirthdayMessageLogRepository.save(
                        studentId = student.studentId,
                        year = currentYear
                    )

                    println(
                        "Birthday log saved for ${student.fullName}"
                    )
                }
            }

            println("====================================")
            println("BIRTHDAY JOB FINISHED")
            println("====================================")
        }
    }
}