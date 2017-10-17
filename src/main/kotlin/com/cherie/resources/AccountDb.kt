package com.cherie.resources

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.SchemaUtils.drop

object Users : Table() {
    val id = integer("id").autoIncrement("users_seq").primaryKey() // Column<Int>
    val name = varchar("name",  50) // Column<String>
    val cityId = (integer("city_id") references Cities.id).nullable() // Column<Int?>
}

object Cities : Table() {
    val id = integer("id").autoIncrement("cities_seq").primaryKey() // Column<Int>
    val name = varchar("name", 50) // Column<String>
}


object AccountDb {

    val jdbc = "jdbc:postgresql://db-account:5432/account?user=jetty&password=jettypass"
    val driver = "org.postgresql.Driver"


    fun init(){
        Database.connect(jdbc, driver)
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
                it[name] = "cherie"
                it[cityId] = munichId
            }



            for (city in Cities.selectAll()) {
                println("${city[Cities.id]}: ${city[Cities.name]}")
            }

            for (user in Users.selectAll()) {
                println("${user[Users.id]}: ${user[Users.name]}")
            }

/*
            (Users innerJoin Cities).slice(Users.name, Users.cityId, Cities.name).
                    select {Cities.name.eq("St. Petersburg") or Users.cityId.isNull()}.forEach {
                if (it[Users.cityId] != null) {
                    println("${it[Users.name]} lives in ${it[Cities.name]}")
                }
                else {
                    println("${it[Users.name]} lives nowhere")
                }
            }

            println("Functions and group by:")

            ((Cities innerJoin Users).slice(Cities.name, Users.id.count()).selectAll().groupBy(Cities.name)).forEach {
                val cityName = it[Cities.name]
                val userCount = it[Users.id.count()]

                if (userCount > 0) {
                    println("$userCount user(s) live(s) in $cityName")
                } else {
                    println("Nobody lives in $cityName")
                }
            }*/

            //drop (Users, Cities)

        }
    }


}