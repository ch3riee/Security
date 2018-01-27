package com.webapp.microservices.authenticator

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sun.security.rsa.RSAPrivateCrtKeyImpl
import java.util.*
import javax.naming.InitialContext
import javax.servlet.http.HttpServletRequest
import javax.sql.DataSource
import javax.ws.rs.core.Cookie
import javax.ws.rs.core.NewCookie

@Path("/public/gateway/session/login")
class Login{

    @GET
    @Produces("text/html")
    fun loginGithub(@Context request: HttpServletRequest): Response{
        val saved = request.getQueryString()
        //The cookie is only for the SSO caveat to make sure we have the correct original request url
        val c = Cookie("j_uri", request.getParameter("j_uri"), "/", request.serverName)
        if(saved != null && (request.getSession(false) == null))
        {
            //for the situation where jetty has no session prior to hit login page but nginx has already redirected
            //testid in <meta> with the hash (md5)
            return Response.status(200).entity("<html>\n" +
                    "  <head>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "    <p>\n" +
                    "      Well, hello there!\n" +
                    "    </p>\n" +
                    "    <p>\n" +
                    "      To login via github, please\n" +
                    "      <a href=\"https://github.com/login/oauth/authorize?redirect_uri=" +
                    "http://127.0.0.1:8080/rest/callback/gateway/ssocallback&scope=user:email" +
                    "&client_id=c1371dc4d42722495100 \n\">Click here</a> to login via SSO!</a>\n" +
                    "    </p>\n" +
                    "    <p>\n" +
                    "      To login via application, please enter login information below\n" +
                    "   <form method=\"post\" action=\"/rest/callback/gateway/localcallback" + "?" + saved + "\">" +
                    "      <p>" +
                    "        Email: <input type=\"email\" name=\"j_username\"/>" +
                    "      </p>" +
                    "      <p>" +
                    "        Password : <input type=\"password\" name=\"j_password\" />" +
                    "      </p>" +
                    "      <input type=\"submit\" value=\"Login\" />" +
                    "   </form>" +
                    "    </p>\n" +
                    "    <p>\n" +
                    "      To continue as guest, please\n" +
                    "      <a href =\" /rest/callback/gateway/guestcallback" + "?" + saved + "\n\"> " +
                    "      Click here </a> as guest</a>\n" +
                    "    </p>\n" +
                    "  </body>\n" +
                    "</html>").cookie(NewCookie(c)).build()

        }

        //normal situation
        return Response.status(200).entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Well, hello there!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      To login via github, please\n" +
                "      <a href=\"https://github.com/login/oauth/authorize?redirect_uri=" +
                "http://127.0.0.1:8080/rest/callback/gateway/ssocallback&scope=user:email" +
                "&client_id=c1371dc4d42722495100\n\">Click here</a> to login via SSO!</a>\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      To login via application, please\n" +
                "   <form method=\"post\" action=\"/rest/callback/gateway/localcallback\">" +
                "      <p>" +
                "        Email: <input type=\"email\" name=\"j_username\"/>" +
                "      </p>" +
                "      <p>" +
                "        Password : <input type=\"password\" name=\"j_password\" />" +
                "      </p>" +
                "      <input type=\"submit\" value=\"Login\" />" +
                "   </form>" +
                "    </p>\n" +
                "    <p>\n" +
                "      To continue as guest, please\n" +
                "      <a href =\" /rest/callback/gateway/guestcallback\n\"> " +
                "      Click here </a> as guest</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }

    @GET
    @Path("local")
    @Produces("text/html")
    fun loginLocal(@Context request: HttpServletRequest): Response{
        println("Inside loginlocal: " + request.requestURI)
        return Response.status(200).entity("<html>\n" +
                " <head>\n" +
                "   <title> Login Form </title>" +
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                " </head>\n" +
                " <body>\n" +
                "   <br />" +
                "   <form method=\"post\" action=\"/rest/callback/gateway/localcallback\">" +
                "      <p>" +
                "        Email: <input type=\"email\" name=\"j_username\"/>" +
                "      </p>" +
                "      <p>" +
                "        Password : <input type=\"password\" name=\"j_password\" />" +
                "      </p>" +
                "      <input type=\"submit\" value=\"Login\" />" +
                "   </form>" +
                " </body>\n" +
                "</html>").build()
    }





    @GET
    @Path("error")
    fun errorLogin(): Response{
        return Response.status(401).entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Error Loggin in!\n" +
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
                "      You have been logged in successfully!\n" +
                "      Please proceed and utilize rest api endpoints\n" +
                "    <p>\n" +
                "      Here is an example endpoint: \n"+
                "      <a href = \"/rest/public/gateway/sample/hello\n\">Example Endpoint</a>\n"+
                "    </p>\n" +
                "      <a href=\"/rest/public/gateway/session/logout\n\">Click here</a> to logout!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }

}

