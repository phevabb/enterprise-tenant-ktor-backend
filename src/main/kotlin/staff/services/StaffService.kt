package com.example.staff.services


import com.example.account.AccountRepository
import com.example.staff.dtos.requests.CreateStaffRequest
import com.example.staff.models.StaffProfile
import com.example.staff.repos.StaffRepository
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random

object StaffService {

    fun createStaff(request: CreateStaffRequest) = transaction {

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

        // 4️⃣ Prevent duplicate staff profile (like Django get_or_create)
        val existing = StaffRepository.findByUserId(user.id)

        if (existing != null) {
            existing
        } else {
            StaffRepository.create(
                StaffProfile(
                    id = 0,
                    user = user.id,
                    assignedClassId = request.assignedClassId,
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


    fun deleteStaff(id: Int): Boolean {
        if (!StaffRepository.existsById(id)) return false
        return StaffRepository.delete(id)
    }

}
