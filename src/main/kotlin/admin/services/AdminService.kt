package com.example.admin.services




import com.example.account.AccountRepository
import com.example.admin.dtos.requests.CreateAdminRequest
import com.example.admin.models.AdminProfile
import com.example.admin.repos.AdminRepository


import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object AdminService {

    fun createAdmin(request: CreateAdminRequest) = transaction {

        // 1️⃣ Generate unique user_id (same as Django)
        val userId = generateUniqueUserId()

        // 2️⃣ Generate PIN
        val pin = Random.Default.nextInt(1000, 9999).toString()

        // 3️⃣ Create User
        val user = AccountRepository.create(
            userId = userId,
            pin = pin,
            fullName = request.user.fullName,
            gender = request.user.gender,
            dateOfBirth = request.user.dateOfBirth,
            nationality = request.user.nationality,
            role = request.user.role.lowercase(),
            isActive = request.user.isActive,
            isStaff = request.user.isStaff
        )

        // 4️⃣ Prevent duplicate admin profile (like Django get_or_create)
        val existing = AdminRepository.findByUserId(user.id)

        if (existing != null) {
            existing
        } else {
            AdminRepository.create(
                AdminProfile(
                    id = 0,
                    user = user.id,

                    tel = request.tel
                )
            )
        }
    }

    private fun generateUniqueUserId(): String {
        while (true) {
            val candidate = Random.Default.nextInt(10_000_000, 99_999_999).toString()
            if (!AccountRepository.existsByUserId(candidate)) {
                return candidate
            }
        }
    }


    fun deleteAdmin(id: Int): Boolean {
        if (!AdminRepository.existsById(id)) return false
        return AdminRepository.delete(id)
    }

}
