package com.example.tenant.services





import com.example.account.AccountRepository
import com.example.admin.dtos.requests.CreateAdminRequest

import com.example.admin.repos.AdminRepository
import com.example.tenant.dto.response.BootstrapAdminResponse
import com.example.tenant.tenantTransaction
import kotlin.random.Random

object AdminBootstrapService {

    fun createBootstrapAdminInSchema(
        tenantSchema: String,
        req: CreateAdminRequest
    ): BootstrapAdminResponse {
        return tenantTransaction(tenantSchema) {
            val pin = Random.nextInt(1000, 10000).toString()
            val generatedLoginUserId = generateUniqueUserId(tenantSchema)

            val account = AccountRepository.createInCurrentTransaction(
                userId = generatedLoginUserId,
                pin = pin,
                fullName = req.user.fullName,
                gender = req.user.gender,
                dateOfBirth = req.user.dateOfBirth,
                nationality = req.user.nationality,
                role = "admin",
                isActive = req.user.isActive,
                isStaff = true
            )

            AdminRepository.createInCurrentTransaction(account.id)

            BootstrapAdminResponse(
                accountId = account.id,
                loginUserId = generatedLoginUserId,
                pin = pin
            )
        }
    }

    private fun generateUniqueUserId(tenantSchema: String): String {
        while (true) {
            val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()

            if (!AccountRepository.existsByUserId(tenantSchema, candidate)) {
                return candidate
            }
        }
    }
}