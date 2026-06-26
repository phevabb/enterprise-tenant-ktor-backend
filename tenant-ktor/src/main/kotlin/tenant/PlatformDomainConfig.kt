package com.example.tenant

object PlatformDomainConfig {

    const val TENANT_ROOT_DOMAIN = "phenaschool.com"    // production
    const val LOCAL_TENANT_ROOT_DOMAIN = "localhost:3000"   // local
    const val LOGIN_PATH = "/#/login"

    fun buildTenantLoginUrl(tenantSlug: String): String {
        val cleanSlug = tenantSlug.trim().lowercase()

      return "https://$cleanSlug.$TENANT_ROOT_DOMAIN$LOGIN_PATH"


    }

    fun buildTenantLocalLoginUrl(tenantSlug: String): String {
        val cleanSlug = tenantSlug.trim().lowercase()

        return "http://$cleanSlug.$LOCAL_TENANT_ROOT_DOMAIN$LOGIN_PATH"     // locally


    }
}