package com.webapp.microservices.authenticator


import org.jetbrains.exposed.sql.*



object Users : Table() {
    val id = integer("id").autoIncrement().primaryKey() // Column<Int>
    val username = varchar("username",  50) // Column<String>
    val password = varchar("pwd", 50)

}

object Roles : Table() {
    val id = integer("id").autoIncrement().primaryKey() // Column<Int>
    val name = varchar("rolename", 50) // Column<String>
}

object UserRole : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val uid = (integer("uid") references Users.id)
    val roleid = (integer("roleid") references Roles.id)
}

object Permissions: Table(){
    val id = integer("id").autoIncrement().primaryKey()
    val operation = varchar("operation", 50)
}

object RolePerm: Table(){
    val id = integer("id").autoIncrement().primaryKey()
    val pid = (integer("pid") references Permissions.id)
    val roleid = (integer("roleid") references Roles.id)
}

object Services: Table(){
    val id = integer("id").autoIncrement().primaryKey()
    val sname = varchar("servicename", 50)
    val token = text("servicetoken")
    val pubKey = text("publickey")
    val secret = varchar("tempsecret", 20)
}

object ServiceRole: Table(){
    val id = integer("id").autoIncrement().primaryKey()
    val sid = (integer("sid") references Services.id)
    val roleid = (integer("roleid") references Roles.id)
}

object JettySessions: Table(){
    val  id = varchar("sessionid", 120)
    val cPath = varchar("contextpath", 60)
    val virtual = varchar("virtualhost", 60)
    val lnode = varchar("lastnode", 60)
    val aTime = long("accesstime")
    val laTime = long("lastaccesstime")
    val createTime = long("createtime")
    val cookieTime = long("cookietime")
    val lsTime = long("lastsavedtime")
    val eTime = long("expirytime")
    val maxInt = long("maxinterval")
    val map = blob("map")


}







