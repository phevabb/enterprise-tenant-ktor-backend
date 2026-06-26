package com.example.tenant




import com.example.tenant.services.TenantRegistryService


class TenantResolver {

    fun resolveByHost(host: String): TenantContext? {
        val tenantSlug = extractTenantSlugFromHost(host)
            ?: return null

        return resolveByTenantSlug(tenantSlug)
    }

    fun resolveByTenantCode(code: String): TenantContext? {
        return TenantRegistryService.resolveByTenantCode(code)
    }

    fun resolveByTenantSlug(slug: String): TenantContext? {
        return TenantRegistryService.resolveByTenantSlug(slug)
    }
}



private const val ROOT_DOMAIN = "phenaschool.com"

fun extractTenantSlugFromHost(host: String): String? {
    val cleanHost = host
        .trim()
        .lowercase()
        .substringBefore(":") // removes port if any

    if (!cleanHost.endsWith(ROOT_DOMAIN)) {
        return null
    }

    if (cleanHost == ROOT_DOMAIN) {
        return null
    }

    val suffix = ".$ROOT_DOMAIN"

    if (!cleanHost.endsWith(suffix)) {
        return null
    }

    val subdomain = cleanHost.removeSuffix(suffix)

    return subdomain.takeIf { it.isNotBlank() }
}



