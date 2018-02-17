package com.webapp.microservices.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.security.RolesAllowed
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("public/gateway/user")
@RolesAllowed("admin")
class UserAccountResource{

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createUsers(body: String): Response {
        //first get the username and password from passed in entity body
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val userName = obj.get("username").asText()
        val pwd = obj.get("password").asText()
        //add roles later?
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var count = 0
        var error:String? = "No Operations Executed"
        val node = mapper.createObjectNode()
        transaction {
            try{
                Users.insert{
                    it[username] = userName
                    it[password] = pwd
                }
                count += 1

            } catch( e: org.postgresql.util.PSQLException){
                error = e.message
            }

        }
        if(count == 0)
        {
            node.put("Error", error)
        }

        node.put("Create User Count", count)
        return Response.ok().entity(node).build()

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteUsers(@QueryParam("uname") userName: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        transaction {
            val ret = Users.deleteWhere{
                Users.username.eq(userName)
            }
            if(ret == 0){
                node.put("Error", "No Operations Executed")
            }
            node.put("Delete User Count", ret)
        }
        return Response.ok().entity(node).build()
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateUser(@QueryParam("uname") userName: String, body: String): Response{
        //body holds the update password
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val pwd = obj.get("password").asText()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val node = mapper.createObjectNode()
        transaction {
            //update password
            val res=  Users.update({Users.username eq userName}){
                it[password] = pwd
            }
            if (res == 0)
            {
                node.put("Error", "No Operations Executed")
                node.put("Update User Count", 0)
                return@transaction
            }
            node.put("Update User Count", 1)
        }

        return Response.ok().entity(node).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun readUsers(@QueryParam("uname") userName: String? ): Response{
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction{
            if (userName != null) {
                val res = Users.select {
                    Users.username.eq(userName)
                }
                if(res.count() == 0)
                {
                    node.put("Error", "No Operations Executed")
                    node.put("Read User Count", 0)
                    return@transaction
                }
                val objNode = mapper.createObjectNode()
                val results = (Roles innerJoin UserRole innerJoin Users)
                        .select { (Users.username.eq(userName)) and (Users.id.eq(UserRole.uid) and (Roles.id.eq(UserRole.roleid))) }
                        .map { it[Roles.name] }
                node.set(userName, objNode)
                val roleNode: ArrayNode = mapper.valueToTree(results)
                objNode.set("roles", roleNode)
                node.put("Read User Count", 1)
            } else {
                //print all of them?
                var count = 0
                for (user in Users.selectAll()) {
                    val objNode: ObjectNode = mapper.createObjectNode()
                    val results = (Roles innerJoin UserRole innerJoin Users)
                            .select { (Users.username.eq(user[Users.username])) and (Users.id.eq(UserRole.uid) and (Roles.id.eq(UserRole.roleid))) }
                            .map { it[Roles.name] }
                    val roleNode: ArrayNode = mapper.valueToTree(results)
                    node.set(user[Users.username], objNode.set("roles", roleNode))
                    count += 1
                }
                node.put("Read User Count", count)

            }
        }
        return Response.ok().entity(node).build()
    }

}
