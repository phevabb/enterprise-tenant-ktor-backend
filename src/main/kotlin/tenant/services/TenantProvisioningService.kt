package com.example.tenant.services

import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.requests.CreateUserPart
import com.example.principal.dtos.responses.BootstrapPrincipalResponse
import com.example.principal.service.PrincipalBootstrapService
import com.example.tenant.PlatformDomainConfig

import com.example.tenant.dto.requests.CreateTenantRequest
import com.example.tenant.dto.response.CreateTenantResponse
import com.example.tenant.tables.TenantFeaturesTable
import com.example.tenant.tables.TenantsTable
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

object TenantProvisioningService {

    fun createTenant(request: CreateTenantRequest): CreateTenantResponse {
        val normalizedTenantCode = request.tenantCode
            .trim()
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "")

        require(normalizedTenantCode.isNotBlank()) {
            "tenantCode is invalid"
        }

        val tenantSchema = "tenant_$normalizedTenantCode"

        println("🔹 [PROVISION] Starting tenant creation")
        println("🔹 [PROVISION] schoolName = ${request.schoolName}")
        println("🔹 [PROVISION] tenantCode = $normalizedTenantCode")
        println("🔹 [PROVISION] tenantSchema = $tenantSchema")

        // 1. Validate tenantCode uniqueness
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

        // 2. Generate unique opaque tenant slug + default domain
        val tenantSlug = generateUniqueTenantSlug()
        val defaultDomain = "${tenantSlug}.${PlatformDomainConfig.BASE_DOMAIN}"

        println("🔹 [PROVISION] tenantSlug = $tenantSlug")
        println("🔹 [PROVISION] defaultDomain = $defaultDomain")

        // 3. Insert public tenant record
        val tenantId = transaction {
            addLogger(StdOutSqlLogger)

            println("📝 [PROVISION] Inserting tenant into public.tenants...")

            TenantsTable.insert {
                it[schoolName] = request.schoolName
                it[tenantCode] = normalizedTenantCode
                it[TenantsTable.tenantSchema] = tenantSchema
                it[TenantsTable.tenantSlug] = tenantSlug
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
            // 4. Create schema + runtime tables
            println("🏗️ [PROVISION] Creating tenant schema: $tenantSchema")
            TenantSchemaService.createTenantSchema(tenantSchema)
            println("✅ [PROVISION] Schema created successfully")

            // 5. Save enabled features
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

            // 6. Create bootstrap principal INSIDE the newly created schema
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

            // 7. Activate tenant
            transaction {
                addLogger(StdOutSqlLogger)

                println("🔄 [PROVISION] Updating tenant status to active...")

                TenantsTable.update({ TenantsTable.id eq tenantId }) {
                    it[status] = "active"
                }
            }

            println("🎉 [PROVISION] Tenant provisioning completed successfully")

            return CreateTenantResponse(
                tenantId = tenantId,
                schoolName = request.schoolName,
                tenantCode = normalizedTenantCode,
                tenantSchema = tenantSchema,
                tenantSlug = tenantSlug,
                defaultDomain = defaultDomain,
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

    private fun generateUniqueTenantSlug(): String {
        repeat(20) {
            val candidate = TenantSlugGenerator.generate()

            val exists = transaction {
                TenantsTable
                    .selectAll()
                    .where { TenantsTable.tenantSlug eq candidate }
                    .limit(1)
                    .singleOrNull() != null
            }

            if (!exists) {
                return candidate
            }
        }

        throw IllegalStateException(
            "Unable to generate unique tenant slug after multiple attempts."
        )
    }
}