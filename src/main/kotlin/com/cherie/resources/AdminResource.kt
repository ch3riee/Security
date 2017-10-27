package com.cherie.resources

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteWhere
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
   @Path("hello")
   @Produces(MediaType.TEXT_PLAIN)
   fun getHello(): Response{
       //for testing purposes
       return Response.ok().entity("Hello from the admin side").build()
   }

    @GET
    @Path("delete")
    @Produces(MediaType.TEXT_PLAIN)
    fun delete(@QueryParam("name") name: String?, @QueryParam("table") table: String?): Response{
        var message= "Successfully deleted!"
        val ic = InitialContext()
        val myDataSource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDataSource)
        transaction{
            when(table) {
                "Users" -> Users.deleteWhere{Users.username eq name}
                "Roles" -> Roles.deleteWhere{Roles.name eq name}
                "Permissions" -> Permissions.deleteWhere{Permissions.operation eq name}
                 else -> message = "No such table exists in the DB"
            }
        }
        return Response.ok().type("text/plain").entity(message).build()
    }
}