package com.cherie.resources

import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.glassfish.jersey.servlet.ServletContainer
import org.glassfish.jersey.server.ResourceConfig
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.HashLoginService
import org.eclipse.jetty.server.session.*
import org.postgresql.ds.PGSimpleDataSource
import org.eclipse.jetty.plus.jndi.Resource
import org.eclipse.jetty.server.*
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature
import org.glassfish.jersey.jackson.JacksonFeature

object App{

    @JvmStatic fun main (args: Array<String>){
        var config = ResourceConfig()
        config.packages("com.cherie.resources")
        config.register(RolesAllowedDynamicFeature::class.java)
        config.register(JacksonFeature::class.java)

        val servlet = ServletHolder(ServletContainer(config))
        val server = Server()

        /*for to be used behind Nginx... (mostly for redirect)*/
        val http_config = HttpConfiguration()
        http_config.outputBufferSize = 32768
        http_config.addCustomizer(ForwardedRequestCustomizer())
        val http = ServerConnector(server, HttpConnectionFactory(http_config))
        http.port = 8081
        http.idleTimeout = 30000
        server.addConnector(http)
        /*----------------------------------------------------*/

        val context = ServletContextHandler(server, "/")
        context.addServlet(servlet, "/rest/*")

        val sessionManager = SessionHandler()
        sessionManager.setUsingCookies(true)
        sessionManager.maxInactiveInterval = 300
        sessionManager.CookieConfig().maxAge = 60 * 5
        val sessionCache = DefaultSessionCache(sessionManager)
        val db = JDBCSessionDataStore()
        val myAdaptor = DatabaseAdaptor()
        myAdaptor.setDriverInfo("org.postgresql.Driver", "jdbc:postgresql://db-session:5432/session?user=jetty&password=jettypass" )
        db.setDatabaseAdaptor(myAdaptor)
        sessionCache.sessionDataStore = db
        sessionManager.sessionCache = sessionCache
        context.sessionHandler = sessionManager
        val csh = ConstraintSecurityHandler()
        context.securityHandler = csh

        csh.authenticator = CustomAuthenticator()
        csh.setInitParameter(CustomAuthenticator.__FORM_LOGIN_PAGE, "/rest/login")
        csh.setInitParameter(CustomAuthenticator.__FORM_ERROR_PAGE, "/rest/login/error") //our endpoints
        csh.loginService = HashLoginService()
        val idService = CustomIdentityService()
        csh.loginService.identityService = idService
        csh.identityService = idService

        val simpleDataSource = PGSimpleDataSource()
        simpleDataSource.serverName = "db-account"
        simpleDataSource.databaseName = "account"
        simpleDataSource.user = "jetty"
        simpleDataSource.password = "jettypass"
        val jndiName = "jdbc/userStore"
        val myDataSource = Resource("java:comp/env/" + jndiName, simpleDataSource)
        server.setAttribute("userStore", myDataSource)

        try{
            server.start()
            server.join()
        }catch(e: InterruptedException){
            server.destroy()
        }




    }


}

