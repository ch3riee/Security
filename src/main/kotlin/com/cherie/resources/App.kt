package com.cherie.resources

import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHolder
import org.glassfish.jersey.servlet.ServletContainer
import org.glassfish.jersey.server.ResourceConfig
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.server.session.*


object App{

    @JvmStatic fun main (args: Array<String>){
        var config = ResourceConfig()
        config.packages("com.cherie.resources")
        val servlet = ServletHolder(ServletContainer(config))
        val server = Server(8080)
        val context = ServletContextHandler(server, "/")
        context.addServlet(servlet, "/rest/*")
        val sessionManager = SessionHandler()
        sessionManager.setUsingCookies(true)
       // sessionManager.setSessionIdPathParameterName("none")
        val sessionCache = DefaultSessionCache(sessionManager)
        val db = JDBCSessionDataStore()
        val myAdaptor = DatabaseAdaptor()
        myAdaptor.setDriverInfo("org.postgresql.Driver", "jdbc:postgresql://db-session:5432/session?user=jetty&password=jettypass" )
        db.setDatabaseAdaptor(myAdaptor)
        sessionCache.setSessionDataStore(db)
        sessionManager.setSessionCache(sessionCache)
        context.sessionHandler = sessionManager
        val csh = ConstraintSecurityHandler()
        context.securityHandler = csh


        val constraint = Constraint()
        constraint.name = "auth"
        constraint.setRoles(arrayOf("user"))
        constraint.authenticate = true

        val cm = ConstraintMapping()
        cm.constraint = constraint
        cm.pathSpec = "/rest/teams/*"

        csh.authenticator = CustomAuthenticator()
        csh.realmName = "MyRealm" //does this realm thing matter without the login service?
        csh.addConstraintMapping(cm)
        //csh.loginService = loginService
        csh.setInitParameter(CustomAuthenticator.__FORM_LOGIN_PAGE, "/rest/login")
        csh.setInitParameter(CustomAuthenticator.__FORM_ERROR_PAGE, "/rest/login/error") //our endpoints
        csh.loginService = HashLoginService()
        AccountDb.init()




        try{
            server.start()
            server.join()
        }catch(e: InterruptedException){
            server.destroy()
        }




    }


}

