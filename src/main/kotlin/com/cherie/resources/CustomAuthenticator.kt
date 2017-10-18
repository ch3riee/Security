package com.cherie.resources

import com.mashape.unirest.http.Unirest
import java.io.IOException
import java.util.Collections
import java.util.Enumeration
import java.util.Locale

import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import javax.servlet.http.HttpSession

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpHeaderValue
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.security.AbstractLoginService
import org.eclipse.jetty.security.Authenticator
import org.eclipse.jetty.security.ServerAuthException
import org.eclipse.jetty.security.UserAuthentication
import org.eclipse.jetty.security.authentication.DeferredAuthentication
import org.eclipse.jetty.security.authentication.LoginAuthenticator
import org.eclipse.jetty.security.authentication.SessionAuthentication
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.Authentication.User
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.UserIdentity
import org.eclipse.jetty.util.MultiMap
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.util.security.Credential
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.Serializable
import java.security.Principal
import java.util.ArrayList
import javax.naming.InitialContext
import javax.security.auth.Subject
import javax.sql.DataSource

/**
 * FORM Authenticator.
 *
 *
 * This authenticator implements form authentication will use dispatchers to
 * the login page if the [.__FORM_DISPATCH] init parameter is set to true.
 * Otherwise it will redirect.
 *
 *
 * The form authenticator redirects unauthenticated requests to a log page
 * which should use a form to gather username/password from the user and send them
 * to the /j_security_check URI within the context.  FormAuthentication uses
 * [SessionAuthentication] to wrap Authentication results so that they
 * are  associated with the session.
 */
class CustomAuthenticator() : LoginAuthenticator() {


    private var _formErrorPage: String? = null
    private var _formErrorPath: String? = null
    private var _formLoginPage: String? = null
    private var _formLoginPath: String? = null
    private var _alwaysSaveUri: Boolean = false
    private var _dispatch: Boolean = false

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /**
     * If true, uris that cause a redirect to a login page will always
     * be remembered. If false, only the first uri that leads to a login
     * page redirect is remembered.
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=379909
     * @param alwaysSave true to always save the uri
     */
    var alwaysSaveUri: Boolean = false

    /* ------------------------------------------------------------ */
    constructor(login: String?, error: String?, dispatch: Boolean) : this() {
        if (login != null)
            setLoginPage(login)
        if (error != null)
            setErrorPage(error)

    }

    /* ------------------------------------------------------------ */
    /**
     * @see org.eclipse.jetty.security.authentication.LoginAuthenticator.setConfiguration
     */
    override fun setConfiguration(configuration: Authenticator.AuthConfiguration) {
        super.setConfiguration(configuration)
        val login = configuration.getInitParameter(__FORM_LOGIN_PAGE)
        if (login != null) {
            setLoginPage(login)
        }

        val error = configuration.getInitParameter(__FORM_ERROR_PAGE)
        if (error != null) {
            setErrorPage(error)
        }
    }

    /* ------------------------------------------------------------ */
    override fun getAuthMethod(): String {
        return Constraint.__FORM_AUTH
    }

    /* ------------------------------------------------------------ */
    private fun setLoginPage(path: String) {
        var path = path
        if (!path.startsWith("/")) {
            LOG.warn("form-login-page must start with /")
            path = "/" + path
        }
        _formLoginPage = path
        _formLoginPath = path
        if (_formLoginPath!!.indexOf('?') > 0)
            _formLoginPath = _formLoginPath!!.substring(0, _formLoginPath!!.indexOf('?'))
    }

    /* ------------------------------------------------------------ */
    private fun setErrorPage(path: String?) {
        var path = path
        if (path == null || path.trim { it <= ' ' }.length == 0) {
            _formErrorPath = null
            _formErrorPage = null
        } else {
            if (!path.startsWith("/")) {
                LOG.warn("form-error-page must start with /")
                path = "/" + path
            }
            _formErrorPage = path
            _formErrorPath = path

            if (_formErrorPath!!.indexOf('?') > 0)
                _formErrorPath = _formErrorPath!!.substring(0, _formErrorPath!!.indexOf('?'))
        }
    }


