package com.cherie.resources

import javax.annotation.security.RolesAllowed
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@RolesAllowed("admin")
@Path("admin")
class AdminResource {

   @GET
   @Produces(MediaType.TEXT_PLAIN)
   fun getHello(): Response{
       return Response.ok().entity("Hello from the admin side").build()
   }

    @GET
    @Path("hello")
    @Produces(MediaType.TEXT_PLAIN)
    fun getHello2(): Response{
        return Response.ok().entity("Hello from the admin side").build()
    }
}