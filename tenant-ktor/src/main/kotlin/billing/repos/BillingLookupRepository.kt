package com.example.billing.repos




import com.example.academics.tables.CategoriesTable
import com.example.billing.dto.AcademicTermLookupResponse
import com.example.billing.dto.AcademicYearLookupResponse
import com.example.billing.dto.BillingTemplateLookupsResponse
import com.example.billing.dto.CategoryLookupResponse
import com.example.student.tables.AcademicYearTable
import com.example.student.tables.TermTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll

object BillingLookupRepository {

    fun findCategories(
        tenantSchema: String
    ): List<CategoryLookupResponse> = tenantTransaction(tenantSchema) {
        CategoriesTable
            .selectAll()
            .orderBy(CategoriesTable.name, SortOrder.ASC)
            .map { row ->
                CategoryLookupResponse(
                    id = row[CategoriesTable.id].value,
                    name = row[CategoriesTable.name]
                )
            }
    }

    fun findAcademicYears(
        tenantSchema: String
    ): List<AcademicYearLookupResponse> = tenantTransaction(tenantSchema) {
        AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.name, SortOrder.DESC)
            .map { row ->
                AcademicYearLookupResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name],
                    isCurrent = row[AcademicYearTable.isCurrent]
                )
            }
    }

    fun findTerms(
        tenantSchema: String,
        academicYearId: Int? = null
    ): List<AcademicTermLookupResponse> = tenantTransaction(tenantSchema) {
        val rows = TermTable
            .selectAll()
            .orderBy(TermTable.name, SortOrder.ASC)
            .toList()

        rows
            .filter { row ->
                academicYearId == null ||
                        row[TermTable.academic_year].value == academicYearId
            }
            .map { row ->
                AcademicTermLookupResponse(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academicYearId = row[TermTable.academic_year].value,
                    isCurrent = row[TermTable.isCurrent]
                )
            }
    }

    fun findCurrentAcademicYear(
        tenantSchema: String
    ): AcademicYearLookupResponse? = tenantTransaction(tenantSchema) {
        AcademicYearTable
            .selectAll()
            .where { AcademicYearTable.isCurrent eq true }
            .singleOrNull()
            ?.let { row ->
                AcademicYearLookupResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name],
                    isCurrent = row[AcademicYearTable.isCurrent]
                )
            }
    }

    fun findCurrentTerm(
        tenantSchema: String
    ): AcademicTermLookupResponse? = tenantTransaction(tenantSchema) {
        TermTable
            .selectAll()
            .where { TermTable.isCurrent eq true }
            .singleOrNull()
            ?.let { row ->
                AcademicTermLookupResponse(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academicYearId = row[TermTable.academic_year].value,
                    isCurrent = row[TermTable.isCurrent]
                )
            }
    }

    fun findBillingTemplateLookups(
        tenantSchema: String
    ): BillingTemplateLookupsResponse = tenantTransaction(tenantSchema) {
        val categories = CategoriesTable
            .selectAll()
            .orderBy(CategoriesTable.name, SortOrder.ASC)
            .map { row ->
                CategoryLookupResponse(
                    id = row[CategoriesTable.id].value,
                    name = row[CategoriesTable.name]
                )
            }

        val academicYears = AcademicYearTable
            .selectAll()
            .orderBy(AcademicYearTable.name, SortOrder.DESC)
            .map { row ->
                AcademicYearLookupResponse(
                    id = row[AcademicYearTable.id].value,
                    name = row[AcademicYearTable.name],
                    isCurrent = row[AcademicYearTable.isCurrent]
                )
            }

        val academicTerms = TermTable
            .selectAll()
            .orderBy(TermTable.name, SortOrder.ASC)
            .map { row ->
                AcademicTermLookupResponse(
                    id = row[TermTable.id].value,
                    name = row[TermTable.name],
                    academicYearId = row[TermTable.academic_year].value,
                    isCurrent = row[TermTable.isCurrent]
                )
            }

        val currentAcademicYear = academicYears.firstOrNull { it.isCurrent }
        val currentAcademicTerm = academicTerms.firstOrNull { it.isCurrent }

        BillingTemplateLookupsResponse(
            categories = categories,
            academicYears = academicYears,
            academicTerms = academicTerms,
            currentAcademicYear = currentAcademicYear,
            currentAcademicTerm = currentAcademicTerm
        )
    }
}


