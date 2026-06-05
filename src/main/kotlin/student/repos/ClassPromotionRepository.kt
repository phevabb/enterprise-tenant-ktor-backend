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


    fun create(
        tenantSchema: String,
        req: CreateClassPromotionRequest
    ): ClassPromotionResponseDto = transaction {



        setTenantSchema(tenantSchema)

        require(stageExists(req.currentStageId)) {
            "Current stage not found: ${req.currentStageId}"
        }

        if (req.nextStageId != null) {
            require(stageExists(req.nextStageId)) {
                "Next stage not found: ${req.nextStageId}"
            }
        }

        val newId = NewClassPromotionTable.insert {
            it[currentStageId] = req.currentStageId
            it[nextStageId] = req.nextStageId
        } get NewClassPromotionTable.id

        findById(
            tenantSchema = tenantSchema,
            id = newId.value
        ) ?: error("Failed to load created promotion")
    }


    fun findById(
        tenantSchema: String,
        id: Int
    ): ClassPromotionResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        val (query, currentStageAlias, nextStageAlias) = baseSelect()

        query
            .andWhere { NewClassPromotionTable.id eq id }
            .limit(1)
            .firstOrNull()
            ?.toClassPromotionResponseDto(
                currentStageAlias,
                nextStageAlias
            )
    }


    fun findAll(
        tenantSchema: String
    ): List<ClassPromotionResponseDto> = transaction {

        setTenantSchema(tenantSchema)

        val (query, currentStageAlias, nextStageAlias) = baseSelect()

        query.map {
            it.toClassPromotionResponseDto(
                currentStageAlias,
                nextStageAlias
            )
        }
    }


    fun patch(
        tenantSchema: String,
        id: Int,
        req: PatchClassPromotionRequest
    ): ClassPromotionResponseDto? = transaction {

        setTenantSchema(tenantSchema)

        if (!promotionExists(id)) {
            return@transaction null
        }

        req.currentStageId?.let {
            require(stageExists(it)) {
                "Current stage not found: $it"
            }
        }

        if (req.setNextStage && req.nextStageId != null) {
            require(stageExists(req.nextStageId)) {
                "Next stage not found: ${req.nextStageId}"
            }
        }

        NewClassPromotionTable.update(
            { NewClassPromotionTable.id eq id }
        ) { stmt ->

            req.currentStageId?.let {
                stmt[currentStageId] = it
            }

            if (req.setNextStage) {
                stmt[nextStageId] = req.nextStageId
            }
        }

        findById(
            tenantSchema = tenantSchema,
            id = id
        )
    }


    fun delete(
        tenantSchema: String,
        id: Int
    ): Boolean = transaction {

        setTenantSchema(tenantSchema)

        NewClassPromotionTable.deleteWhere {
            NewClassPromotionTable.id eq id
        } > 0
    }


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