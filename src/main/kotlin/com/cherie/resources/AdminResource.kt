package com.cherie.resources

import javax.annotation.security.DeclareRoles
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


//@DeclareRoles(value = *arrayOf("guest", "admin"))
@Path("admin")
class AdminResource {

   @GET
   @Produces(MediaType.TEXT_PLAIN)
  // @RolesAllowed("admin")
   fun getHello(@Context myrequest: HttpServletRequest): Response{
       /*var inrole = myrequest.isUserInRole("admin")
       if(inrole != true)
       {
           return Response.status(Response.Status.FORBIDDEN).build()
       }*/
       return Response.ok().entity("Hello from the admin side").build()
   }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
            // @RolesAllowed("admin")
    fun getHello2(): Response{
        /*var inrole = myrequest.isUserInRole("admin")
        if(inrole != true)
        {
            return Response.status(Response.Status.FORBIDDEN).build()
        }*/
        return Response.ok().entity("Hello from the admin side").build()
    }
}