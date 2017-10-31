package com.webapp.microservices.authenticator

import io.jsonwebtoken.Jwts
import sun.security.rsa.RSAPublicKeyImpl
import java.util.*
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Path("jwt")
class JwtResource{

    @GET
    @Path("checkJWT")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkToken(@CookieParam("JwtToken") cookie: String): Response{
        var publicKey = (this::class.java.classLoader).getResource("pki/Public.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        val res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(cookie).body
        return Response.status(Status.OK).entity(res).build()
    }

}