package com.example.tenant



data class TenantContext(
    val tenantId: Int,
    val schoolName: String,
    val tenantCode: String,
    val tenantSlug: String,
    val tenantSchema: String,
    val defaultDomain: String,
    val status: String,
    val features: Set<String>
) {
    fun isActive(): Boolean = status == "active" || status == "trial"

    fun hasFeature(featureCode: String): Boolean {
        return features.contains(featureCode)
    }
}
