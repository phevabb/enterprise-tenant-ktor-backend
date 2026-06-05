package com.example.principal.service

import com.example.account.AccountRepository
import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.responses.BootstrapPrincipalResponse
import com.example.principal.repos.PrincipalRepository
import com.example.tenant.tenantTransaction
import kotlin.random.Random

object PrincipalBootstrapService {

    fun createBootstrapPrincipalInSchema(
        tenantSchema: String,
        req: CreatePrincipalRequest
    ): BootstrapPrincipalResponse {
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
                role = "principal",
                isActive = req.user.isActive,
                isStaff = true
            )

            PrincipalRepository.createInCurrentTransaction(account.id)

            BootstrapPrincipalResponse(
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