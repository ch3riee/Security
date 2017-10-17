package com.cherie.resources

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop
import javax.naming.InitialContext
import javax.sql.DataSource




object Users : Table() {
    val id = integer("id").autoIncrement("users_seq").primaryKey() // Column<Int>
    val username = varchar("name",  50) // Column<String>
    val password = varchar("pd", 50)

}

object Cities : Table() {
    val id = integer("id").autoIncrement("cities_seq").primaryKey() // Column<Int>
    val name = varchar("name", 50) // Column<String>
}


object AccountDb {


    fun init(){
        val ic = InitialContext()
        val myDatasource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDatasource)
        transaction {
            create (Cities, Users)
            val saintPetersburgId = Cities.insert {
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
            }



            for (city in Cities.selectAll()) {
                println("${city[Cities.id]}: ${city[Cities.name]}")
            }

            for (user in Users.selectAll()) {
                println("${user[Users.id]}: ${user[Users.username]}")
            }



        }
    }

}
