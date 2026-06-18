package com.example.tenant



import io.ktor.server.application.*

fun ApplicationCall.currentTenant(): TenantContext {
    return attributes[TenantKey]
}