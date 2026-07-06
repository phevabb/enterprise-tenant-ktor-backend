package com.example.billing.repos

import com.example.academics.tables.CategoriesTable
import com.example.account.AccountTable
import com.example.billing.dto.GeneratedBillSummaryResponse
import com.example.billing.dto.SkippedStudentBillResponse
import com.example.billing.dto.StudentBillItemResponse
import com.example.billing.dto.StudentBillResponse
import com.example.billing.tables.BillTemplateItemsTable
import com.example.billing.tables.BillTemplatesTable
import com.example.billing.tables.StudentBillItemsTable
import com.example.billing.tables.StudentBillsTable
import com.example.fees.tables.FeeStructureTable
import com.example.fees.tables.StudentFeeRecordTable
import com.example.student.StudentsTable
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.NewGradeClassTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.util.UUID

object StudentBillRepository {

    fun generateIndividualBillsFromTemplate(
        tenantSchema: String,
        billTemplateId: Int,
        dueDateEpochMillis: Long?
    ): GeneratedBillSummaryResponse = tenantTransaction(tenantSchema) {
        val now = System.currentTimeMillis()

        val template = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq billTemplateId }
            .singleOrNull()
            ?: error("Bill template not found.")

        val categoryId = template[BillTemplatesTable.category].value
        val academicYearId = template[BillTemplatesTable.academicYear].value
        val academicTermId = template[BillTemplatesTable.academicTerm].value

        val templateItems = BillTemplateItemsTable
            .selectAll()
            .where {
                (BillTemplateItemsTable.billTemplate eq billTemplateId) and
                        (BillTemplateItemsTable.isActive eq true)
            }
            .orderBy(BillTemplateItemsTable.sortOrder, SortOrder.ASC)
            .toList()

        val billableTemplateItems = templateItems.filter { row ->
            row[BillTemplateItemsTable.amountCedis] != null
        }

        val classIdsInCategory = NewGradeClassTable
            .selectAll()
            .filter { row ->
                row[NewGradeClassTable.category]?.value == categoryId
            }
            .map { row ->
                row[NewGradeClassTable.id].value
            }

        if (classIdsInCategory.isEmpty()) {
            return@tenantTransaction GeneratedBillSummaryResponse(
                generatedCount = 0,
                skippedCount = 0,
                generatedBillIds = emptyList(),
                skippedStudents = emptyList()
            )
        }

        val studentRows = StudentsTable
            .selectAll()
            .filter { row ->
                val studentClassId = row[StudentsTable.currentNewGradeClass]?.value

                studentClassId != null && classIdsInCategory.contains(studentClassId)
            }

        val generatedBillIds = mutableListOf<Int>()
        val skippedStudents = mutableListOf<SkippedStudentBillResponse>()

        studentRows.forEach { studentRow ->
            val studentId = studentRow[StudentsTable.id].value
            val classId = studentRow[StudentsTable.currentNewGradeClass]?.value

            val existingBillForTerm = StudentBillsTable
                .selectAll()
                .where {
                    (StudentBillsTable.student eq studentId) and
                            (StudentBillsTable.academicTerm eq academicTermId)
                }
                .limit(1)
                .singleOrNull()

            if (existingBillForTerm != null) {
                skippedStudents += SkippedStudentBillResponse(
                    studentId = studentId,
                    studentName = getStudentName(studentId),
                    reason = "Bill already exists for this student and term."
                )

                return@forEach
            }

            val className = getClassNameSnapshot(classId)

            val fixedItemsTotal = billableTemplateItems.fold(BigDecimal.ZERO) { total, row ->
                total + (row[BillTemplateItemsTable.amountCedis] ?: BigDecimal.ZERO)
            }

            val arrears = calculateStudentArrearsFromFeeRecords(
                studentId = studentId,
                currentAcademicYearId = academicYearId,
                currentAcademicTermId = academicTermId
            )

            val totalAmount = fixedItemsTotal + arrears
            val billNumber = generateBillNumber(studentId)

            val studentBillId = StudentBillsTable.insert {
                it[student] = studentId
                it[billTemplate] = billTemplateId
                it[academicYear] = academicYearId
                it[academicTerm] = academicTermId
                it[classNameSnapshot] = className
                it[StudentBillsTable.billNumber] = billNumber
                it[subTotalCedis] = fixedItemsTotal
                it[arrearsCedis] = arrears
                it[discountCedis] = BigDecimal.ZERO
                it[totalAmountCedis] = totalAmount
                it[amountPaidCedis] = BigDecimal.ZERO
                it[balanceCedis] = totalAmount
                it[status] = if (totalAmount <= BigDecimal.ZERO) "paid" else "pending"
                it[StudentBillsTable.dueDateEpochMillis] = dueDateEpochMillis
                it[createdAtEpochMillis] = now
                it[updatedAtEpochMillis] = null
            } get StudentBillsTable.id

            StudentBillItemsTable.batchInsert(billableTemplateItems) { row ->
                this[StudentBillItemsTable.studentBill] = studentBillId.value
                this[StudentBillItemsTable.itemName] = row[BillTemplateItemsTable.itemName]
                this[StudentBillItemsTable.description] = row[BillTemplateItemsTable.description]
                this[StudentBillItemsTable.amountCedis] =
                    row[BillTemplateItemsTable.amountCedis] ?: BigDecimal.ZERO
                this[StudentBillItemsTable.itemType] = row[BillTemplateItemsTable.itemType]
                this[StudentBillItemsTable.sortOrder] = row[BillTemplateItemsTable.sortOrder]
            }

            if (arrears > BigDecimal.ZERO) {
                StudentBillItemsTable.insert {
                    it[studentBill] = studentBillId.value
                    it[itemName] = "Arrears from Previous Term"
                    it[description] = "Outstanding balance from previous fee records"
                    it[amountCedis] = arrears
                    it[itemType] = "arrears"
                    it[sortOrder] = 999
                }
            }

            generatedBillIds += studentBillId.value
        }

