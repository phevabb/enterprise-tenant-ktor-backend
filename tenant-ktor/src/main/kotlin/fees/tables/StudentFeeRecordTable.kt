package com.example.fees.tables


import com.example.student.StudentsTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

object StudentFeeRecordTable : IntIdTable("student_fee_record") {

    val student = reference(
        name = "student_id",
        foreign = StudentsTable,
        onDelete = ReferenceOption.CASCADE
    )

    val feeStructure = reference(
        name = "fee_structure_id",
        foreign = FeeStructureTable,
        onDelete = ReferenceOption.CASCADE
    )

    // whole cedis (no decimals)
    val amountPaid = integer("amount_paid").default(0)
    val balance = integer("balance").default(0)

    val isFullyPaid = bool("is_fully_paid").default(false)

    // ✅ NO java.time: store as epoch millis
    val dateCreated = long("date_created")



    init {
        uniqueIndex("uq_sfr_student_feestructure", student, feeStructure)

        index("idx_sfr_student_fee", false, student, feeStructure)
        index("idx_sfr_fully_paid", false, isFullyPaid)
        index("idx_sfr_balance", false, balance)
        index("idx_sfr_date_created", false, dateCreated)
    }
}