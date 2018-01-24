package com.webapp.microservices.authenticator

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.CookieParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.Cookie

@Path("callback")
class Callbacks{
    @GET
    @Path("/gateway/guestcallback") //will have to fix
    fun callbackGuest(@Context request: HttpServletRequest){
    }


    @GET
    @Path("/gateway/ssocallback")
    fun callbackSSO(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?){
        //this is where custom authenticator intercepts and tries to validate the github login request
    }

    @POST
    @Path("/gateway/localcallback")
    fun callbackLocal(){
        //this is where custom authenticator intercepts and tries to validate the local login request
    }
}