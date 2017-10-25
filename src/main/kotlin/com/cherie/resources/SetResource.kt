package com.cherie.resources

import com.fasterxml.jackson.core.JsonParser
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import javax.annotation.security.RolesAllowed
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.mashape.unirest.http.JsonNode
import org.json.JSONObject
import javax.print.attribute.standard.Media
import javax.ws.rs.core.MediaType


@Path("set")
@RolesAllowed("guest")
class SetResource{


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("set2")
    fun set2(@QueryParam("key") key :String?, @Context req: HttpServletRequest, data: String?): Response {
        val session = req.getSession(false)
        val currObj = session.getAttribute(key!!.substring(0,1)) as String?
        val requestedJSON = JSONObject(data)

        var theroot:ObjectNode? = null
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
                println("the current node that should be created " + curr)
                i = i + 1
            }
           // println("This is the whole object " + theroot)
        }
        val configuration = Configuration.builder()
                .jsonProvider(JacksonJsonNodeJsonProvider())
                .mappingProvider(JacksonMappingProvider())
                .build()
        val path = JsonPath.compile("$" + key!!.substring(1))
       //val testBody = mapper.createObjectNode()
        //testBody.put("hello", "hi")
      //  val testBody = mapper.readValue(data, com.fasterxml.jackson.databind.JsonNode::class.java)
        val testBody = mapper.readTree(data)
        val newJson: String? = JsonPath.using(configuration).parse(theroot).set(path, testBody).jsonString()
        //println("First One: " +newJson)
        session.setAttribute(key!!.substring(0,1),newJson)
        val a = session.getAttribute(key!!.substring(0,1)) as String
       // val test = mapper.readValue(session.getAttribute(key!!.substring(0,1)) as String?, ObjectNode::class.java)
        return Response.ok().entity(mapper.readTree(a)).build()

    }
}