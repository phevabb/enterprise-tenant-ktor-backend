package com.example.tenant

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class TenantFeatureGuardConfig {
    var featureCode: String = ""
}

val TenantFeatureGuard = createRouteScopedPlugin(
    name = "TenantFeatureGuard",
    createConfiguration = ::TenantFeatureGuardConfig
) {
    val featureCode = pluginConfig.featureCode

    onCall { call ->
        val tenant = call.currentTenant()

        if (!tenant.hasFeature(featureCode)) {
            call.respond(
                HttpStatusCode.Forbidden,
                "Feature '$featureCode' is not enabled for this tenant"
            )
            return@onCall
        }
    }
}

fun Route.requireTenantFeature(featureCode: String, build: Route.() -> Unit): Route {
    val guardedRoute = createChild(object : RouteSelector() {
        override suspend fun evaluate(
            context: RoutingResolveContext,
            segmentIndex: Int
        ): RouteSelectorEvaluation = RouteSelectorEvaluation.Constant
    })

    guardedRoute.install(TenantFeatureGuard) {
        this.featureCode = featureCode
    }

    guardedRoute.build()
    return guardedRoute
}
