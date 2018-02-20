package com.webapp.microservices.authenticator



import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.security.RolesAllowed
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.*
import javax.ws.rs.core.Response
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.jetbrains.exposed.sql.*
import javax.ws.rs.core.MediaType


@Path("public/gateway/role")
@RolesAllowed("admin")
class RoleResource{

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    fun createRoles(body: String): Response {
       //add permission names later
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val roleName = obj.get("rname").asText()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val node = mapper.createObjectNode()
        var error:String? = "No Operations Executed"
        var count = 0
        transaction {
            try {
                Roles.insert {
                    it[name] = roleName
                }
                count += 1
            }catch( e: org.postgresql.util.PSQLException){
                error = e.message
            }
        }
        if(count == 0)
        {
            node.put("Error", error)
        }
        node.put("Create Role Count", count)
        return Response.ok().entity(node).build()

    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteRoles(@QueryParam("rname") roleName: String, @QueryParam("name") name: String?,
                    @QueryParam("type") type: String?): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var ret = 0
        transaction {
            if (name != null) {
                if (type == "service") {
                    val results = (Roles innerJoin ServiceRole innerJoin Services)
                            .select { (Roles.id.eq(ServiceRole.roleid)) and (Services.id.eq(ServiceRole.sid) and (Services.sname.eq(name)) and (Roles.name.eq(roleName))) }
                            .map { it[ServiceRole.id] }
                    ret = ServiceRole.deleteWhere { ServiceRole.id.inList(results) }

                } else {
                    val results = (Roles innerJoin UserRole innerJoin Users).select { (Roles.id.eq(UserRole.roleid)) and (Users.id.eq(UserRole.uid)) and (Users.username.eq(name)) and (Roles.name.eq(roleName)) }
                            .map { it[UserRole.id] }
                    ret = UserRole.deleteWhere { UserRole.id.inList(results) }
                }
            } else {
                ret = Roles.deleteWhere {
                    Roles.name.eq(roleName)
                }

            }
        }
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        if(ret == 0)
        {
            node.put("Error", "No Operations Executed")
        }
        node.put("Delete Role Count", ret)
        return Response.ok().entity(node).build()
    }


    @GET
    @Path("assign")
    @Produces(MediaType.APPLICATION_JSON)
    fun assignRoles(@QueryParam("rname") roleName: String,
                    @QueryParam("name") name: String, @QueryParam("type") type: String ): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        var count = 0
        var error: String? = "No operations executed"
        transaction{
           var rid = 0
            var id = 0
            Roles.select{
                Roles.name.eq(roleName)
            }.forEach{
                rid = it[Roles.id]
            }

               if(type == "service")
               {
                       Services.select {
                           Services.sname.eq(name)
                       }.forEach {
                           id = it[Services.id]
                           try{
                               ServiceRole.insert {
                                   it[roleid] = rid
                                   it[sid] = id
                               }
                               count += 1
                           }catch(e: org.postgresql.util.PSQLException){
                               error = e.message
                               //count = 0
                           }

                       }
                   }
               else{
                       Users.select {
                           Users.username.eq(name)
                       }.forEach {
                           id = it[Users.id]
                           try{
                               UserRole.insert {
                                   it[roleid] = rid
                                   it[uid] = id
                               }
                               count += 1
                           }catch(e: org.postgresql.util.PSQLException){
                               error = e.message
                               //count = 0
                           }

                       }
               }
        }

        if(count == 0){
            node.put("Error", error)
        }
        node.put("Assign Role Count", count)
        return Response.ok().entity(node).build()
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateRole(@QueryParam("rname") rname: String, body: String): Response{
        val mapper = ObjectMapper()
        val list: ArrayList<String> = mapper.readValue(body, TypeFactory.defaultInstance()
                .constructCollectionType(ArrayList::class.java, String::class.java))
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val node = mapper.createObjectNode()
        var count = 0
        var error:String? = "No Operations Executed"
        transaction {
            //here we are updating roles with new perms. First we delete existing roleperm associations
            //switched to batchinsert for less IO usage

            var rid = 0
            Roles.select{
                Roles.name.eq(rname)
            }.forEach{
                rid = it[Roles.id]
            }

            RolePerm.deleteWhere{
                RolePerm.roleid.eq(rid)
            }

            val permList = Permissions.select{
                Permissions.operation.inList(list)
            }

            val pairList = ArrayList<Pair<Int, Int>>()

            permList.map{
                val p = Pair(it[Permissions.id], rid)
                pairList.add(p)
            }

           val ret =  RolePerm.batchInsert(pairList){ p ->
                this[RolePerm.roleid] = p.second
                this[RolePerm.pid] =  p.first
            }
            count = ret.size

        }
        if(count == 0){
            node.put("Error", error)
        }
        node.put("Update Role Count", count)
        return Response.ok().entity(node).build()
    }

    /*@GET
    @Path("print")
    @Suppress("UNCHECKED_CAST")
            //This method is purely for debugging purposes
    fun getSessions(): Response{
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        transaction {
            for (role in Roles.selectAll()) {
                val results = (Roles innerJoin RolePerm innerJoin Permissions)
                        .select { (Roles.id.eq(RolePerm.roleid) and (Permissions.id.eq(RolePerm.pid)))}
                        .map { println(it[Roles.name] + " :" + "permission: " + it[Permissions.operation]) }

            }

        }
        return Response.ok().type(MediaType.TEXT_PLAIN).entity("Testing").build()

    }*/
}

