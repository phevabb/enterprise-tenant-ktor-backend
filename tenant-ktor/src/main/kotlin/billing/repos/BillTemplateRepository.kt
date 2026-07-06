package com.example.billing.repos

import com.example.academics.tables.CategoriesTable
import com.example.billing.dto.BillTemplateItemResponse
import com.example.billing.dto.BillTemplateResponse
import com.example.billing.dto.CreateBillTemplateItemRequest
import com.example.billing.dto.CreateBillTemplateRequest
import com.example.billing.dto.UpdateBillTemplateItemRequest
import com.example.billing.dto.UpdateBillTemplateRequest
import com.example.billing.tables.BillTemplateItemsTable
import com.example.billing.tables.BillTemplatesTable
import com.example.billing.tables.StudentBillsTable
import com.example.student.tables.AcademicYearTable
import com.example.billing.tables.StudentBillItemsTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal

object BillTemplateRepository {

    fun create(
        tenantSchema: String,
        request: CreateBillTemplateRequest
    ): BillTemplateResponse = tenantTransaction(tenantSchema) {
        val now = System.currentTimeMillis()

        val templateId = BillTemplatesTable.insert {
            it[name] = request.name.trim()
            it[category] = request.categoryId
            it[academicYear] = request.academicYearId
            it[academicTerm] = request.academicTermId
            it[description] = request.description
            it[isActive] = true
            it[createdAtEpochMillis] = now
            it[updatedAtEpochMillis] = null
        } get BillTemplatesTable.id

        if (request.items.isNotEmpty()) {
            BillTemplateItemsTable.batchInsert(request.items) { item ->
                this[BillTemplateItemsTable.billTemplate] = templateId.value
                this[BillTemplateItemsTable.itemName] = item.itemName.trim()
                this[BillTemplateItemsTable.description] = item.description
                this[BillTemplateItemsTable.amountCedis] =
                    item.amountCedis?.let { value -> BigDecimal.valueOf(value) }
                this[BillTemplateItemsTable.itemType] = item.itemType
                this[BillTemplateItemsTable.sortOrder] = item.sortOrder
                this[BillTemplateItemsTable.isActive] = item.isActive
            }
        }

        mapTemplate(templateId.value)
            ?: error("Bill template was created but could not be loaded.")
    }

    fun findAll(
        tenantSchema: String
    ): List<BillTemplateResponse> = tenantTransaction(tenantSchema) {
        BillTemplatesTable
            .selectAll()
            .orderBy(BillTemplatesTable.createdAtEpochMillis, SortOrder.DESC)
            .mapNotNull { row ->
                mapTemplate(row[BillTemplatesTable.id].value)
            }
    }

    fun findById(
        tenantSchema: String,
        templateId: Int
    ): BillTemplateResponse? = tenantTransaction(tenantSchema) {
        mapTemplate(templateId)
    }

    fun updateTemplate(
        tenantSchema: String,
        templateId: Int,
        request: UpdateBillTemplateRequest
    ): BillTemplateResponse? = tenantTransaction(tenantSchema) {
        val existing = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq templateId }
            .singleOrNull()
            ?: return@tenantTransaction null

        BillTemplatesTable.update({ BillTemplatesTable.id eq templateId }) {
            request.name?.let { value ->
                it[name] = value.trim()
            }

            request.description?.let { value ->
                it[description] = value
            }

            request.isActive?.let { value ->
                it[isActive] = value
            }

            it[updatedAtEpochMillis] = System.currentTimeMillis()
        }

