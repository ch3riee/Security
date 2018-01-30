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
    @Path("create")
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

    @GET
    @Path("delete")
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

    @POST
    @Path("update")
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
    @Path("read")
    @Produces(MediaType.APPLICATION_JSON)
    fun readUsers(@QueryParam("uname") userName: String? ): Response{
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction{
            if (userName != null)
            {
                val objNode = mapper.createObjectNode()
                var uid = 0
                val rList = ArrayList<Int>()
                val roles = ArrayList<String>()
                //read this one user
                val res = Users.select{
                    Users.username.eq(userName)
                }
                if(res.count() == 0)
                {
                    node.put("Error", "No Operations Executed")
                    node.put("Read User Count", 0)
                    return@transaction
                }
                res.forEach{
                    //grab user info + role info
                    uid = it[Users.id]
                    node.set(userName, objNode)
                }
                UserRole.select{
                    UserRole.uid.eq(uid)
                }.forEach{
                    rList.add(it[UserRole.roleid])
                }
                rList.forEach({ e: Int ->
                   Roles.select{
                       Roles.id.eq(e)
                   }.forEach{
                       roles.add(it[Roles.name])
                   }
                })
                val roleNode: ArrayNode = mapper.valueToTree(roles)
                objNode.set("roles", roleNode)
            } else{
                //print all of them?
                for (user in Users.selectAll()) {
                    val rList = ArrayList<Int>()
                    val roles = ArrayList<String>()
                    UserRole.select{
                        UserRole.uid.eq(user[Users.id])
                    }.forEach{
                       rList.add(it[UserRole.roleid])
                    }
                    rList.forEach({ e: Int ->
                        Roles.select {
                            Roles.id.eq(e)
                        }.forEach {
                            roles.add(it[Roles.name])
                        }
                    })
                    val roleNode: ArrayNode = mapper.valueToTree(roles)
                    val objNode: ObjectNode = mapper.createObjectNode()
                    node.set(user[Users.username], objNode.set("roles", roleNode) )

                }
            }
        }
        return Response.ok().entity(node).build()
    }

}