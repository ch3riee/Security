package com.webapp.microservices.authenticator

import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.security.RolesAllowed
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.*
import javax.ws.rs.core.Response
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.exposed.sql.*
import javax.ws.rs.core.MediaType


@Path("role")
@RolesAllowed("admin")
class RoleResource{

    @GET
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    fun createRoles(@QueryParam("name") roleName: String): Response {
       //add permission names later
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {
            Roles.insert{
                it[name] = roleName
            } //get Roles.id
        }
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        node.put("Create Count", 1)
        return Response.ok().entity(node).build()

    }

    @GET
    @Path("delete")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteRoles(@QueryParam("name") roleName: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {
            Roles.deleteWhere{
                Roles.name.eq(roleName)
            }
        }
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        node.put("Delete Count", 1)
        return Response.ok().entity(node).build()
    }

    @GET
    @Path("assign")
    @Produces(MediaType.APPLICATION_JSON)
    fun assignRoles(@QueryParam("rname") roleName: String,
                    @QueryParam("name") name: String, @QueryParam("type") type: String ): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
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
                Services.select{
                    Services.sname.eq(name)
                }.forEach{
                    id = it[Services.id]
                    ServiceRole.insert{
                        it[roleid] = rid
                        it[sid] = id
                    }
                }

            }
            else{
                Users.select{
                    Users.username.eq(name)
                }.forEach{
                    id = it[Users.id]
                    UserRole.insert{
                        it[roleid] = rid
                        it[uid] = id
                    }
                }

            }
        }
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        node.put("Assign Count", 1)
        return Response.ok().entity(node).build()
    }

    @Path("update")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateRole(@QueryParam("name") rname: String, body: String): Response{
        val mapper = ObjectMapper()
        val list: ArrayList<String> = mapper.readValue(body, TypeFactory.defaultInstance()
                .constructCollectionType(ArrayList::class.java, String::class.java))
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {
            var rid = 0
            Roles.select{
                Roles.name.eq(rname)
            }.forEach{
                rid = it[Roles.id]
            }

            //now delete all existing ones
            RolePerm.deleteWhere{
                RolePerm.roleid.eq(rid)
            }

            list.forEach({ e: String ->
                    val id = Permissions.insert {
                        it[operation] = e
                    } get Permissions.id

                    RolePerm.insert {
                        it[roleid] = rid
                        it[pid] = id
                    }
            })
        }
        val node = mapper.createObjectNode()
        node.put("Update Count", 1)
        return Response.ok().entity(node).build()
    }

    @GET
    @Path("print") //purely for testing purposes
    fun printPerms(): Response
    {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {

            for(r in Permissions.selectAll()){
                println("${r[Permissions.id]}: ${r[Permissions.operation]}")
            }

            for(r in RolePerm.selectAll()){
                println("${r[RolePerm.roleid]}: ${r[RolePerm.pid]}")
            }
        }
        return Response.ok().build()
    }
}