package tenant

import com.example.tenant.TenantContext



import io.ktor.util.AttributeKey

val TenantContextKey =
    AttributeKey<TenantContext>("TenantContext")