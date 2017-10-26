package com.cherie.resources

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import javax.annotation.security.RolesAllowed
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@RolesAllowed("poweruser")
@Path("admin")
class AdminResource {

   @GET
   @Produces(MediaType.TEXT_PLAIN)
   fun getHello(): Response{
       return Response.ok().entity("Hello from the admin side").build()
   }

    @GET
    @Path("delete")
    @Produces(MediaType.TEXT_PLAIN)
    fun delete(@QueryParam("name") name: String?, @QueryParam("table") table: String?): Response{
        val ic = InitialContext()
        val myDatasource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDatasource)
        transaction{
            if(table.equals("Users"))
            {
                Users.deleteWhere{Users.username eq name}
            }
            else if(table.equals("Roles"))
            {
                Roles.deleteWhere{Roles.name eq name}
            }
            else{
                Permissions.deleteWhere{Permissions.operation eq name}
            }
            for (user in Users.selectAll()) {
                println("${user[Users.id]}: ${user[Users.username]} : ${user[Users.password]}")
            }
            for (role in Roles.selectAll()) {
                println("${role[Roles.id]}: ${role[Roles.name]}")
            }
            for (perm in Permissions.selectAll()){
                println("${perm[Permissions.id]}: ${perm[Permissions.operation]}")
            }
        }
        return Response.ok().type("text/plain").entity("Successfully deleted").build()
    }
}