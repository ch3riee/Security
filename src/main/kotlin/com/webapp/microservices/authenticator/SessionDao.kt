package com.webapp.microservices.authenticator

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import sun.security.rsa.RSAPublicKeyImpl
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType


@Path("session")
class SessionDao{

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    fun get( @Context req: HttpServletRequest, @QueryParam("key") key :String,
             @Context headers: HttpHeaders): Response {
        val temp = headers.getRequestHeader("authorization").get(0) as String
        val jwt = temp.substring(temp.indexOf(' ') + 1) //trims out bearer
        val mapper = ObjectMapper()
        if(checkToken(jwt) != null)
        {
            val session = req.getSession(false)
            val arr = key.split(".")
            val theJson = session.getAttribute(arr[0]) as String?
            theJson?.let {
                val data =  mapper.readTree(theJson)
                val configuration = Configuration.builder()
                        .jsonProvider(JacksonJsonNodeJsonProvider())
                        .mappingProvider(JacksonMappingProvider())
                        .build()
                val path = JsonPath.compile("$" + key!!.substring(1))
                val newJson: JsonNode = JsonPath.using(configuration).parse(data).read(path)
                return Response.status(Response.Status.OK).entity(newJson).build()
            }
            val notFound = mapper.createObjectNode().put("Error", "Attribute was not found in the session!")
            return Response.status(Response.Status.OK).entity(notFound).build()
            //means that theJson is null
        }
        val invalid = mapper.createObjectNode().put("Token Error", "Invalid token error!")
        return Response.status(Response.Status.FORBIDDEN).entity(invalid).build()
    }

    //can we just check by comparing to db? Or is this way better for security
    fun checkToken(token: String): Claims?
    {
        var publicKey = (this::class.java.classLoader).getResource("pki/Public.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        var res : Claims? = null
        try{
            res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(token).body
        }catch(e: Exception){
            return res //exceptions when trying to parse and validate jwt
        }
        return res
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON) //can't explicitly say in request header that want json
    // only b/c login page is in html will say 406
    @Path("set")
    fun set(@QueryParam("key") key :String, @Context req: HttpServletRequest, data: String,
            @Context headers: HttpHeaders): Response {
        val temp = headers.getRequestHeader("authorization").get(0) as String
        val jwt = temp.substring(temp.indexOf(' ') + 1) //trims out bearer
        val mapper = ObjectMapper()
        if(checkToken(jwt) != null)
        {
            val session = req.getSession(false)

            val arr = key.split(".")
            val currObj = session.getAttribute(arr[0]) as String?
            var theRoot: JsonNode
            val mapper = ObjectMapper()

            theRoot = if(currObj == null) mapper.createObjectNode() else (
                    mapper.readTree(currObj))
            if(arr.size == 1 || theRoot.isArray())
            {
                session.setAttribute(key, mapper.readTree(data).toString())
            }
            else{
                var curr: ObjectNode = theRoot as ObjectNode
                var i = 1
                while(i < arr.size - 1){
                    val temp = curr.get(arr[i])
                    //if does not exist then we create
                    curr = if(temp == null) curr.putObject(arr[i]) else (temp as ObjectNode)
                    i += 1
                }

                val configuration = Configuration.builder()
                        .jsonProvider(JacksonJsonNodeJsonProvider())
                        .mappingProvider(JacksonMappingProvider())
                        .build()
                val index = key.lastIndexOf('.')
                val newJson: String? = JsonPath.using(configuration).parse(theRoot)
                        .put(JsonPath.compile("$" + key.substring(1,index)),
                                key.substring(index + 1), mapper.readTree(data)).jsonString()
                session.setAttribute(key.substring(0,1),newJson)
            }

            val a = session.getAttribute(key.substring(0,1)) as String //done this way for debugging purposes
            return Response.ok().entity(mapper.readTree(a)).build()
        }
        //token was invalid
        val invalid = mapper.createObjectNode().put("Token Error", "Invalid token error!")
        return Response.status(Response.Status.FORBIDDEN).entity(invalid).build()

    }

}