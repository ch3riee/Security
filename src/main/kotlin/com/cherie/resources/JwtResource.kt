package com.cherie.resources

import java.net.URI
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.NewCookie

@Path("getJWT")
class JwtResource{

    @GET
    @RolesAllowed("guest")
    fun getToken(@Context request: HttpServletRequest): Response{
        val session = request.getSession(false)
        val jwt = session.getAttribute("JwtToken") as String
        println(jwt)
        val cookie = NewCookie("JwtToken", jwt)
        return Response.status(Status.OK).type("text/plain").entity(jwt).cookie(cookie).build()

    }

}