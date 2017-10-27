package com.cherie.resources

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
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
    @RolesAllowed("admin")
    fun get( @Context req: HttpServletRequest, @QueryParam("key") key :String): Response {
        val session = req.getSession(false)
        val theJson = session.getAttribute(key!!.substring(0,1)) as String?
        theJson?.let {
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
        //means that theJson is null
        return Response.status(Response.Status.NOT_FOUND)
                .type("plain/text")
                .entity("I'm sorry there is no such attribute in the session")
                .build()


    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("set")
    @RolesAllowed("admin")
    fun set(@QueryParam("key") key :String, @Context req: HttpServletRequest, data: String): Response {
        val session = req.getSession(false)
        val currObj = session.getAttribute(key!!.substring(0,1)) as String?
        val arr = key.split(".")
        var theRoot: ObjectNode
        val mapper = ObjectMapper()
        theRoot = if(currObj == null) mapper.createObjectNode() else (
                mapper.readValue(currObj, ObjectNode::class.java))
        var curr: ObjectNode = theRoot
        var i = 1
        while(i < arr.size - 1){
            var temp = curr.get(arr[i])
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
            val a = session.getAttribute(key.substring(0,1)) as String //done this way for debugging purposes
            return Response.ok().entity(mapper.readTree(a)).build()
    }

}