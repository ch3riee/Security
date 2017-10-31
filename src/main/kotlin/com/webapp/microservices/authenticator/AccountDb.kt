package com.webapp.microservices.authenticator

import org.jetbrains.exposed.sql.*



object Users : Table() {
    val id = integer("id").autoIncrement("users_seq").primaryKey() // Column<Int>
    val username = varchar("username",  50) // Column<String>
    val password = varchar("pwd", 50)

}

object Roles : Table() {
    val id = integer("id").autoIncrement("roles_seq").primaryKey() // Column<Int>
    val name = varchar("rolename", 50) // Column<String>
}

object UserRole : Table() {
    val uid = (integer("uid") references Users.id)
    val roleid = (integer("roleid") references Roles.id)
}

object Permissions: Table(){
    val id = integer("id").autoIncrement().primaryKey()
    val operation = varchar("operation", 50)
}

object RolePerm: Table(){
    val pid = (integer("pid") references Permissions.id)
    val roleid = (integer("roleid") references Roles.id)
}