        mapTemplate(existing[BillTemplatesTable.id].value)
    }

    fun updateTemplateActiveStatus(
        tenantSchema: String,
        templateId: Int,
        isActive: Boolean
    ): BillTemplateResponse? = tenantTransaction(tenantSchema) {
        val updatedRows = BillTemplatesTable.update({ BillTemplatesTable.id eq templateId }) {
            it[BillTemplatesTable.isActive] = isActive
            it[updatedAtEpochMillis] = System.currentTimeMillis()
        }

        if (updatedRows == 0) {
            null
        } else {
            mapTemplate(templateId)
        }
    }

    fun deleteTemplate(
        tenantSchema: String,
        templateId: Int
    ): Boolean = tenantTransaction(tenantSchema) {
        val templateExists = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq templateId }
            .singleOrNull() != null

        if (!templateExists) {
            return@tenantTransaction false
        }

        /**
         * Find all generated student bills from this template.
         */
        val studentBillIds = StudentBillsTable
            .selectAll()
            .where { StudentBillsTable.billTemplate eq templateId }
            .map { row ->
                row[StudentBillsTable.id].value
            }

        /**
         * Delete generated student bill items first.
         */
        studentBillIds.forEach { studentBillId ->
            StudentBillItemsTable.deleteWhere {
                StudentBillItemsTable.studentBill eq studentBillId
            }
        }

        /**
         * Delete generated student bills.
         */
        StudentBillsTable.deleteWhere {
            StudentBillsTable.billTemplate eq templateId
        }

        /**
         * Delete template items.
         */
        BillTemplateItemsTable.deleteWhere {
            BillTemplateItemsTable.billTemplate eq templateId
        }

        /**
         * Delete template.
         */
        val deletedRows = BillTemplatesTable.deleteWhere {
            BillTemplatesTable.id eq templateId
        }

        deletedRows > 0
    }

    fun addItem(
        tenantSchema: String,
        templateId: Int,
        request: CreateBillTemplateItemRequest
    ): BillTemplateItemResponse = tenantTransaction(tenantSchema) {
        val templateExists = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq templateId }
            .singleOrNull() != null

        if (!templateExists) {
            error("Bill template not found.")
        }

        val itemId = BillTemplateItemsTable.insert {
            it[billTemplate] = templateId
            it[itemName] = request.itemName.trim()
            it[description] = request.description
            it[amountCedis] = request.amountCedis?.let { value ->
                BigDecimal.valueOf(value)
            }
            it[itemType] = request.itemType
            it[sortOrder] = request.sortOrder
            it[isActive] = request.isActive
        } get BillTemplateItemsTable.id

        val row = BillTemplateItemsTable
            .selectAll()
            .where { BillTemplateItemsTable.id eq itemId.value }
            .single()

        mapTemplateItem(row)
    }

    fun updateItem(
        tenantSchema: String,
        templateId: Int,
        itemId: Int,
        request: UpdateBillTemplateItemRequest
    ): BillTemplateItemResponse? = tenantTransaction(tenantSchema) {
        val existing = BillTemplateItemsTable
            .selectAll()
            .where {
                (BillTemplateItemsTable.id eq itemId) and
                        (BillTemplateItemsTable.billTemplate eq templateId)
            }
            .singleOrNull()
            ?: return@tenantTransaction null

        BillTemplateItemsTable.update({
            (BillTemplateItemsTable.id eq itemId) and
                    (BillTemplateItemsTable.billTemplate eq templateId)
        }) {
            request.itemName?.let { value ->
                it[itemName] = value.trim()
            }

            request.description?.let { value ->
                it[description] = value
            }

            request.amountCedis?.let { value ->
                it[amountCedis] = BigDecimal.valueOf(value)
            }

            request.itemType?.let { value ->
                it[itemType] = value
            }

            request.sortOrder?.let { value ->
                it[sortOrder] = value
            }

            request.isActive?.let { value ->
                it[isActive] = value
            }
        }

        val updatedRow = BillTemplateItemsTable
            .selectAll()
            .where {
                (BillTemplateItemsTable.id eq existing[BillTemplateItemsTable.id].value) and
                        (BillTemplateItemsTable.billTemplate eq templateId)
            }
            .single()

        mapTemplateItem(updatedRow)
    }

    fun updateItemActiveStatus(
        tenantSchema: String,
        templateId: Int,
        itemId: Int,
        isActive: Boolean
    ): BillTemplateItemResponse? = tenantTransaction(tenantSchema) {
        val updatedRows = BillTemplateItemsTable.update({
            (BillTemplateItemsTable.id eq itemId) and
                    (BillTemplateItemsTable.billTemplate eq templateId)
        }) {
            it[BillTemplateItemsTable.isActive] = isActive
        }

        if (updatedRows == 0) {
            return@tenantTransaction null
        }

        val row = BillTemplateItemsTable
            .selectAll()
            .where {
                (BillTemplateItemsTable.id eq itemId) and
                        (BillTemplateItemsTable.billTemplate eq templateId)
            }
            .single()

        mapTemplateItem(row)
    }

    fun deleteItem(
        tenantSchema: String,
        templateId: Int,
        itemId: Int
    ): Boolean = tenantTransaction(tenantSchema) {
        val deletedRows = BillTemplateItemsTable.deleteWhere {
            (BillTemplateItemsTable.id eq itemId) and
                    (BillTemplateItemsTable.billTemplate eq templateId)
        }

        deletedRows > 0
    }

    private fun mapTemplate(
        templateId: Int
    ): BillTemplateResponse? {
        val row = BillTemplatesTable
            .selectAll()
            .where { BillTemplatesTable.id eq templateId }
            .singleOrNull()
            ?: return null

        val categoryId = row[BillTemplatesTable.category].value
        val academicYearId = row[BillTemplatesTable.academicYear].value
        val academicTermId = row[BillTemplatesTable.academicTerm].value

        val categoryName = CategoriesTable
            .selectAll()
            .where {CategoriesTable.id eq categoryId }
            .singleOrNull()
            ?.get(CategoriesTable.name)

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

        val items = BillTemplateItemsTable
            .selectAll()
            .where { BillTemplateItemsTable.billTemplate eq templateId }
            .orderBy(BillTemplateItemsTable.sortOrder, SortOrder.ASC)
            .map { itemRow ->
                mapTemplateItem(itemRow)
            }

        return BillTemplateResponse(
            id = row[BillTemplatesTable.id].value,
            name = row[BillTemplatesTable.name],

            categoryId = categoryId,
            categoryName = categoryName,

            academicYearId = academicYearId,
            academicYearName = academicYearName,

            academicTermId = academicTermId,
            academicTermName = academicTermName,

            description = row[BillTemplatesTable.description],
            isActive = row[BillTemplatesTable.isActive],

            createdAtEpochMillis = row[BillTemplatesTable.createdAtEpochMillis],
            updatedAtEpochMillis = row[BillTemplatesTable.updatedAtEpochMillis],

            items = items
        )
    }

    private fun mapTemplateItem(
        row: ResultRow
    ): BillTemplateItemResponse {
        return BillTemplateItemResponse(
            id = row[BillTemplateItemsTable.id].value,
            itemName = row[BillTemplateItemsTable.itemName],
            description = row[BillTemplateItemsTable.description],
            amountCedis = row[BillTemplateItemsTable.amountCedis]?.toDouble(),
            itemType = row[BillTemplateItemsTable.itemType],
            sortOrder = row[BillTemplateItemsTable.sortOrder],
            isActive = row[BillTemplateItemsTable.isActive]
        )
    }
}