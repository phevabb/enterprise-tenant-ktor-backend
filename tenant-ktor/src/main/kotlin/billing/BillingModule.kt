package com.example.billing



import com.example.billing.routes.billRoutes
import com.example.billing.routes.billingLookupRoutes
//import com.example.billing.routes.studentBillRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.billingModule() {
    routing {
        /**
         * Bill template routes:
         *
         * POST   /api/billing/templates
         * GET    /api/billing/templates
         * GET    /api/billing/templates/{templateId}
         * PUT    /api/billing/templates/{templateId}
         * PATCH  /api/billing/templates/{templateId}/active
         * DELETE /api/billing/templates/{templateId}
         *
         * POST   /api/billing/templates/{templateId}/items
         * PUT    /api/billing/templates/{templateId}/items/{itemId}
         * PATCH  /api/billing/templates/{templateId}/items/{itemId}/active
         * DELETE /api/billing/templates/{templateId}/items/{itemId}
         */
        billRoutes()
        billingLookupRoutes()

        /**
         * Student bill routes:
         *
         * POST  /api/billing/templates/{templateId}/generate-individual-bills
         *
         * GET   /api/billing/student-bills
         * GET   /api/billing/student-bills?status=pending
         * GET   /api/billing/student-bills/{billId}
         *
         * GET   /api/billing/students/{studentId}/bills
         * GET   /api/billing/templates/{templateId}/student-bills
         *
         * PATCH /api/billing/student-bills/{billId}/status
         * POST  /api/billing/student-bills/{billId}/payments
         */
//        studentBillRoutes()
    }
}