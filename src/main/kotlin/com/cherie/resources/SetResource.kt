package com.cherie.resources

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import javax.annotation.security.PermitAll
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import javax.annotation.security.RolesAllowed
import javax.ws.rs.client.Entity.json
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory




@Path("set")
@RolesAllowed("guest")
class SetResource{

    @GET
   // @Consumes("application/json")
    fun set(@QueryParam("key") key :String?, @Context req: HttpServletRequest, data:String): Response {
        /*val factory = JsonFactory()

        val mapper = ObjectMapper(factory)
        val rootNode = mapper.readTree(data)

        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonProvider())
                .mappingProvider(JacksonMappingProvider())
                .build()
        val session = req.getSession(false)
        val attr = session.getAttribute(key!!.substring(0, 1))
        if (attr == null) {
            //val newJson = JsonPath.using(configuration).parse(attr).set("$" + key.substring(1), data).jsonString()
            val node = JsonNodeFactory.instance.objectNode()
            val newJson: com.mashape.unirest.http.JsonNode = JsonPath.using(configuration).parse(node).set("$" + key.substring(1), rootNode).json()
            println("newJson: " + newJson)
            session.setAttribute(key.substring(0,1), newJson.toString())
            return Response.ok().type("text/plain").entity(newJson.toString()).build()
        }
        else{
            val newJson = JsonPath.using(configuration).parse(attr).set("$" + key.substring(1), data).jsonString()
            println("newJson: " + newJson)
            return Response.ok().entity(newJson).build()
        }*/
       // val actualObj = com.mashape.unirest.http.JsonNode("{\"parameter\": \"value\"}")
        //return Response.ok().entity(actualObj).build()
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonProvider())
                .mappingProvider(JacksonMappingProvider())
                .build()
        val session = req.getSession(false)
        val actualObj = com.mashape.unirest.http.JsonNode("{\"parameter\": \"value\"}")
        val node = JsonNodeFactory.instance.objectNode() // initializing
        node.put("x", "value") // building

        val factory = JsonFactory()
        val mapper = ObjectMapper(factory)
        val rootNode = mapper.readTree(data)
        println(rootNode)

        val newJson: com.mashape.unirest.http.JsonNode = JsonPath.using(configuration).parse(node).set("$" + key!!.substring(1), rootNode).json()
        println(newJson)
        session.setAttribute(key!!.substring(0,1),newJson.toString())
        return Response.ok().type("text/plain").entity(session.getAttribute(key!!.substring(0,1))).build()

    }

}