package com.example.tenant

object PlatformDomainConfig {
    // ✅ Local frontend base
//    const val BASE_DOMAIN = "http://localhost:3000"

    // ✅ Production frontend base
     const val BASE_DOMAIN = "https://enterprise-tenant-vue-frontend.vercel.app"

    // ✅ Vue login hash route
    const val LOGIN_PATH = "/#/login"
}