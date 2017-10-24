package com.cherie.resources

import com.jayway.jsonpath.JsonPath
import com.mashape.unirest.http.Unirest
import javax.annotation.security.PermitAll
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.JsonNode


@Path("session")
class GetResource{

    @GET
    @Path("get")
    @RolesAllowed("guest")
    fun get( @Context req: HttpServletRequest): Response {
        //first handle the dot notation query param
        //key=a.b.c.d
        //val attr = attrPath.substring(0, 1) //gets the attribute name
       // val path = attrPath.substring(1)
        val session = req.getSession(false)
        val theJson = session.getAttribute("JwtToken")
        val me = session.attributeNames
        for( value:String in  me.toList()){
            println("name="+value)
        }
        println("this below is returned for get")
        println(theJson)
        /*val ret:String = JsonPath.read(theJson, "$" + path)
        if(!ret.equals(""))
            return Response.status(Response.Status.OK).entity(ret).build()*/
      //  else
       return Response.status(Response.Status.OK).type("text/plain").entity(theJson).build()

    }

   /* fun index(obj: Any, i: String) : Any{
        return obj::i
    }*/
    @GET
    @Path("post")
    @RolesAllowed("guest")
    fun postTest(){
       val actualObj = JsonNode("{\"parameter\": \"value\"}")
       val jsonResponse = Unirest.post("http://127.0.0.1:8080/rest/set")
                .header("accept", "text/plain")
                .header("Content-Type", "application/json")
                .queryString("key", "a.b.c")
                .body(actualObj)
                .asJson()

        val obj = jsonResponse.getBody().getObject()
        println("This is my obj " + obj)
    }

}