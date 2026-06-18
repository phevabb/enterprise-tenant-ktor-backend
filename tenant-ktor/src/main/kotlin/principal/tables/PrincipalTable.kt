package com.example.principal.tables



import com.example.account.AccountTable
import org.jetbrains.exposed.dao.id.IntIdTable

object PrincipalTable : IntIdTable("principal_profile") {

    val user = reference("user", AccountTable)
        .uniqueIndex()
        .nullable() // Django: on_delete = SET_NULL
}
