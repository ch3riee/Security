package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*

@Path("hello")
class TeamResource{

    @GET
    @PermitAll
    fun getHello(): Response{
        return Response.status(Status.OK).type("text/plain").entity("HELLO WORLD").build()
    }

}

