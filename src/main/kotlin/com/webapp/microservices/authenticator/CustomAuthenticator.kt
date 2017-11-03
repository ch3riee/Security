package com.webapp.microservices.authenticator

import com.mashape.unirest.http.Unirest
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm

import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper
import javax.servlet.http.HttpSession
import javax.naming.InitialContext
import javax.security.auth.Subject
import javax.sql.DataSource

import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.http.HttpHeaderValue
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.security.Authenticator
import org.eclipse.jetty.security.ServerAuthException
import org.eclipse.jetty.security.UserAuthentication
import org.eclipse.jetty.security.authentication.DeferredAuthentication
import org.eclipse.jetty.security.authentication.LoginAuthenticator
import org.eclipse.jetty.server.Authentication
import org.eclipse.jetty.server.Authentication.User
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.UserIdentity
import org.eclipse.jetty.util.MultiMap
import org.eclipse.jetty.util.URIUtil
import org.eclipse.jetty.util.log.Log
import org.eclipse.jetty.util.security.Constraint
import org.eclipse.jetty.util.security.Credential

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

import sun.security.rsa.RSAPrivateCrtKeyImpl
import java.io.*
import java.security.Principal
import java.util.*
import javax.servlet.http.Cookie


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
        if (username == null)
            return null

        var user_email: String = username
        var authenticated = false
        var flag = false
        if((password as String).isEmpty()) {
            flag = true
            //Get sso token and then subsequently get the user email as username
            user_email = ssoHelper(username)
            Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
            transaction {
                val c = Users.select {
                    Users.username.eq(user_email)
                }.count()
                if (c == 0) {
                    //does not exist yet must create an entry
                    val userId = Users.insert {
                        it[Users.username] = user_email
                        it[Users.password] = ""
                    } get Users.id
                    //create a new row in UserRole table
                    //find the role id for admin
                    val myList = ArrayList<String>()
                    myList.add("admin") //for testing purposes!
                    myList.add("guest")
                    myList.add("user")
                    Roles.select {
                        Roles.name.inList(myList)
                    }.forEach {
                        val adminId = it[Roles.id]
                        UserRole.insert {
                            it[uid] = userId
                            it[roleid] = adminId
                        }
                    }
                }
                authenticated = true
            }
        }
        else{
            //this is a local authentication attempt, check database to see if credentials are correct
            Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
            transaction{
                val currUser = Users.select {
                    Users.username.eq(user_email)
                }
                if (currUser.count() == 1)
                {
                    currUser.forEach{
                        if(it[Users.password].isEmpty()) return@transaction
                        if(it[Users.password] == password) authenticated = true
                    }
                }
            }
        }
        if(!authenticated) return null

        //if we made it this far than we have authenticated both sso and local login
        val user = createUserIdentity(user_email, password) as CustomUserIdentity?
        user?.let {
            val session = (request as HttpServletRequest).getSession(true)
            session.setAttribute(SessionAuthentication.__J_AUTHENTICATED,
                    SessionAuthentication(authMethod, user, password))
            session.setAttribute("JwtToken",generateJWT(user_email,flag ))
        }
        return user
    }

    fun generateJWT(email: String, flag: Boolean): String {
        val (roles, perms) = getRolesPerms(email)
        var privateKey = (this::class.java.classLoader).getResource("pki/Private.key")
                                                       .readText()
                                                       .toByteArray()
        privateKey = Base64.getDecoder().decode(privateKey)
        val myMap = HashMap<String, Any>()
        myMap.put("Roles", roles.toTypedArray())
        myMap.put("Permissions", perms.toTypedArray())
        val type = if(flag) "sso" else "local"
        myMap.put("LoginType", type)
        return Jwts.builder()
                      .setClaims(myMap)
                      .setSubject(email)
                      .signWith(SignatureAlgorithm.RS512, RSAPrivateCrtKeyImpl.newKey(privateKey))
                      .compact()
    }

    fun ssoHelper(code: String): String{
        val jsonResponse = Unirest.post("https://github.com/login/oauth/access_token")
                .header("accept", "application/json")
                .queryString("client_id", "c1371dc4d42722495100")
                .queryString("client_secret", "9c445d6a689f8c5b94ca9a9d5669a19abe86feec")
                .queryString("code", code)
                .asJson()
        val obj = jsonResponse.body.getObject()
        val userResponse = Unirest.get("https://api.github.com/user/emails")
                .header("accept", "application/json")
                .queryString("access_token", obj.getString("access_token"))
                .asJson()
        return userResponse.body.getArray().getJSONObject(0).getString("email")
    }


    fun createUserIdentity(user_name: String, pwd: String?): UserIdentity? {
        val cred = Credential.getCredential(pwd)
        val userPrincipal = UserPrincipal(user_name, cred)
        val subject = Subject()
        subject.principals.add(userPrincipal)
        subject.privateCredentials.add(cred)
        val roles = getRoles(user_name, pwd)
        for (role in roles) {
               subject.principals.add(RolePrincipal(role))
            }
        subject.setReadOnly()
        return _identityService.newUserIdentity(subject, userPrincipal, roles.toTypedArray())
    }

    fun getRoles(user_name: String, pwd: String?): ArrayList<String>{
        val roles = ArrayList<String>()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction{
            var currId = 0
            Users.select {
                Users.username.eq(user_name)
            }.forEach{
                currId = it[Users.id]
            }

            UserRole.select{
                UserRole.uid.eq(currId)
            }.forEach {
                Roles.select {
                    Roles.id.eq(it[UserRole.roleid])
                }.forEach {
                    roles.add(it[Roles.name])
                }
            }
        }
        return roles
    }

    fun getRolesPerms(user_name: String): Pair<ArrayList<String>, ArrayList<String>> {
        val roles = ArrayList<String>()
        val permissions = HashSet<String>()
        val roleIds = ArrayList<Int>()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction{
            var currId = 0
            Users.select {
                Users.username.eq(user_name)
            }.forEach{
                currId = it[Users.id]
            }

            UserRole.select{
                UserRole.uid.eq(currId)
            }.forEach{
                Roles.select{
                    Roles.id.eq(it[UserRole.roleid])
                }.forEach{
                    roles.add(it[Roles.name]) //gives us list of role names to save in the subject
                    roleIds.add(it[Roles.id]) //gives us list of role ids so that we can find matching permissions
                }
            }

            RolePerm.select{ RolePerm.roleid.inList(roleIds)}.forEach{
                Permissions.select{
                    Permissions.id.eq(it[RolePerm.pid])
                }.forEach{
                    permissions.add(it[Permissions.operation])
                }
            }
            }
        return Pair(roles, ArrayList<String>(permissions))

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
        if (juri == null || juri.isEmpty())
            return  //no original uri saved
        val method = session.getAttribute(__J_METHOD) as String
        if (method == null || method.isEmpty())
            return  //didn't save original request method

        val buf = httpRequest.requestURL
        if (httpRequest.queryString != null)
            buf.append("?").append(httpRequest.queryString)

        if (juri != buf.toString())
            return  //this request is not for the same url as the original

        //restore the original request's method on this request
        if (LOG.isDebugEnabled) LOG.debug("Restoring original" +
                "method {} for {} with method {}", method, juri, httpRequest.method)
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

        if (isLoginOrErrorPage(URIUtil.addPaths(request.servletPath, request.pathInfo)) &&
                !DeferredAuthentication.isDeferred(response))
            return DeferredAuthentication(this)

        var session: HttpSession? = request.getSession(true)

        //if unable to create a session, user must be unauthenticated
        if (session == null) return Authentication.UNAUTHENTICATED

        try {
            // Handle a request for authentication. )
            var user: UserIdentity? = null
            var checked = false
            if (isJSecurityCheck(uri)) {
                val tempCode = request.getParameter("code")
                checked = true
                user = login(tempCode, "", request)
            }
            else if(isJLocal(uri)) {
                val username = request.getParameter(__J_USERNAME)
                val password = request.getParameter(__J_PASSWORD)
                user = login(username, password, request)
                checked = true
            }
            session = request.getSession(false)
            if (user != null) {
                // Redirect to original request
                var nuri: String? = null
                var form_auth = CustomAuthentication(authMethod, user)
                synchronized(session) {
                    nuri = session!!.getAttribute(__J_URI) as String?
                    if (nuri == null || nuri!!.isEmpty() ||
                            nuri!!.contains("/rest/login/")) {
                        nuri = "/rest/login/success"
                        if (nuri!!.isEmpty())
                            nuri = URIUtil.SLASH
                    }
                }

                response.setContentLength(0)
                val c = Cookie("JwtToken", session.getAttribute("JwtToken") as String)
                c.domain = request.serverName
                c.path = "/"
                response.addCookie(c)

                val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version)
                    HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                base_response.sendRedirect(redirectCode, response.encodeRedirectURL(nuri))
                return form_auth
            }//if login was successful will return here
            else if (checked == true){
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
                    val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version)
                        HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                    //base_response.sendRedirect(redirectCode,
                    //response.encodeRedirectURL(URIUtil.addPaths(request.contextPath, _formErrorPage)))
                    res.sendRedirect(_formErrorPage)
                }
                return Authentication.SEND_FAILURE
            }

            // Look for cached authentication
            val authentication = session.getAttribute(SessionAuthentication.__J_AUTHENTICATED) as Authentication?
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

                    if (MimeTypes.Type.FORM_ENCODED.`is`(req.getContentType())
                            && HttpMethod.POST.`is`(request.method)) {
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
                if (match != false)
                {
                   //we do not want to redirect or send the challenge
                    return Authentication.NOT_CHECKED

                }
                LOG.debug("challenge {}->{}", session.id, _formLoginPage)
                val redirectCode = if (base_request.httpVersion.version < HttpVersion.HTTP_1_1.version)
                    HttpServletResponse.SC_MOVED_TEMPORARILY else HttpServletResponse.SC_SEE_OTHER
                //base_response.sendRedirect(redirectCode,
                //        response.encodeRedirectURL(URIUtil.addPaths(request.contextPath, _formLoginPage)))
                res.sendRedirect(_formLoginPage)
            }
            return Authentication.SEND_CONTINUE
            //this ends the try block
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
    override fun secureResponse(req: ServletRequest, res: ServletResponse,
                                mandatory: Boolean, validatedUser: User?): Boolean {
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
            return if (name.toLowerCase(Locale.ENGLISH).startsWith("if-"))
                Collections.enumeration(emptyList()) else super.getHeaders(name)
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
    class CustomAuthentication(method: String, userIdentity: UserIdentity):
            UserAuthentication(method, userIdentity), Authentication.ResponseSent {

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

