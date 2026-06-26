package com.example.tenant



import kotlinx.serialization.Serializable
import com.example.academics.tables.SubjectsTable
import com.example.admin.dtos.requests.CreateAdminRequest
import com.example.admin.tables.AdminTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.PaymentTable
import com.example.fees.tables.ReceiptsTable
import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.tenant.services.AdminBootstrapService
import com.example.principal.dtos.requests.CreateUserPart
import com.example.principal.dtos.responses.BootstrapPrincipalResponse
import com.example.principal.service.PrincipalBootstrapService
import com.example.staff.tables.StaffTable
import com.example.student.StudentsTable
import com.example.student.repos.AcademicYearRepository
import com.example.student.repos.setTenantSchema
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import com.example.tenant.dto.requests.CreateTenantRequest
import com.example.tenant.dto.response.CreateTenantResponse
import com.example.tenant.services.TenantSchemaService
import com.example.tenant.tables.TenantFeaturesTable

import com.example.tenant.tables.TenantsTable

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import com.example.admin.dtos.requests.CreateUserPart as CreateAdminUserPart

fun Application.tenantAdminModule() {
    routing {
        post("/internal/tenants/create") {
            try {
                val request = call.receive<CreateTenantRequest>()
                val academicYearName = request.academicCalendar.academicYearName
                val response = createTenant(request)


                val tenantSchema = response.tenantSchema


                println("🚧 Creating academic year...")
                AcademicYearRepository.create(
                    tenantSchema = tenantSchema,
                    name = academicYearName
                )
                println("✅ Academic year created successfully")

                println("🎉 ALL OPERATIONS COMPLETED SUCCESSFULLY")

                call.respond(HttpStatusCode.Created, response)

                println("========== END /internal/tenants/create ==========")

            } catch (e: IllegalArgumentException) {
                println("❌ IllegalArgumentException occurred")
                println("Message: ${e.message}")
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )

            } catch (e: Exception) {
                println("💥 Unexpected Exception occurred")
                println("Message: ${e.message}")
                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unable to create tenant"))
                )
            }
        }




        get("/api/internal/tenants/{tenantCode}/students/count") {
            try {
                val tenantCode = call.parameters["tenantCode"]!!
                println("📥 Student count request: tenantCode=$tenantCode")

                val tenant = TenantResolver().resolveByTenantCode(tenantCode)
                    ?: throw IllegalArgumentException("Tenant not found")

                println("✅ Tenant resolved: ${tenant.tenantSchema}")

                val count = transaction {
                    setTenantSchema(tenant.tenantSchema)

                    val c = StudentsTable.selectAll().count()
                    println("📊 Students count inside DB: $c")
                    c
                }

                println("✅ Final student count response: $count")

                call.respond(
                    mapOf(
                        "tenantCode" to tenantCode,
                        "tenantSchema" to tenant.tenantSchema,
                        "studentCount" to count
                    )
                )

            } catch (e: Exception) {
                println("💥 Error in student count API: ${e.message}")
                e.printStackTrace()

                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }



        get("/api/met/{tenantCode}/monitor") {
            try {
                println("got hereeeeee")
                val tenantCode = call.parameters["tenantCode"]!!
                println("📥 Monitor request: tenantCode=$tenantCode")

                val tenant = TenantResolver().resolveByTenantCode(tenantCode)
                    ?: throw IllegalArgumentException("Tenant not found")

                println("✅ Tenant resolved: ${tenant.tenantSchema}")

                val response = transaction {
                    setTenantSchema(tenant.tenantSchema)

                    println("🔄 Fetching all grouped data...")

                    // ✅ PEOPLE
                    val studentCount = StudentsTable.selectAll().count()
                    val staffCount = StaffTable.selectAll().count()
                    val adminCount = AdminTable.selectAll().count()

                    // ✅ ACADEMICS
                    val academicYearCount = AcademicYearTable.selectAll().count()
                    val termCount = TermTable.selectAll().count()
                    val subjectCount = SubjectsTable.selectAll().count()

                    // ✅ FINANCE
                    val paymentCount = PaymentTable.selectAll().count()
                    val feeStructures = FeeStructureTable.selectAll().count()
                    val receiptsCount = ReceiptsTable.selectAll().count()

                    println("✅ Data aggregation complete")

                    TenantMonitorResponse(
                        people = mapOf(
                            "students" to studentCount,
                            "staff" to staffCount,
                            "admins" to adminCount
                        ),
                        academics = mapOf(
                            "academicYears" to academicYearCount,
                            "terms" to termCount,
                            "subjects" to subjectCount
                        ),
                        finance = mapOf(
                            "payments" to paymentCount,
                            "feeStructures" to feeStructures,
                            "receipts" to receiptsCount
                        )
                    )
                }

                println("✅ Monitor response ready")

                call.respond(response)

            } catch (e: Exception) {
                println("💥 Error in monitor API: ${e.message}")
                e.printStackTrace()

                call.respond(HttpStatusCode.InternalServerError, e.message ?: "Error")
            }
        }
    }
}


