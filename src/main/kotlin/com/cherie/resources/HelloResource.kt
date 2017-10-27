package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*

@Path("hello")
class HelloResource {

    @GET
    @PermitAll
    fun getHello(): Response{
        //this resource is purely for testing purposes
        return Response.status(Status.OK).type("text/plain").entity("HELLO WORLD").build()
    }

}

