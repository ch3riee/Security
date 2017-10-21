package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

@Path("get")
class GetResource{

    @GET
    @PermitAll
    @Path("{attribute}")
    fun get(@PathParam("attribute") attr: String, @Context req: HttpServletRequest): Response {

        val session = req.getSession(false)
        val ret = session.getAttribute(attr) as String?
        if(ret != null)
            return Response.status(Response.Status.OK).type("text/plain").entity(ret).build()
        else
            return Response.status(Response.Status.OK).type("text/plain").entity("Attribute does not exist").build()

    }

}