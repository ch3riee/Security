package com.cherie.resources

import javax.ws.rs.core.UriInfo
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import com.mashape.unirest.http.Unirest
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Cookie


@Path("login")
class Login{

    @GET
    @Produces("text/html")
    fun loginGithub(): Response{
        /*if(cookie != null)
        {
            val requestedSubject = Subject.Builder().sessionId(cookie.getValue()).buildSubject()
            if(requestedSubject.getSession(false) != null)
            {
                return Response.temporaryRedirect(URI("http://127.0.0.1:8080/rest/login/success")).build()
            }
        }*/
        return Response.status(200).entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Well, hello there!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      We're going to now talk to the GitHub API. Ready?\n" +
                "      <a href=\"https://github.com/login/oauth/authorize?redirect_uri=http://127.0.0.1:8080/rest/login/ssocallback&scope=user:email&client_id=c1371dc4d42722495100\n\">Click here</a> to begin!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build();
    }

    @GET
    @Path("ssocallback")
    fun callbackSSO(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?): Response{
        //will hopefully receive the request from github
        println(cookie!!.value)
        return Response.ok().entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Well, hello there!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "    You have been logged in successfully!\n" +
                "    Please proceed and utilize rest api endpoints\n" +
                "    <p>\n" +
                "    Here is an example endpoint: \n"+
                "    <a href = \"http://127.0.0.1:8080/rest/teams/GoldenStateWarriors\n\">Example Endpoint</a>\n"+
                "    </p>\n" +
                "    <a href=\"http://127.0.0.1:8080/rest/logout\n\">Click here</a> to logout!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }

    @GET
    @Path("error")
    fun errorlogin(): Response{
        return Response.status(401).entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Error Loggin in!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "    You were unable to be logged in\n" +
                "    Please proceed and utilize rest api endpoints\n" +
                "    <p>\n" +
                "    Here is an example endpoint: \n"+
                "    <a href = \"http://127.0.0.1:8080/rest/teams/GoldenStateWarriors\n\">Example Endpoint</a>\n"+
                "    </p>\n" +
                "    <a href=\"http://127.0.0.1:8080/rest/logout\n\">Click here</a> to logout!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }


    @GET
    @Path("success")
    fun successLogin(): Response{
        return Response.ok().entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Well, hello there!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "    You have been logged in successfully!\n" +
                "    Please proceed and utilize rest api endpoints\n" +
                "    <p>\n" +
                "    Here is an example endpoint: \n"+
                "    <a href = \"http://127.0.0.1:8080/rest/teams/GoldenStateWarriors\n\">Example Endpoint</a>\n"+
                "    </p>\n" +
                "    <a href=\"http://127.0.0.1:8080/rest/logout\n\">Click here</a> to logout!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }






}

