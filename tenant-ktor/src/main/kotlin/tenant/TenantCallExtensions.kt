package tenant


import com.example.tenant.TenantContext
import com.example.tenant.TenantKey
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.getTenantContext(): TenantContext {
    return attributes[TenantKey]
}

fun ApplicationCall.getTenantSchema(): String {
    return attributes[TenantKey].tenantSchema
}

fun ApplicationCall.getSchoolName(): String {
    return attributes[TenantKey].schoolName
}