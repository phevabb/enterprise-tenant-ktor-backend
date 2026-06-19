package com.example.tenant




import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.requests.CreateUserPart
import com.example.principal.dtos.responses.BootstrapPrincipalResponse
import com.example.principal.service.PrincipalBootstrapService
import com.example.tenant.dto.requests.CreateTenantRequest
import com.example.tenant.dto.response.CreateTenantResponse
import com.example.tenant.services.TenantSchemaService
import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantFeaturesTable.featureCode
import com.example.tenant.tables.TenantFeaturesTable.isEnabled
import com.example.tenant.tables.TenantsTable
import com.example.tenant.tables.TenantsTable.academicYear
import com.example.tenant.tables.TenantsTable.accountOwnerName
import com.example.tenant.tables.TenantsTable.contactEmail
import com.example.tenant.tables.TenantsTable.createdAt
import com.example.tenant.tables.TenantsTable.location
import com.example.tenant.tables.TenantsTable.primaryDomain
import com.example.tenant.tables.TenantsTable.schoolName
import com.example.tenant.tables.TenantsTable.schoolType
import com.example.tenant.tables.TenantsTable.status
import com.example.tenant.tables.TenantsTable.tenantCode
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

fun Application.tenantAdminModule() {
    routing {
        post("/internal/tenants/create") {
            try {
                val request = call.receive<CreateTenantRequest>()

                println("Reached /internal/tenants/create")
                println("Request payload: $request")


                val response = createTenant(request)
                println("Calling createTenant...")
                println("Tenant created: $response")


                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "Unable to create tenant"))
                )
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

    // ✅ 3. Build login URLs using query-param tenancy
    val baseUrl = PlatformDomainConfig.BASE_DOMAIN.trimEnd('/')
    val loginPath = PlatformDomainConfig.LOGIN_PATH

    // These now all point to query-param-based login URLs
    val defaultDomain = "$baseUrl$loginPath?tenant=$tenantSlug"
    val defaultLocalDomain = "$baseUrl$loginPath?tenant=$tenantSlug"
    val fallbackLocalUrl = "$baseUrl$loginPath?tenant=$tenantSlug"

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

            // Store login-style URL or base domain, depending on your table meaning
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

    try {
        // ✅ 5. Create schema
        println("🏗️ [PROVISION] Creating tenant schema: $tenantSchema")
        TenantSchemaService.createTenantSchema(tenantSchema)
        println("✅ [PROVISION] Schema created successfully")

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
            principalPin = principalBootstrap.pin
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