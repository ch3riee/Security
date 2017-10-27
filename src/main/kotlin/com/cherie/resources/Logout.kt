package com.cherie.resources

import javax.ws.rs.core.Response
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Cookie

@Path("logout")
class Logout{
    @GET
    @Produces("text/html")
    fun logout(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?): Response {
        if(cookie != null)
        {
            request.getSession(false).invalidate()
        }
        return Response.temporaryRedirect(URI("http://127.0.0.1:8080/rest/login")).build()
    }

}
