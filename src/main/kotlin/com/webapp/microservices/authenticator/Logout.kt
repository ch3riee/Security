package com.webapp.microservices.authenticator

import javax.ws.rs.core.Response
import java.net.URI
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Cookie


@Path("public/gateway/session/logout")
class Logout{

    @GET
    fun logout(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?): Response {

        //check that there is the login hash
        if(cookie != null)
        {
            request.getSession(false).invalidate()
        }
        return Response.temporaryRedirect(URI("/rest/public/gateway/session/login"))
                .header("Set-Cookie", "JwtToken=deleted;Domain=127.0.0.1;Path=/;Expires=Thu, 01-Jan-1970 00:00:01 GMT")
                .build()

    }

}
