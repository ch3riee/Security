package com.webapp.microservices.sample

import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*

@RolesAllowed(value = *arrayOf("user", "guest"))
@Path("hello")
class HelloResource {

    @GET
    fun getHello(): Response{
        //this resource is purely for testing purposes
        return Response.status(Status.OK).type("text/plain").entity("HELLO WORLD").build()
    }

}