    /* ------------------------------------------------------------ */
    override fun login(username: String, password: Any, request: ServletRequest): UserIdentity? {
        println("in login")
        //this is the most important to override. Called by login down below
        //val user = super.login(username, password, request)
        //this^ calls the loginservice login method
        //need to create a UserIdentity and create database connection and update if user.
        if (username == null)
            return null
        //Get sso token and then subsequently get the user email as username
        var user_email: String = username
        var authenticated: Boolean = false
        if(password == null) {
            user_email = ssoHelper(username)
        }
        else{
            //this is a local authentication attempt, check database to see if credentials are correct
            println("TRYING TO LOGIN LOCALLY IN LOGIN")
            val ic = InitialContext()
            val myDatasource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
            Database.connect(myDatasource)
            transaction{
                val curruser = Users.select {
                    Users.username.eq(user_email)
                }
                println(curruser.count())
                if (curruser.count() == 1)
                {
                    var cred: String? = null
                    //in this case we do not create a new entry unless was on the signup page.
                    //for sso we create one if not in the db yet.
                    curruser.forEach{
                        cred = it[Users.password]
                    }
                    println(cred)
                    println(password)
                    if(cred!!.equals(password))
                    {
                        println("authentication is true")
                        authenticated = true
                    }

                }
            }
        }
        if(authenticated == false)
        {
            println("authentication did not work for local login")
           return null //was not authenticated for local login
        }
        //if we made it this far than we have authenticated both sso and local login
        val user = createUserIdentity(user_email, password as String)
        if (user != null) {
            val session = (request as HttpServletRequest).getSession(true)
            val cached = SessionAuthentication(authMethod, user, password)
            session.setAttribute(SessionAuthentication.__J_AUTHENTICATED, cached)
            println(session)
        }
        return user
    }

    fun ssoHelper(code: String): String{
        val jsonResponse = Unirest.post("https://github.com/login/oauth/access_token")
                .header("accept", "application/json")
                .queryString("client_id", "c1371dc4d42722495100")
                .queryString("client_secret", "9c445d6a689f8c5b94ca9a9d5669a19abe86feec")
                .queryString("code", code)
                .asJson()
        val obj = jsonResponse.getBody().getObject()
        val a_token: String = obj.getString("access_token")
        val userResponse = Unirest.get("https://api.github.com/user/emails")
                .header("accept", "application/json")
                .queryString("access_token", a_token)
                .asJson()
        val userArray = userResponse.getBody().getArray()
        val user_email = userArray.getJSONObject(0).getString("email")
        return user_email
    }

    fun createUserIdentity(user_name: String, pwd: String?): UserIdentity? {
        //code for accessing database
        val roles = ArrayList<String>()
        val ic = InitialContext()
        val myDatasource = ic.lookup("java:comp/env/jdbc/userStore") as DataSource
        Database.connect(myDatasource)
        transaction{
            val curruser = Users.select {
                Users.username.eq(user_name)
            }
            if (curruser.count() < 1)
            {
                //there is no entry yet create a new row in users table
                val userid = Users.insert{
                    it[username] = user_name
                    it[password] = pwd
                } get Users.id

                //create a new row in roles table
                val rid = Roles.insert{
                    it[name] = "guest"
                    it[desc]= "default role for all user accounts"
                } get Roles.id

                //create an entry in the user-role table
                UserRole.insert{
                    it[uid] = userid
                    it[roleid] = rid
                }
            }
            else{
                //just need to grab the role information
                var currid: Int = 0
                curruser.forEach{
                    currid = it[Users.id]
                }
                UserRole.select{
                    UserRole.uid.eq(currid)
                }.forEach{
                    Roles.select{
                        Roles.id.eq(it[UserRole.roleid])
                    }.forEach{
                        roles.add(it[Roles.name])
                    }
                }
            }
        }
        val cred = Credential.getCredential(pwd)
        val userPrincipal = UserPrincipal(user_name, cred) //nul for credential?
        if (userPrincipal != null) {
            //safe to load the roles
            val subject = Subject()
            subject.principals.add(userPrincipal)
            subject.privateCredentials.add(cred)
            if (roles != null)
                for (role in roles!!)
                    subject.principals.add(RolePrincipal(role))
            subject.setReadOnly()
            println("Creating user identity")
            return _identityService.newUserIdentity(subject, userPrincipal, roles.toTypedArray())
        }
        //should not get here
        return null
    }


