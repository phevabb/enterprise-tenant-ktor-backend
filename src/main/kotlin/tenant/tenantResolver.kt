package com.example.tenant




import com.example.tenant.services.TenantRegistryService


class TenantResolver {

    fun resolveByHost(host: String): TenantContext? {
        return TenantRegistryService.resolveByHost(host)
    }

    fun resolveByTenantCode(code: String): TenantContext? {
        return TenantRegistryService.resolveByTenantCode(code)
    }

    fun resolveByTenantSlug(slug: String): TenantContext? {
        return TenantRegistryService.resolveByTenantSlug(slug)
    }
}




