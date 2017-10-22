package com.cherie.resources

import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

@Path("get")
class GetResource{

    @GET
    @PermitAll
    fun get(@QueryParam("key") attr :String, @Context req: HttpServletRequest): Response {
        //first handle the dot notation query param
        //key=a.b.c.d

        val session = req.getSession(false)
        val ret = session.getAttribute(attr) as String?
        if(ret != null)
            return Response.status(Response.Status.OK).type("text/plain").entity(ret).build()
        else
            return Response.status(Response.Status.OK).type("text/plain").entity("Attribute does not exist").build()

    }

   /* fun index(obj: Any, i: String) : Any{
        return obj::i
    }*/

}