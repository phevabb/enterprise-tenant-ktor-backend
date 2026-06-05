package com.example.principal.service

import com.example.account.AccountRepository


import com.example.principal.dtos.requests.CreatePrincipalRequest
import com.example.principal.dtos.responses.CreatePrincipalResponse
import com.example.principal.repos.PrincipalRepository
import com.example.tenant.tenantTransaction
import kotlin.random.Random

object PrincipalService {

    fun createPrincipal(
        tenantSchema: String,
        req: CreatePrincipalRequest
    ): CreatePrincipalResponse {
        return tenantTransaction(tenantSchema) {
            createPrincipalInCurrentTransaction(req)
        }
    }

    fun createPrincipalInCurrentTransaction(
        req: CreatePrincipalRequest
    ): CreatePrincipalResponse {
        val pin = Random.nextInt(1000, 10000).toString()
        val generatedLoginUserId = generateUniqueUserIdInCurrentTransaction()

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

        return CreatePrincipalResponse(
            accountId = account.id,
            loginUserId = generatedLoginUserId,
            pin = pin
        )
    }

    private fun generateUniqueUserIdInCurrentTransaction(): String {
        while (true) {
            val candidate = Random.nextInt(10_000_000, 99_999_999).toString()

            if (!AccountRepository.existsByUserIdInCurrentTransaction(candidate)) {
                return candidate
            }
        }
    }
}



