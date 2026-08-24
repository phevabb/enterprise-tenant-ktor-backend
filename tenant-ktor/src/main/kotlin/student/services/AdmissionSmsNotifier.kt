package student.services



import com.example.academics.repos.setTenantSchema
import com.example.notifications.SmsService
import com.example.tenant.tables.TenantsTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object AdmissionSmsNotifier {

    private val scope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.IO
        )

    fun notifyAsync(
        tenantSchema: String,
        studentName: String,
        contactOfFather: String,
        message: String
    ) {

        if (tenantSchema.isBlank()) {

            println(
                "[AdmissionSmsNotifier] SMS skipped: tenant schema is blank."
            )

            return
        }

        if (contactOfFather.isBlank()) {

            println(
                "[AdmissionSmsNotifier] SMS skipped: father contact is blank."
            )

            return
        }

        if (message.isBlank()) {

            println(
                "[AdmissionSmsNotifier] SMS skipped: admission message is blank."
            )

            return
        }

        scope.launch {

            try {

                val tenantCode =
                    findTenantCodeBySchema(
                        tenantSchema = tenantSchema
                    )

                if (tenantCode.isNullOrBlank()) {

                    println(
                        "[AdmissionSmsNotifier] SMS skipped: tenant code was not found for schema=$tenantSchema"
                    )

                    return@launch
                }

                val result =
                    SmsService.sendAndCharge(
                        tenantCode = tenantCode,
                        recipients = listOf(
                            contactOfFather
                        ),
                        message = message.trim()
                    )

                println(
                    "[AdmissionSmsNotifier] " +
                            "studentName=$studentName, " +
                            "tenantCode=$tenantCode, " +
                            "success=${result.success}, " +
                            "message=${result.message}"
                )

            } catch (e: Exception) {

                println(
                    "[AdmissionSmsNotifier] Failed: ${e.message}"
                )

                e.printStackTrace()
            }
        }
    }

    private fun findTenantCodeBySchema(
        tenantSchema: String
    ): String? {

        return transaction {

            setTenantSchema(
                "public"
            )

            TenantsTable
                .selectAll()
                .firstOrNull { row ->

                    row[
                        TenantsTable.tenantSchema
                    ].equals(
                        tenantSchema,
                        ignoreCase = true
                    )
                }
                ?.get(
                    TenantsTable.tenantCode
                )
                ?.let {
                    normalizeTenantCode(it)
                }
        }
    }

    private fun normalizeTenantCode(
        tenantCode: String
    ): String {

        return tenantCode
            .trim()
            .lowercase()
            .replace(
                Regex("[^a-z0-9_]"),
                ""
            )
    }
}
