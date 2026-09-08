package birthday.routes

import birthday.dto.BirthdaySettingsRequest
import birthday.dto.BirthdaySettingsResponse
import birthday.models.BirthdayTestResponse
import birthday.services.BirthdayCronService
import birthday.services.BirthdayService
import birthday.services.BirthdaySettingsService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import tenant.getSchoolName
import tenant.getTenantContext
import tenant.getTenantSchema

fun Route.birthdayRoutes() {

    get("/settings") {

        val tenantSchema =
            call.getTenantSchema()

        val settings =
            BirthdaySettingsService.get(
                tenantSchema
            )

        call.respond(
            settings ?: BirthdaySettingsResponse(
                enabled = false,
                birthdayMessage =
                    """
                Happy Birthday {student_name}!

                From all of us at {school_name},
                we wish you joy, good health and success.
                """.trimIndent()
            )
        )
    }


    put("/settings") {

        val tenantSchema =
            call.getTenantSchema()

        val request =
            call.receive<BirthdaySettingsRequest>()

        BirthdaySettingsService.save(
            tenantSchema,
            request
        )

        call.respond(
            "Birthday settings saved"
        )
    }


    get("/system/test-birthday-job") {

        val tenantSchema =
            call.getTenantSchema()

        val schoolName =
            call.getSchoolName()

        val tenantContext =
            call.getTenantContext()

        BirthdayService.processBirthdays(
            tenantSchema = tenantContext.tenantSchema,
            tenantCode = tenantContext.tenantCode,
            schoolName = tenantContext.schoolName
        )

        call.respond(
            "Birthday test completed"
        )
    }

    get("/system/run") {

        BirthdayCronService.run()

        call.respond(
            "Birthday cron completed"
        )
    }
}