fun createTenant(request: CreateTenantRequest): CreateTenantResponse {

    val normalizedTenantCode = request.tenantCode
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9_]"), "")

    require(normalizedTenantCode.isNotBlank()) {
        "tenantCode is invalid"
    }

    val tenantSchema = "tenant_$normalizedTenantCode"

    // ✅ 1. Validate tenantCode uniqueness
    transaction {
        addLogger(StdOutSqlLogger)

        val existing = TenantsTable
            .selectAll()
            .where { TenantsTable.tenantCode eq normalizedTenantCode }
            .limit(1)
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException(
                "A tenant with code '$normalizedTenantCode' already exists."
            )
        }
    }

    // ✅ 2. Generate slug
    val baseSlug = generateInitialSlug(request.schoolName)
    val tenantSlug = ensureUniqueTenantSlug(baseSlug)

    // ✅ 3. Build login URLs using wildcard subdomain tenancy

    val defaultDomain = PlatformDomainConfig.buildTenantLoginUrl(tenantSlug)


    val defaultLocalDomain = PlatformDomainConfig.buildTenantLocalLoginUrl(tenantSlug)

// Fallback URL should be local/testing URL
    val fallbackLocalUrl = defaultLocalDomain

    println("🔹 [PROVISION] tenantSlug = $tenantSlug")
    println("🔹 [PROVISION] defaultDomain = $defaultDomain")
    println("🔹 [PROVISION] defaultLocalDomain = $defaultLocalDomain")
    println("🔹 [PROVISION] fallbackLocalUrl = $fallbackLocalUrl")
    println("🔹 [PROVISION] tenantSchema = $tenantSchema")



    println("🔹 [PROVISION] tenantSlug = $tenantSlug")
    println("🔹 [PROVISION] defaultDomain = $defaultDomain")
    println("🔹 [PROVISION] defaultLocalDomain = $defaultLocalDomain")
    println("🔹 [PROVISION] fallbackLocalUrl = $fallbackLocalUrl")
    println("🔹 [PROVISION] tenantSchema = $tenantSchema")

    // ✅ 4. Insert tenant into public schema
    val tenantId = transaction {
        addLogger(StdOutSqlLogger)

        println("📝 [PROVISION] Inserting tenant into public.tenants...")

        TenantsTable.insert {
            it[schoolName] = request.schoolName
            it[tenantCode] = normalizedTenantCode
            it[TenantsTable.tenantSchema] = tenantSchema
            it[TenantsTable.tenantSlug] = tenantSlug

            /**
             * Store production wildcard-subdomain login URL.
             * Example:
             * https://kingofgloryacademy.phenaschool.com/#/login
             */
            it[TenantsTable.defaultDomain] = defaultDomain

            it[schoolType] = request.schoolType
            it[location] = request.location
            it[contactEmail] = request.contactEmail
            it[accountOwnerName] = request.accountOwnerName
            it[primaryDomain] = request.primaryDomain
            it[academicYear] = request.academicYear
            it[status] = "provisioning"
            it[createdAt] = Instant.now().toString()
        } get TenantsTable.id
    }

    println("✅ [PROVISION] Tenant inserted with id = $tenantId")

    // keep the rest of your try/catch provisioning code unchanged...



    try {
        // ✅ 5. Create schema

        TenantSchemaService.createTenantSchema(tenantSchema)


        // ✅ 6. Insert features
        transaction {
            addLogger(StdOutSqlLogger)

            println("🧩 [PROVISION] Inserting tenant features...")

            request.features.forEach { feature ->
                println("   ➕ [PROVISION] feature = $feature")

                TenantFeaturesTable.insert {
                    it[TenantFeaturesTable.tenantId] = tenantId
                    it[featureCode] = feature
                    it[isEnabled] = true
                }
            }
        }

        println("✅ [PROVISION] Features inserted successfully")

        // ✅ 7. Create bootstrap principal
        val bootstrapPrincipalName = when {
            !request.accountOwnerName.isNullOrBlank() -> request.accountOwnerName
            else -> "${request.schoolName} Principal"
        }

        println("👤 [PROVISION] Creating bootstrap principal in schema: $tenantSchema")

        val principalBootstrap: BootstrapPrincipalResponse =
            PrincipalBootstrapService.createBootstrapPrincipalInSchema(
                tenantSchema = tenantSchema,
                req = CreatePrincipalRequest(
                    user = CreateUserPart(
                        fullName = bootstrapPrincipalName,
                        role = "principal",
                        isActive = true,
                        isStaff = true
                    )
                )
            )


        println("✅ [PROVISION] Bootstrap principal created")
        println("🔐 [PROVISION] Principal loginUserId = ${principalBootstrap.loginUserId}")
        println("🔐 [PROVISION] Principal pin = ${principalBootstrap.pin}")

        // ✅ 8. Create bootstrap administrator
        val bootstrapAdminName = when {
            !request.accountOwnerName.isNullOrBlank() -> request.accountOwnerName
            else -> "${request.schoolName} Administrator"
        }

        println("👤 [PROVISION] Creating bootstrap administrator in schema: $tenantSchema")

        val adminBootstrap =
            AdminBootstrapService.createBootstrapAdminInSchema(
                tenantSchema = tenantSchema,
                req = CreateAdminRequest(
                    user = CreateAdminUserPart(
                        fullName = bootstrapAdminName,
                        role = "admin",
                        isActive = true,
                        isStaff = true
                    )
                )
            )

        println("✅ [PROVISION] Bootstrap administrator created")
        println("🔐 [PROVISION] Admin loginUserId = ${adminBootstrap.loginUserId}")
        println("🔐 [PROVISION] Admin pin = ${adminBootstrap.pin}")

        // ✅ 8. Activate tenant
        transaction {
            addLogger(StdOutSqlLogger)

            println("🔄 [PROVISION] Updating tenant status to active...")

            TenantsTable.update({ TenantsTable.id eq tenantId }) {
                it[status] = "active"
            }
        }

        println("🎉 [PROVISION] Tenant provisioning completed successfully")

        // ✅ 9. Final response — PASS EVERYTHING
        return CreateTenantResponse(
            tenantId = tenantId,
            schoolName = request.schoolName,
            tenantCode = normalizedTenantCode,
            tenantSchema = tenantSchema,
            tenantSlug = tenantSlug,
            defaultDomain = defaultDomain,
            defaultLocalDomain = defaultLocalDomain,
            fallbackLocalUrl = fallbackLocalUrl,
            status = "active",
            message = "School created successfully",
            principalLoginUserId = principalBootstrap.loginUserId,
            principalPin = principalBootstrap.pin,
            adminLoginUserId = adminBootstrap.loginUserId,
            adminPin = adminBootstrap.pin

        )

    } catch (e: Exception) {
        println("🔥 [PROVISION] Exception during provisioning: ${e.message}")
        e.printStackTrace()

        transaction {
            addLogger(StdOutSqlLogger)

            println("⚠️ [PROVISION] Marking tenant as failed...")

            TenantsTable.update({ TenantsTable.id eq tenantId }) {
                it[status] = "failed"
            }
        }

        throw e
    }
}

private fun generateInitialSlug(name: String): String {
    return name
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]"), "")
        .ifBlank { "school" }
}

private fun ensureUniqueTenantSlug(baseSlug: String): String {
    var candidate = baseSlug
    var counter = 1

    while (transaction {
            TenantsTable
                .selectAll()
                .where { TenantsTable.tenantSlug eq candidate }
                .limit(1)
                .singleOrNull() != null
        }
    ) {
        candidate = "$baseSlug$counter"
        counter++
    }

    return candidate
}




@Serializable
data class TenantMonitorResponse(
    val people: Map<String, Long>,
    val academics: Map<String, Long>,
    val finance: Map<String, Long>
)