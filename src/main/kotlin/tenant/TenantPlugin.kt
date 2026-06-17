package com.example.tenant

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*




val TenantKey = AttributeKey<TenantContext>("tenant-context")

class TenantPluginConfig {
    lateinit var resolver: TenantResolver
}

val TenantPlugin = createApplicationPlugin(
    name = "TenantPlugin",
    createConfiguration = ::TenantPluginConfig
) {
    val resolver = pluginConfig.resolver

    onCall { call ->
        val path = call.request.path()

        // Skip internal/platform routes
        if (path.startsWith("/internal/")) {
            return@onCall
        }

        val tenantSlugHeader = call.request.headers["X-Tenant-Slug"]
        val tenantCodeHeader = call.request.headers["X-Tenant-Code"]
        val host = call.request.local.serverHost

        val tenant = tenantSlugHeader?.let {
            resolver.resolveByTenantSlug(it)
        } ?: resolver.resolveByHost(host)

        if (tenant == null) {
            call.respond(HttpStatusCode.NotFound, "Tenant not found")
            return@onCall
        }

        if (!tenant.isActive()) {
            call.respond(HttpStatusCode.PaymentRequired, "Tenant is not active")
            return@onCall
        }

        call.attributes.put(TenantKey, tenant)
    }
}