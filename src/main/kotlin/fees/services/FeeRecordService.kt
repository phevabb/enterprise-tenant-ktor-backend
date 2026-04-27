package com.example.fees.services



import com.example.fees.tables.StudentFeeRecordTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq

object FeeRecordService {

    fun applyPaymentDelta(sfrId: Int, delta: Int) {
        require(delta > 0) { "Payment delta must be positive" }

        StudentFeeRecordTable.update({ StudentFeeRecordTable.id eq sfrId }) { ub ->
            with(SqlExpressionBuilder) {
                val bal = StudentFeeRecordTable.balance
                val paid = StudentFeeRecordTable.amountPaid

                // newBalance = max(balance - delta, 0)
                val newBalance =
                    Case()
                        .When((bal - delta) less 0, intLiteral(0))
                        .Else(bal - delta)

                // applied = min(delta, balance)
                val applied =
                    Case()
                        .When((bal - delta) less 0, bal)
                        .Else(intLiteral(delta))

                ub.update(StudentFeeRecordTable.amountPaid, paid + applied)
                ub.update(StudentFeeRecordTable.balance, newBalance)
                ub.update(
                    StudentFeeRecordTable.isFullyPaid,
                    Case()
                        .When(newBalance lessEq 0, booleanLiteral(true))
                        .Else(booleanLiteral(false))
                )
            }
        }
    }
}