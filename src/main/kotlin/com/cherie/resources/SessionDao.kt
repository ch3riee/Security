package com.cherie.resources

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.BaseJsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("session")
class SessionDao{

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("guest")
    fun get( @Context req: HttpServletRequest, @QueryParam("key") key :String? ): Response {
        val session = req.getSession(false)
        var theJson = session.getAttribute(key!!.substring(0,1)) as String?
        if(theJson == null)
        {
           return Response.status(Response.Status.NOT_FOUND).type("plain/text").entity("I'm sorry there is no such attribute in the session").build()
        }
        val mapper = ObjectMapper()
        val data =  mapper.readTree(theJson)
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .mappingProvider(JacksonMappingProvider())
                .build()
        val path = JsonPath.compile("$" + key!!.substring(1))
        val newJson: JsonNode = JsonPath.using(configuration).parse(data).read(path)
       return Response.status(Response.Status.OK).entity(newJson).build()

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("set")
    @RolesAllowed("guest")
    fun set(@QueryParam("key") key :String?, @Context req: HttpServletRequest, data: String?): Response {
        val session = req.getSession(false)
        val currObj = session.getAttribute(key!!.substring(0,1)) as String?

        var theroot: ObjectNode? = null
        val mapper = ObjectMapper()
        if(currObj == null){
            //must create the entire structure
            val arr = key.split(".")
            theroot = mapper.createObjectNode()
            var curr: ObjectNode = theroot
            var i = 0
            while(i < arr.size - 1){
                curr = curr.putObject(arr[i+1])
                i = i + 1
            }

        }
        else{

            theroot = mapper.readValue(currObj, ObjectNode::class.java)
            val arr = key.split(".")
            var curr: ObjectNode = theroot
            var i = 1
            while(i <= arr.size - 1){
                var temp = theroot.get(arr[i])
                if(temp == null)
                {
                    curr = curr.putObject(arr[i])
                }
                else{
                    curr = temp as ObjectNode
                }
                i = i + 1
            }
        }
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .mappingProvider(JacksonMappingProvider())
                .build()
        val path = JsonPath.compile("$" + key!!.substring(1))
        val testBody = mapper.readTree(data)
        val newJson: String? = JsonPath.using(configuration).parse(theroot).set(path, testBody).jsonString()
        session.setAttribute(key!!.substring(0,1),newJson)
        val a = session.getAttribute(key!!.substring(0,1)) as String //for debugging purposes, can change
        return Response.ok().entity(mapper.readTree(a)).build()

    }



}