package com.webapp.microservices.authenticator

import javax.ws.rs.core.Response
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Cookie

@Path("logout")
class Logout{

    @GET
    fun logout(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?): Response {
        if(cookie != null)
        {
            request.getSession(false).invalidate()
        }
        return Response.temporaryRedirect(URI("/rest/login")).build()
    }

}
