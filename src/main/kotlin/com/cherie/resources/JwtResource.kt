package com.cherie.resources

import io.jsonwebtoken.Jwts
import sun.security.rsa.RSAPublicKeyImpl
import java.net.URI
import java.security.spec.RSAPublicKeySpec
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.NewCookie

@Path("jwt")
class JwtResource{

    @GET
    @RolesAllowed("guest")
    @Path("getJWT")
    fun getToken(@Context request: HttpServletRequest): Response{
        val session = request.getSession(false)
        val jwt = session.getAttribute("JwtToken") as String
        val cookie = NewCookie("JwtToken", jwt)
        return Response.status(Status.OK).type("text/plain").entity(jwt).cookie(cookie).build()

    }

    @GET
    @RolesAllowed("guest")
    @Path("checkJWT")
    fun checkToken(@CookieParam("JwtToken") cookie: String): Response{
        var publicKey = (this::class.java.classLoader).getResource("pki/Java.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        val res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(cookie).getBody().toList().joinToString {
            it.first + it.second
        }
        return Response.status(Status.OK).type("text/plain").entity(res).build()
    }

}