        GeneratedBillSummaryResponse(
            generatedCount = generatedBillIds.size,
            skippedCount = skippedStudents.size,
            generatedBillIds = generatedBillIds,
            skippedStudents = skippedStudents
        )
    }

    fun findBillById(
        tenantSchema: String,
        billId: Int
    ): StudentBillResponse? = tenantTransaction(tenantSchema) {
        val row = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.id eq billId }
            .singleOrNull()
            ?: return@tenantTransaction null

        mapStudentBill(row)
    }

    fun findBillsByStudent(
        tenantSchema: String,
        studentId: Int
    ): List<StudentBillResponse> = tenantTransaction(tenantSchema) {
        StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.student eq studentId }
            .orderBy(StudentBillsTable.createdAtEpochMillis, SortOrder.DESC)
            .map { row ->
                mapStudentBill(row)
            }
    }

    fun findAllStudentBills(
        tenantSchema: String
    ): List<StudentBillResponse> = tenantTransaction(tenantSchema) {
        StudentBillsTable
            .selectAll()
            .orderBy(StudentBillsTable.createdAtEpochMillis, SortOrder.DESC)
            .map { row ->
                mapStudentBill(row)
            }
    }

    fun findBillsByStatus(
        tenantSchema: String,
        status: String
    ): List<StudentBillResponse> = tenantTransaction(tenantSchema) {
        StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.status eq status }
            .orderBy(StudentBillsTable.createdAtEpochMillis, SortOrder.DESC)
            .map { row ->
                mapStudentBill(row)
            }
    }

    fun findBillsByTemplate(
        tenantSchema: String,
        templateId: Int
    ): List<StudentBillResponse> = tenantTransaction(tenantSchema) {
        StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.billTemplate eq templateId }
            .orderBy(StudentBillsTable.createdAtEpochMillis, SortOrder.DESC)
            .map { row ->
                mapStudentBill(row)
            }
    }

    fun updateBillStatus(
        tenantSchema: String,
        billId: Int,
        status: String
    ): StudentBillResponse? = tenantTransaction(tenantSchema) {
        val existingBill = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.id eq billId }
            .singleOrNull()
            ?: return@tenantTransaction null

        StudentBillsTable.update({ StudentBillsTable.id eq billId }) {
            it[StudentBillsTable.status] = status
            it[StudentBillsTable.updatedAtEpochMillis] = System.currentTimeMillis()
        }

        val updatedRow = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.id eq existingBill[StudentBillsTable.id].value }
            .single()

        mapStudentBill(updatedRow)
    }

    fun recordPayment(
        tenantSchema: String,
        billId: Int,
        amountPaidCedis: Double
    ): StudentBillResponse? = tenantTransaction(tenantSchema) {
        val row = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.id eq billId }
            .singleOrNull()
            ?: return@tenantTransaction null

        val oldAmountPaid = row[StudentBillsTable.amountPaidCedis]
        val oldBalance = row[StudentBillsTable.balanceCedis]

        val paymentAmount = BigDecimal.valueOf(amountPaidCedis)
        val newAmountPaid = oldAmountPaid + paymentAmount

        val calculatedBalance = oldBalance - paymentAmount

        val newBalance = if (calculatedBalance < BigDecimal.ZERO) {
            BigDecimal.ZERO
        } else {
            calculatedBalance
        }

        val newStatus = when {
            newBalance <= BigDecimal.ZERO -> "paid"
            newAmountPaid > BigDecimal.ZERO -> "partial"
            else -> "pending"
        }

        StudentBillsTable.update({ StudentBillsTable.id eq billId }) {
            it[StudentBillsTable.amountPaidCedis] = newAmountPaid
            it[StudentBillsTable.balanceCedis] = newBalance
            it[StudentBillsTable.status] = newStatus
            it[StudentBillsTable.updatedAtEpochMillis] = System.currentTimeMillis()
        }

        val updatedRow = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.id eq billId }
            .single()

        mapStudentBill(updatedRow)
    }

    private fun mapStudentBill(
        row: ResultRow
    ): StudentBillResponse {
        val billId = row[StudentBillsTable.id].value

        val studentId = row[StudentBillsTable.student].value
        val billTemplateId = row[StudentBillsTable.billTemplate].value
        val academicYearId = row[StudentBillsTable.academicYear].value
        val academicTermId = row[StudentBillsTable.academicTerm].value

        val templateRow = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq billTemplateId }
            .singleOrNull()

        val billTemplateName = templateRow?.get(BillTemplatesTable.name)

        val categoryId = templateRow
            ?.get(BillTemplatesTable.category)
            ?.value

        val categoryName = categoryId?.let { id ->
            CategoriesTable
                .selectAll()
                .where { CategoriesTable.id eq id }
                .singleOrNull()
                ?.get(CategoriesTable.name)
        }

        val academicYearName = AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.id eq academicYearId }
            .singleOrNull()
            ?.get(AcademicYearTable.name)

        val academicTermName = TermTable
            .selectAll()
            .where { TermTable.id eq academicTermId }
            .singleOrNull()
            ?.get(TermTable.name)

        val studentName = getStudentName(studentId)

        val items = StudentBillItemsTable
            .selectAll()
            .where { StudentBillItemsTable.studentBill eq billId }
            .orderBy(StudentBillItemsTable.sortOrder, SortOrder.ASC)
            .map { itemRow ->
                StudentBillItemResponse(
                    id = itemRow[StudentBillItemsTable.id].value,
                    itemName = itemRow[StudentBillItemsTable.itemName],
                    description = itemRow[StudentBillItemsTable.description],
                    amountCedis = itemRow[StudentBillItemsTable.amountCedis].toDouble(),
                    itemType = itemRow[StudentBillItemsTable.itemType],
                    sortOrder = itemRow[StudentBillItemsTable.sortOrder]
                )
            }

        return StudentBillResponse(
            id = billId,

            studentId = studentId,
            studentName = studentName,

            billTemplateId = billTemplateId,
            billTemplateName = billTemplateName,

            categoryId = categoryId,
            categoryName = categoryName,

            academicYearId = academicYearId,
            academicYearName = academicYearName,

            academicTermId = academicTermId,
            academicTermName = academicTermName,

            billNumber = row[StudentBillsTable.billNumber],
            classNameSnapshot = row[StudentBillsTable.classNameSnapshot],

            subTotalCedis = row[StudentBillsTable.subTotalCedis].toDouble(),
            arrearsCedis = row[StudentBillsTable.arrearsCedis].toDouble(),
            discountCedis = row[StudentBillsTable.discountCedis].toDouble(),
            totalAmountCedis = row[StudentBillsTable.totalAmountCedis].toDouble(),
            amountPaidCedis = row[StudentBillsTable.amountPaidCedis].toDouble(),
            balanceCedis = row[StudentBillsTable.balanceCedis].toDouble(),

            status = row[StudentBillsTable.status],
            dueDateEpochMillis = row[StudentBillsTable.dueDateEpochMillis],
            createdAtEpochMillis = row[StudentBillsTable.createdAtEpochMillis],
            updatedAtEpochMillis = row[StudentBillsTable.updatedAtEpochMillis],

            items = items
        )
    }

    private fun getStudentName(
        studentId: Int
    ): String? {
        val studentRow = StudentsTable
            .selectAll()
            .where { StudentsTable.id eq studentId }
            .singleOrNull()
            ?: return null

        val accountId = studentRow[StudentsTable.user].value

        val accountRow = AccountTable
            .selectAll()
            .where { AccountTable.id eq accountId }
            .singleOrNull()
            ?: return null

        return accountRow[AccountTable.fullName]
    }

    private fun calculateStudentArrearsFromFeeRecords(
        studentId: Int,
        currentAcademicYearId: Int,
        currentAcademicTermId: Int
    ): BigDecimal {
        val unpaidRows = StudentFeeRecordTable
            .innerJoin(FeeStructureTable)
            .selectAll()
            .where {
                (StudentFeeRecordTable.student eq studentId) and
                        (StudentFeeRecordTable.isFullyPaid eq false)
            }
            .filter { row ->
                val balance = row[StudentFeeRecordTable.balance]
                val academicYearId = row[FeeStructureTable.academic_year].value
                val termId = row[FeeStructureTable.term].value

                balance > 0 &&
                        (
                                academicYearId != currentAcademicYearId ||
                                        termId != currentAcademicTermId
                                )
            }

        val totalArrears = unpaidRows.fold(0) { total, unpaidRow ->
            total + unpaidRow[StudentFeeRecordTable.balance]
        }

        return BigDecimal(totalArrears)
    }

    private fun getClassNameSnapshot(
        classId: Int?
    ): String? {
        if (classId == null) return null

        return NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq classId }
            .singleOrNull()
            ?.get(NewGradeClassTable.name)
    }

    private fun generateBillNumber(
        studentId: Int
    ): String {
        val suffix = UUID.randomUUID().toString().take(8).uppercase()

        return "BILL-$studentId-$suffix"
    }
}