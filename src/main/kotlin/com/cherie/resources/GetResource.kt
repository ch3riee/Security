package com.cherie.resources

import com.jayway.jsonpath.JsonPath
import javax.annotation.security.RolesAllowed
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import javax.ws.rs.core.MediaType


@Path("session")
class GetResource{

    @GET
    @Path("get")
    @RolesAllowed("guest")
    @Produces(MediaType.APPLICATION_JSON)
    fun get( @Context req: HttpServletRequest, @QueryParam("key") key :String? ): Response {
        val session = req.getSession(false)
        val theJson = session.getAttribute(key!!.substring(0,1))
        val me = session.attributeNames
        for( value:String in  me.toList()){
            println("name="+value)
        }
        val newJson: String? = JsonPath.read(theJson, "$" + key!!.substring(1))
       return Response.status(Response.Status.OK).type("application").entity(newJson).build()

    }



}