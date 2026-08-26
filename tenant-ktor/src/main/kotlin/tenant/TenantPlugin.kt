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

        val overallStart = System.currentTimeMillis()

        val path = call.request.path()

        println("======================================================")
        println("[TENANT PLUGIN] REQUEST START")
        println("[TENANT PLUGIN] PATH = $path")
        println("[TENANT PLUGIN] METHOD = ${call.request.httpMethod.value}")
        println("======================================================")

        val excludedPaths = listOf(
            "/api/internal/",
            "/api/public/",
            "/api/superadmin/",
            "/internal/",
            "/api/met/",
            "/health",
            "/metrics"
        )

        if (excludedPaths.any { path.startsWith(it) }) {

            println(
                "[TENANT PLUGIN] SKIPPED TENANT RESOLUTION FOR PATH=$path"
            )

            println(
                "[TENANT PLUGIN] FINISHED IN ${
                    System.currentTimeMillis() - overallStart
                } ms"
            )

            return@onCall
        }

        val tenantSlugHeader =
            call.request.headers[
                "X-Tenant-Slug"
            ]
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val tenantCodeHeader =
            call.request.headers[
                "X-Tenant-Code"
            ]
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val tenantSlugQuery =
            call.request
                .queryParameters[
                "tenantSlug"
            ]
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val tenantCodeQuery =
            call.request
                .queryParameters[
                "tenantCode"
            ]
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        val tenantSlug =
            tenantSlugHeader
                ?: tenantSlugQuery

        val tenantCode =
            tenantCodeHeader
                ?: tenantCodeQuery




        val host =
            call.request.local.serverHost

        println("[TENANT PLUGIN] HOST = $host")
        println(
            "[TENANT PLUGIN] X-Tenant-Slug = $tenantSlugHeader"
        )

        println(
            "[TENANT PLUGIN] X-Tenant-Code = $tenantCodeHeader"
        )

        println(
            "[TENANT PLUGIN] Query tenantSlug = $tenantSlugQuery"
        )

        println(
            "[TENANT PLUGIN] Query tenantCode = $tenantCodeQuery"
        )

        println(
            "[TENANT PLUGIN] Resolved tenantSlug = $tenantSlug"
        )

        println(
            "[TENANT PLUGIN] Resolved tenantCode = $tenantCode"
        )

        val resolutionStart = System.currentTimeMillis()









        val tenant =
            tenantSlug?.let { resolvedSlug ->

                println(
                    "[TENANT PLUGIN] " +
                            "RESOLVING BY TENANT SLUG = $resolvedSlug"
                )

                val start =
                    System.currentTimeMillis()

                val result =
                    resolver.resolveByTenantSlug(
                        resolvedSlug
                    )

                println(
                    "[TENANT PLUGIN] " +
                            "RESOLVE BY SLUG TOOK ${
                                System.currentTimeMillis() - start
                            } ms"
                )

                result

            } ?: tenantCode?.let { resolvedCode ->

                println(
                    "[TENANT PLUGIN] " +
                            "RESOLVING BY TENANT CODE = $resolvedCode"
                )

                val start =
                    System.currentTimeMillis()

                val result =
                    resolver.resolveByTenantCode(
                        resolvedCode
                    )

                println(
                    "[TENANT PLUGIN] " +
                            "RESOLVE BY CODE TOOK ${
                                System.currentTimeMillis() - start
                            } ms"
                )

                result

            } ?: run {

                println(
                    "[TENANT PLUGIN] RESOLVING BY HOST = $host"
                )

                val start =
                    System.currentTimeMillis()

                val result =
                    resolver.resolveByHost(
                        host
                    )

                println(
                    "[TENANT PLUGIN] " +
                            "RESOLVE BY HOST TOOK ${
                                System.currentTimeMillis() - start
                            } ms"
                )

                result
            } ?: run {

            println(
                "[TENANT PLUGIN] RESOLVING BY HOST = $host"
            )

            val start = System.currentTimeMillis()

            val result = resolver.resolveByHost(host)

            println(
                "[TENANT PLUGIN] RESOLVE BY HOST TOOK ${
                    System.currentTimeMillis() - start
                } ms"
            )

            result
        }

        println(
            "[TENANT PLUGIN] TOTAL RESOLUTION TIME = ${
                System.currentTimeMillis() - resolutionStart
            } ms"
        )

        if (tenant == null) {

            println(
                "[TENANT PLUGIN] TENANT NOT FOUND"
            )

            call.respond(
                HttpStatusCode.NotFound,
                "Tenant not found"
            )

            return@onCall
        }

        println(
            "[TENANT PLUGIN] TENANT FOUND"
        )

        println(
            "[TENANT PLUGIN] tenantId=${tenant.tenantId}"
        )

        println(
            "[TENANT PLUGIN] tenantCode=${tenant.tenantCode}"
        )

        println(
            "[TENANT PLUGIN] tenantSlug=${tenant.tenantSlug}"
        )

        println(
            "[TENANT PLUGIN] tenantSchema=${tenant.tenantSchema}"
        )

        println(
            "[TENANT PLUGIN] schoolName=${tenant.schoolName}"
        )

        if (!tenant.isActive()) {

            println(
                "[TENANT PLUGIN] TENANT INACTIVE"
            )

            call.respond(
                HttpStatusCode.PaymentRequired,
                "Tenant is not active"
            )

            return@onCall
        }

        println(
            "[TENANT PLUGIN] STORING TENANT IN REQUEST ATTRIBUTES"
        )

        call.attributes.put(
            TenantKey,
            tenant
        )

        println(
            "[TENANT PLUGIN] REQUEST FINISHED IN ${
                System.currentTimeMillis() - overallStart
            } ms"
        )

        println("======================================================")
    }
}