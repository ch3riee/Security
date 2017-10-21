package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("set")
class SetResource{

    @GET
    @PermitAll
    @Path("{attribute}")
    @Consumes(MediaType.APPLICATION_JSON)
    fun get(@PathParam("attribute") attr: String, @Context req: HttpServletRequest, data: String): Response {

        val session = req.getSession(false)
        val ret = session.setAttribute(attr, data )
        return Response.ok().type("text/plain").entity(data).build()



    }

}