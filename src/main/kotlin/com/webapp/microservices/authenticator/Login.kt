package com.webapp.microservices.authenticator

import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import javax.ws.rs.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import javax.naming.InitialContext
import javax.servlet.http.HttpServletRequest
import javax.sql.DataSource
import javax.ws.rs.core.Cookie


@Path("login")
class Login{

    @GET
    @Produces("text/html")
    fun loginGithub(): Response{
        return Response.status(200).entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Well, hello there!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      To login via github, please use this link:\n" +
                "      <a href=\"https://github.com/login/oauth/authorize?redirect_uri=" +
                "http://127.0.0.1:8080/rest/login/ssocallback&scope=user:email" +
                "&client_id=c1371dc4d42722495100\n\">Click here</a> to login via SSO!</a>\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      To login via application, please use this link:\n" +
                "      <a href=\"/rest/login/local\n\">Click here" +
                "      </a> to login locally!</a>\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      To signup via application, please use the below link\n" +
                "      <a href=\"/rest/login/signup\n\"> " +
                "      Click here </a> to signup for application!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }

    @GET
    @Path("local")
    @Produces("text/html")
    fun loginLocal(): Response{
        return Response.status(200).entity("<html>\n" +
                " <head>\n" +
                "   <title> Login Form </title>" +
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                " </head>\n" +
                " <body>\n" +
                "   <br />" +
                "   <form method=\"post\" action=\"/rest/login/localcallback\">" +
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
    @Path("signup")
    @Produces("text/html")
    fun signupLocal(): Response{
        return Response.status(200).entity("<html>\n" +
                " <head>\n" +
                "   <title> SignUp Form </title>" +
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                " </head>\n" +
                " <body>\n" +
                "   <br />" +
                "   <form method=\"post\" action=\"/rest/login/signupcallback\">" +
                "     <p>" +
                "       Please enter in email address that will be used as your username" +
                "     </p>" +
                "     <p>" +
                "       Email: <input type=\"email\" name=\"j_username\"/>" +
                "     </p>" +
                "     <p>" +
                "       Password : <input type=\"password\" name=\"j_password\" />" +
                "     </p>" +
                "     <input type=\"submit\" value=\"SignUp\" />" +
                "   </form>" +
                " </body>\n" +
                "</html>").build()
    }

    @GET
    @Path("ssocallback")
    fun callbackSSO(@Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?){
        //this is where custom authenticator intercepts and tries to validate the github login request
    }

    @POST
    @Path("localcallback")
    fun callbackLocal(){
        //this is where custom authenticator intercepts and tries to validate the local login request
    }

    @POST
    @Path("signupcallback")
    @Consumes("application/x-www-form-urlencoded")
    fun callbackSignUp(@FormParam("j_username") email: String, @FormParam("j_password") pwd: String): Response{
        val ic = InitialContext()
        val myDataSource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDataSource)
        var exists = false
        transaction{
            val c = Users.select {
                Users.username.eq(email)
            }.count()
            if(c > 0)
            {
                exists = true
            }
            else{
                //does not exist yet must create an entry
                val userId = Users.insert{
                    it[username] = email
                    it[password] = pwd
                } get Users.id

                //create a new row in UserRole table
                //find the role id for admin
                val myList = ArrayList<String>()
                myList.add("admin")
                myList.add("guest")
                myList.add("poweruser")
                Roles.select{
                    Roles.name.inList(myList)
                }.forEach{
                    val adminId = it[Roles.id]
                    UserRole.insert{
                        it[uid] = userId
                        it[roleid] = adminId
                    }
                }
            }
        }
        if(exists)
        {
            return Response.status(200).entity("<html>\n" +
                " <head>\n" +
                "   <title> SignUp Form </title>" +
                "   <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" +
                " </head>\n" +
                " <body>\n" +
                "   <p>" +
                "     Error: Email already exists. Please pick different email or login!!" +
                "   </p" +
                "   <br />" +
                "   <form method=\"post\" action=\"/rest/login/signupcallback\">" +
                "     <p>" +
                "       Please enter in email address that will be used as your username" +
                "     </p>" +
                "     <p>" +
                "       Email: <input type=\"email\" name=\"j_username\"/>" +
                "     </p>" +
                "     <p>" +
                "       Password : <input type=\"password\" name=\"j_password\" />" +
                "     </p>" +
                "     <input type=\"submit\" value=\"SignUp\" />" +
                "   </form>" +
                " </body>\n" +
                "</html>").build()
        }
        return Response.ok().entity("<html>\n" +
                "  <head>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>\n" +
                "      Successfully Signed Up!\n" +
                "    </p>\n" +
                "    <p>\n" +
                "      Please proceed to login page via link below and login :)\n" +
                "    <p>\n" +
                "      <a href = \"/rest/login/local\n\">Login Page</a>\n"+
                "    </p>\n" +
                "  </body>\n" +
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
                "      <a href = \"/rest/hello\n\">Example Endpoint</a>\n"+
                "    </p>\n" +
                "      <a href=\"/rest/logout\n\">Click here</a> to logout!</a>\n" +
                "    </p>\n" +
                "  </body>\n" +
                "</html>").build()
    }

}
