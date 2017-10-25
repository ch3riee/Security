package com.cherie.resources

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import javax.naming.InitialContext
import javax.sql.DataSource




object Users : Table() {
    val id = integer("id").autoIncrement("users_seq").primaryKey() // Column<Int>
    val username = varchar("username",  50) // Column<String>
    val password = varchar("pwd", 50)

}

object Roles : Table() {
    val id = integer("id").autoIncrement("roles_seq").primaryKey() // Column<Int>
    val name = varchar("rolename", 50) // Column<String>
    val desc = varchar("description", 50)
}

object UserRole : Table() {
    val uid = (integer("uid") references Users.id)
    val roleid = (integer("roleid") references Roles.id)
}


object AccountDb {


    fun init(){
        val ic = InitialContext()
        val myDatasource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDatasource)

        transaction {

            create (Users, Roles, UserRole) //just create the tables in init

            Roles.insert{
                it[name] = "guest"
                it[desc] = "default role given"
            }

            Roles.insert{
                it[name] = "admin"
                it[desc] = "administrative role given"
            }

            /*val saintPetersburgId = Cities.insert {
                it[name] = "St. Petersburg"
            } get Cities.id

            val munichId = Cities.insert {
                it[name] = "Munich"
            } get Cities.id

            Cities.insert {
                it[name] = "Prague"
            }

            Users.insert{
                it[username] = "cherie"
                it[password] = "mypass"
            }*/





        }
    }

}
