package com.example.familyfees.tables

import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.postgresql.shaded.com.ongres.scram.common.bouncycastle.pbkdf2.Integers

object FamilyFeeRecordTable: IntIdTable("family_fee_record") {

    val family = reference("family", FamilyTable)
    val amount_to_pay = integer("amount_to_pay")
    val amount_paid = integer("amount_paid")
    val balance = integer("balance")
    val is_fully_paid = bool("is_fully_paid").default(false)
    val term = reference("term", TermTable)
    val academic_year = reference("academic_year", AcademicYearTable)
    val date_created = long("date_created")


}