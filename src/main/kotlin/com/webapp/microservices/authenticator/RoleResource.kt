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


@Path("public/gateway/role")
@RolesAllowed("admin")
class RoleResource{

    @GET
    @Path("create")
    @Produces(MediaType.APPLICATION_JSON)
    fun createRoles(@QueryParam("rname") roleName: String): Response {
       //add permission names later
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
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

    @GET
    @Path("delete")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteRoles(@QueryParam("rname") roleName: String, @QueryParam("name") name: String?,
                    @QueryParam("type") type: String?): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var ret = 0
        transaction {
            if(name != null){
                var rid = 0


                for (rows in UserRole.selectAll()){
                    println("${rows[UserRole.uid]} and ${rows[UserRole.roleid]}")
                }


                println("hello in here")
                val idList = java.util.ArrayList<Int>()


              /* val p= Roles.innerJoin(UserRole, {Roles.id}, {UserRole.roleid})
                        .innerJoin(Users, {UserRole.uid}, {Users.id})
                        .forEach{

                        }*/
              //  UserRole.deleteWhere{p}
                val results =(Roles innerJoin UserRole innerJoin Users).select{(Roles.id.eq(UserRole.roleid)) and (Users.id.eq(UserRole.uid))}.map{ it[UserRole.id] }

                UserRole.deleteWhere{UserRole.id.inList(results)}

                for (rows in UserRole.selectAll()){
                    println("${rows[UserRole.uid]} and ${rows[UserRole.roleid]}")
                }


                            //println("${it[Roles.id]} and ${it[UserRole.uid]} and ${it[UserRole.roleid]} and ${it[Users.id]}")}
               /* (Services innerJoin ServiceRole innerJoin Roles).select{(Roles.id.eq(ServiceRole.roleid)) and (Services.id.eq(ServiceRole.sid))}
                        .forEach{
                            println(it)
                           // println("${it[Roles.id]} and ${it[ServiceRole.sid]} and ${it[ServiceRole.roleid]} and ${it[Services.id]}")
                           // ServiceRole.deleteWhere{}
                        }*/

               /* Roles.select{
                    Roles.name.eq(roleName)
                }.forEach{
                    rid = it[Roles.id]
                }

                if(type == "service"){
                  var sid = 0
                  Services.select{
                      Services.sname.eq(name)
                  }.forEach{
                      sid = it[Services.id]
                  }
                  ret = ServiceRole.deleteWhere{
                      ServiceRole.roleid.eq(rid) and ServiceRole.sid.eq(sid)
                  }

                }
                else{
                    //type User Account
                    var uid = 0
                    Users.select{
                        Users.username.eq(name)
                    }.forEach{
                        uid = it[Users.id]
                    }
                    ret = UserRole.deleteWhere{
                        UserRole.roleid.eq(rid) and UserRole.uid.eq(uid)
                    }
                }
            }
            else{
                ret = Roles.deleteWhere{
                    Roles.name.eq(roleName)
                }*/
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

    @Path("update")
    @POST
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

            var rid = 0
            Roles.select{
                Roles.name.eq(rname)
            }.forEach{
                rid = it[Roles.id]
            }

            RolePerm.deleteWhere{
                RolePerm.roleid.eq(rid)
            }

            var id = 0
            list.forEach({ e: String ->
                Permissions.select{
                    Permissions.operation.eq(e)
                }.forEach{
                    id = it[Permissions.id]
                    try {
                        RolePerm.insert {
                            it[roleid] = rid
                            it[pid] = id
                        }
                        count += 1
                    }catch(e: org.postgresql.util.PSQLException){
                        error = e.message
                        //count = 0
                    }

                }
            })
        }
        if(count == 0){
            node.put("Error", error)
        }
        node.put("Update Role Count", count)
        return Response.ok().entity(node).build()
    }
}