    /* ------------------------------------------------------------ */
    /**
     * UserPrincipal
     */
    open class UserPrincipal/* -------------------------------------------------------- */
    (private val _name: String, private val _credential: Credential?) : Principal, Serializable {

        /* -------------------------------------------------------- */
        fun authenticate(credentials: Any): Boolean {
            return _credential != null && _credential.check(credentials)
        }

        /* -------------------------------------------------------- */
        fun authenticate(c: Credential?): Boolean {
            return _credential != null && c != null && _credential == c
        }

        /* ------------------------------------------------------------ */
        override fun getName(): String {
            return _name
        }


        /* -------------------------------------------------------- */
        override fun toString(): String {
            return _name
        }

        companion object {
            private const val serialVersionUID = -6226920753748399662L
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * RolePrincipal
     */
    class RolePrincipal(private val _roleName: String) : Principal, Serializable {
        override fun getName(): String {
            return _roleName
        }

        companion object {
            private const val serialVersionUID = 2998397924051854402L
        }
    }


    /* ------------------------------------------------------------ */
    override fun prepareRequest(request: ServletRequest?) {
        //if this is a request resulting from a redirect after auth is complete
        //(ie its from a redirect to the original request uri) then due to
        //browser handling of 302 redirects, the method may not be the same as
        //that of the original request. Replace the method and original post
        //params (if it was a post).
        //
        //See Servlet Spec 3.1 sec 13.6.3
        val httpRequest = request as HttpServletRequest?
        val session = httpRequest!!.getSession(false)
        if (session == null || session.getAttribute(SessionAuthentication.__J_AUTHENTICATED) == null)
            return  //not authenticated yetv
        val juri = session.getAttribute(__J_URI) as String?
        if (juri == null || juri.length == 0)
            return  //no original uri saved
        val method = session.getAttribute(__J_METHOD) as String
        if (method == null || method.length == 0)
            return  //didn't save original request method

        val buf = httpRequest.requestURL
        if (httpRequest.queryString != null)
            buf.append("?").append(httpRequest.queryString)

        if (juri != buf.toString())
            return  //this request is not for the same url as the original

        //restore the original request's method on this request
        if (LOG.isDebugEnabled) LOG.debug("Restoring original method {} for {} with method {}", method, juri, httpRequest.method)
        val base_request = Request.getBaseRequest(request!!)
        base_request.method = method
    }

    /* ------------------------------------------------------------ */
    @Throws(ServerAuthException::class)
    override fun validateRequest(req: ServletRequest, res: ServletResponse, mandatory: Boolean): Authentication {
        val request = req as HttpServletRequest
        val response = res as HttpServletResponse
        val base_request = Request.getBaseRequest(request)
        val base_response = base_request.response

        var uri: String? = request.requestURI
        if (uri == null)
            uri = URIUtil.SLASH

        if (isLoginOrErrorPage(URIUtil.addPaths(request.servletPath, request.pathInfo)) && !DeferredAuthentication.isDeferred(response))
            return DeferredAuthentication(this)

        var session: HttpSession? = request.getSession(true)
        //println(session!!.getId())

        //if unable to create a session, user must be
        //unauthenticated
        if (session == null)
        {
            return Authentication.UNAUTHENTICATED
        }


        try {
            // Handle a request for authentication.
            if (isJSecurityCheck(uri)) {
                val tempCode = request.getParameter("code")

                val user = login(tempCode, "", request)
                session = request.getSession(false)
                if (user != null) {
                    // Redirect to original request
                    var nuri: String? = "hi"
                    var form_auth: CustomAuthentication = CustomAuthentication(authMethod, user)
                    synchronized(session) {
                        nuri = session!!.getAttribute(__J_URI) as String?

                        if (nuri == null || nuri!!.length == 0) {
                            nuri = "http://127.0.0.1:8080/rest/login/success"
                            if (nuri!!.length == 0)
                                nuri = URIUtil.SLASH
                        }
                    }

                    response.setContentLength(0)
                    val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version) HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(nuri))
                    println("just redirected, returning form_auth")
                    return form_auth
                }

                // not authenticated
                if (LOG.isDebugEnabled)
                    LOG.debug("Form authentication FAILED")
                if (_formErrorPage == null) {
                    LOG.debug("auth failed ->403")
                    response?.sendError(HttpServletResponse.SC_FORBIDDEN)
                } else if (_dispatch) {
                    val dispatcher = request.getRequestDispatcher(_formErrorPage)
                    response.setHeader(HttpHeader.CACHE_CONTROL.asString(), HttpHeaderValue.NO_CACHE.asString())
                    response.setDateHeader(HttpHeader.EXPIRES.asString(), 1)
                    dispatcher.forward(FormRequest(request), FormResponse(response))
                } else {
                    val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version) HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(URIUtil.addPaths(request.contextPath, _formErrorPage)))
                }

                return Authentication.SEND_FAILURE
            }
            if(isJLocal(uri))
            {
                println("INSIDE J LOCAL")
                val username = request.getParameter(__J_USERNAME)
                val password = request.getParameter(__J_PASSWORD)
                val user = login(username, password , request)
                println(username)
                println(password)
                session = request.getSession(false)
                if (user != null) {
                    println("inside isJLocal")
                    // Redirect to original request
                    var nuri: String? = "hi"
                    var form_auth: CustomAuthentication = CustomAuthentication(authMethod, user)
                    synchronized(session) {
                        nuri = session!!.getAttribute(__J_URI) as String?

                        if (nuri == null || nuri!!.length == 0) {
                            nuri = "http://127.0.0.1:8080/rest/login/success"
                            if (nuri!!.length == 0)
                                nuri = URIUtil.SLASH
                        }
                    }

                    response.setContentLength(0)
                    val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version) HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(nuri))
                    println("just redirected, returning form_auth for local login")
                    return form_auth
                }

                // not authenticated
                if (LOG.isDebugEnabled)
                    LOG.debug("Form authentication FAILED")
                if (_formErrorPage == null) {
                    LOG.debug("auth failed ->403")
                    response?.sendError(HttpServletResponse.SC_FORBIDDEN)
                } else if (_dispatch) {
                    val dispatcher = request.getRequestDispatcher(_formErrorPage)
                    response.setHeader(HttpHeader.CACHE_CONTROL.asString(), HttpHeaderValue.NO_CACHE.asString())
                    response.setDateHeader(HttpHeader.EXPIRES.asString(), 1)
                    dispatcher.forward(FormRequest(request), FormResponse(response))
                } else {
                    val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version) HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                    base_response.sendRedirect(redirectCode, response.encodeRedirectURL(URIUtil.addPaths(request.contextPath, _formErrorPage)))
                }

                return Authentication.SEND_FAILURE

            }//end of local login sequence

            // Look for cached authentication
            val authentication = session!!.getAttribute(SessionAuthentication.__J_AUTHENTICATED) as Authentication?
            if (authentication != null) {
                // Has authentication been revoked?
                if (authentication is Authentication.User &&
                        _loginService != null &&
                        !_loginService.validate(authentication.userIdentity)) {
                    session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED)
                } else {
                    synchronized(session) {
                        val j_uri = session!!.getAttribute(__J_URI) as String?
                        if (j_uri != null) {
                            //check if the request is for the same url as the original and restore
                            //params if it was a post
                            LOG.debug("auth retry {}->{}", authentication, j_uri)
                            val buf = request.requestURL
                            if (request.queryString != null)
                                buf.append("?").append(request.queryString)
                        }
                    }
                    LOG.debug("auth {}", authentication)
                    return authentication
                }
            }

            // if we can't send challenge
            if (DeferredAuthentication.isDeferred(response)) {
                LOG.debug("auth deferred {}", session.id)
                return Authentication.UNAUTHENTICATED
            }

            // remember the current URI
            synchronized(session) {
                // But only if it is not set already, or we save every uri that leads to a login form redirect
                if (session!!.getAttribute(__J_URI) == null || _alwaysSaveUri) {
                    val buf = request.requestURL
                    if (request.queryString != null)
                        buf.append("?").append(request.queryString)
                    session!!.setAttribute(__J_URI, buf.toString())
                    session!!.setAttribute(__J_METHOD, request.method)

                    if (MimeTypes.Type.FORM_ENCODED.`is`(req.getContentType()) && HttpMethod.POST.`is`(request.method)) {
                        val formParameters = MultiMap<String>()
                        base_request.extractFormParameters(formParameters)
                        session!!.setAttribute(__J_POST, formParameters)
                    }
                }
            }

            // send the the challenge
            if (_dispatch) {
                LOG.debug("challenge {}=={}", session.id, _formLoginPage)
                val dispatcher = request.getRequestDispatcher(_formLoginPage)
                response.setHeader(HttpHeader.CACHE_CONTROL.asString(), HttpHeaderValue.NO_CACHE.asString())
                response.setDateHeader(HttpHeader.EXPIRES.asString(), 1)
                dispatcher.forward(FormRequest(request), FormResponse(response))
            } else {
                val regex = Regex("/login(?=/|$)")
                val match = regex.containsMatchIn(uri)
                println(uri)
                println(match)
                if (match != false)
                {
                   //we do not want to redirect or send the challenge
                    return Authentication.NOT_CHECKED

                }
                LOG.debug("challenge {}->{}", session.id, _formLoginPage)
                val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version) HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                base_response.sendRedirect(redirectCode, response.encodeRedirectURL(URIUtil.addPaths(request.contextPath, _formLoginPage)))
            }
            return Authentication.SEND_CONTINUE
        } catch (e: IOException) {
            throw ServerAuthException(e)
        } catch (e: ServletException) {
            throw ServerAuthException(e)
        }


    }

    /* ------------------------------------------------------------ */
    fun isJSecurityCheck(uri: String): Boolean {
        val jsc = uri.indexOf(__J_SECURITY_CHECK)

        if (jsc < 0)
            return false
        val e = jsc + __J_SECURITY_CHECK.length
        if (e == uri.length)
            return true
        val c = uri[e]
        return c == ';' || c == '#' || c == '/' || c == '?'
    }

    fun isJLocal(uri: String): Boolean {
        val jsc = uri.indexOf(__J_LOCAL)

        if (jsc < 0)
            return false
        val e = jsc + __J_LOCAL.length
        if (e == uri.length)
            return true
        val c = uri[e]
        return c == ';' || c == '#' || c == '/' || c == '?'
    }

    /* ------------------------------------------------------------ */
    fun isLoginOrErrorPage(pathInContext: String?): Boolean {
        return pathInContext != null && (pathInContext == _formErrorPath || pathInContext == _formLoginPath)
    }

    /* ------------------------------------------------------------ */
    @Throws(ServerAuthException::class)
    override fun secureResponse(req: ServletRequest, res: ServletResponse, mandatory: Boolean, validatedUser: User?): Boolean {
        return true
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class FormRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

        override fun getDateHeader(name: String): Long {
            return if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) -1 else super.getDateHeader(name)
        }

        override fun getHeader(name: String): String? {
            return if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) null else super.getHeader(name)
        }

        override fun getHeaderNames(): Enumeration<String> {
            return Collections.enumeration(Collections.list(super.getHeaderNames()))
        }

        override fun getHeaders(name: String): Enumeration<String> {
            return if (name.toLowerCase(Locale.ENGLISH).startsWith("if-")) Collections.enumeration(emptyList()) else super.getHeaders(name)
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    protected class FormResponse(response: HttpServletResponse) : HttpServletResponseWrapper(response) {

        override fun addDateHeader(name: String, date: Long) {
            if (notIgnored(name))
                super.addDateHeader(name, date)
        }

        override fun addHeader(name: String, value: String) {
            if (notIgnored(name))
                super.addHeader(name, value)
        }

        override fun setDateHeader(name: String, date: Long) {
            if (notIgnored(name))
                super.setDateHeader(name, date)
        }

        override fun setHeader(name: String, value: String) {
            if (notIgnored(name))
                super.setHeader(name, value)
        }

        private fun notIgnored(name: String): Boolean {
            return if (HttpHeader.CACHE_CONTROL.`is`(name) ||
                    HttpHeader.PRAGMA.`is`(name) ||
                    HttpHeader.ETAG.`is`(name) ||
                    HttpHeader.EXPIRES.`is`(name) ||
                    HttpHeader.LAST_MODIFIED.`is`(name) ||
                    HttpHeader.AGE.`is`(name)) false else true
        }
    }

    /* ------------------------------------------------------------ */
    /** This Authentication represents a just completed Form authentication.
     * Subsequent requests from the same user are authenticated by the presents
     * of a [SessionAuthentication] instance in their session.
     */
    class CustomAuthentication(method: String, userIdentity: UserIdentity) : UserAuthentication(method, userIdentity), Authentication.ResponseSent {

        override fun toString(): String {
            return "Form" + super.toString()
        }
    }

    companion object {
        private val LOG = Log.getLogger(CustomAuthenticator::class.java)

        val __FORM_LOGIN_PAGE = "org.eclipse.jetty.security.form_login_page"
        val __FORM_ERROR_PAGE = "org.eclipse.jetty.security.form_error_page"
        val __J_URI = "org.eclipse.jetty.security.form_URI"
        val __J_POST = "org.eclipse.jetty.security.form_POST"
        val __J_METHOD = "org.eclipse.jetty.security.form_METHOD"
        val __J_SECURITY_CHECK = "/rest/login/ssocallback"
        val __J_LOCAL = "/rest/login/localcallback"
        val __J_USERNAME = "j_username"
        val __J_PASSWORD = "j_password"
    }
}

