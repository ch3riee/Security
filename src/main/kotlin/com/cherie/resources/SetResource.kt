package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("set")
class SetResource{

    @GET
    @PermitAll
    @Consumes(MediaType.APPLICATION_JSON)
    fun set(@QueryParam("key") attr :String, @Context req: HttpServletRequest, data: String): Response {
        val session = req.getSession(false)
        val ret = session.setAttribute(attr, data )
        return Response.ok().type("text/plain").entity(data).build()



    }

}