package com.example.student.repos



import com.example.student.dtos.requests.CreateClassPromotionRequest
import com.example.student.dtos.requests.PatchClassPromotionRequest
import com.example.student.dtos.response.ClassPromotionResponseDto
import com.example.student.dtos.response.toClassPromotionResponseDto
import com.example.student.tables.NewClassPromotionTable
import com.example.student.tables.NewGradeClassTable
import io.ktor.util.reflect.TypeInfo
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

class ClassPromotionRepository {

    /**
     * Base query used by findById/findAll to return expanded DTO with
     * currentStage + nextStage + promotionPath
     */
    private fun baseSelect() = run {
        val currentStageAlias = NewGradeClassTable.alias("current_stage")
        val nextStageAlias = NewGradeClassTable.alias("next_stage")

        val query = NewClassPromotionTable
            .join(
                otherTable = currentStageAlias,
                joinType = JoinType.INNER,
                onColumn = NewClassPromotionTable.currentStageId,
                otherColumn = currentStageAlias[NewGradeClassTable.id]
            )
            .join(
                otherTable = nextStageAlias,
                joinType = JoinType.LEFT,
                onColumn = NewClassPromotionTable.nextStageId,
                otherColumn = nextStageAlias[NewGradeClassTable.id]
            )
            .selectAll()

        Triple(query, currentStageAlias, nextStageAlias)
    }

    /**
     * CREATE
     */
    fun create(req: CreateClassPromotionRequest): ClassPromotionResponseDto = transaction {
        // Optional validation (recommended):
        // Ensure current stage exists; ensure next stage exists (if provided).
        // If you already validate in service layer, you may remove these checks.
        require(stageExists(req.currentStageId)) { "Current stage not found: ${req.currentStageId}" }
        if (req.nextStageId != null) {
            require(stageExists(req.nextStageId)) { "Next stage not found: ${req.nextStageId}" }
        }

        val newId = NewClassPromotionTable.insert {
            it[currentStageId] = req.currentStageId
            it[nextStageId] = req.nextStageId
        } get NewClassPromotionTable.id

        // Return expanded view
        findById(newId.value) ?: error("Failed to load created promotion with id=${newId.value}")
    }

    /**
     * FIND BY ID (expanded)
     */
    fun findById(id: Int): ClassPromotionResponseDto? = transaction {
        val (query, currentStageAlias, nextStageAlias) = baseSelect()

        query
            .andWhere { NewClassPromotionTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.toClassPromotionResponseDto(currentStageAlias, nextStageAlias)
    }

    /**
     * FIND ALL (expanded)
     */
    fun findAll(): List<ClassPromotionResponseDto> = transaction {
        val (query, currentStageAlias, nextStageAlias) = baseSelect()

        query.map { row ->
            row.toClassPromotionResponseDto(currentStageAlias, nextStageAlias)
        }
    }

    /**
     * PATCH
     *
     * Patch semantics:
     * - currentStageId != null => update current stage
     * - setNextStage == true:
     *      - nextStageId = Int => set next stage
     *      - nextStageId = null => clear next stage (Graduated)
     */
    fun patch(id: Int, req: PatchClassPromotionRequest): ClassPromotionResponseDto? = transaction {
        // If record doesn't exist, return null
        if (!promotionExists(id)) return@transaction null

        // Optional validation:
        req.currentStageId?.let {
            require(stageExists(it)) { "Current stage not found: $it" }
        }
        if (req.setNextStage && req.nextStageId != null) {
            require(stageExists(req.nextStageId)) { "Next stage not found: ${req.nextStageId}" }
        }

        NewClassPromotionTable.update({ NewClassPromotionTable.id eq id }) { stmt ->
            req.currentStageId?.let { stmt[currentStageId] = it }

            if (req.setNextStage) {
                stmt[nextStageId] = req.nextStageId // can be null (Graduated)
            }
        }

        findById(id)
    }

    /**
     * DELETE
     */
    fun delete(id: Int): Boolean = transaction {
        NewClassPromotionTable.deleteWhere { NewClassPromotionTable.id eq id } > 0
    }

    // -------------------------
    // Small helpers
    // -------------------------
    private fun stageExists(stageId: Int): Boolean {
        return NewGradeClassTable
            .selectAll()
            .where { NewGradeClassTable.id eq stageId }
            .limit(1)
            .any()
    }

    private fun promotionExists(id: Int): Boolean {
        return NewClassPromotionTable
            .selectAll()
            .where{ NewClassPromotionTable.id eq id }
            .limit(1)
            .any()
    }


}