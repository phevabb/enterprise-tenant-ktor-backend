package birthday.services

import com.example.tenant.services.TenantRegistryService
import tenant.repository.getAllTenants

object BirthdayCronService {

    fun run() {

        println()
        println("====================================")
        println("BIRTHDAY CRON STARTED")
        println("====================================")

        val tenants = getAllTenants()

        println(
            "Total tenants = ${tenants.size}"
        )

        tenants.forEach { tenant ->

            try {

                println()
                println(
                    "Processing school = ${tenant.schoolName}"
                )

                println(
                    "Tenant code = ${tenant.tenantCode}"
                )

                println(
                    "Tenant schema = ${tenant.tenantSchema}"
                )

                BirthdayService.processBirthdays(
                    tenantSchema =
                        tenant.tenantSchema,

                    tenantCode =
                        tenant.tenantCode,

                    schoolName =
                        tenant.schoolName
                )

                println(
                    "Finished school = ${tenant.schoolName}"
                )

            } catch (e: Exception) {

                println(
                    "FAILED tenant = ${tenant.tenantSchema}"
                )

                println(
                    "Error = ${e.message}"
                )

                e.printStackTrace()
            }
        }

        println()
        println("====================================")
        println("BIRTHDAY CRON FINISHED")
        println("====================================")
    }
}