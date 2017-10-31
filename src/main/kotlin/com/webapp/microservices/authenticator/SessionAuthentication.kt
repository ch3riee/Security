package com.webapp.microservices.authenticator

import java.io.Serializable

import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionActivationListener
import javax.servlet.http.HttpSessionBindingEvent
import javax.servlet.http.HttpSessionBindingListener
import javax.servlet.http.HttpSessionEvent

import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.server.session.Session
import org.eclipse.jetty.util.log.Log

class SessionAuthentication(method: String, userIdentity: CustomUserIdentity, private val _credentials: Any) : AbstractUserAuthentication(method, userIdentity), Serializable, HttpSessionActivationListener, HttpSessionBindingListener {

    private val _name: String
    @Transient private var _session: HttpSession? = null

    init {
        _name = userIdentity.userPrincipal.name
    }


   /* @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        stream.defaultReadObject()

        val security = SecurityHandler.getCurrentSecurityHandler() ?: throw IllegalStateException("!SecurityHandler")
        val login_service = security.loginService ?: throw IllegalStateException("!LoginService")

        _userIdentity = login_service.login(_name, _credentials, null)
        LOG.debug("Deserialized and relogged in {}", this)
    }*/

    override fun logout() {
        if (_session != null && _session!!.getAttribute(__J_AUTHENTICATED) != null)
            _session!!.removeAttribute(__J_AUTHENTICATED)

        doLogout()
    }

    private fun doLogout() {
        val security = SecurityHandler.getCurrentSecurityHandler()
        security?.logout(this)
        if (_session != null)
            _session!!.removeAttribute(Session.SESSION_CREATED_SECURE)
    }

    override fun toString(): String {
        return String.format("%s@%x{%s,%s}", this.javaClass.simpleName, hashCode(), if (_session == null) "-" else _session!!.id, _userIdentity)
    }

    override fun sessionWillPassivate(se: HttpSessionEvent) {

    }

    override fun sessionDidActivate(se: HttpSessionEvent) {
        if (_session == null) {
            _session = se.session
        }
    }

    override fun valueBound(event: HttpSessionBindingEvent) {
        if (_session == null) {
            _session = event.session
        }
    }

    override fun valueUnbound(event: HttpSessionBindingEvent) {
        doLogout()
    }


    companion object {
        private val LOG = Log.getLogger(SessionAuthentication::class.java)

        private const val serialVersionUID = -4643200685888258706L


        val __J_AUTHENTICATED = "org.eclipse.jetty.security.UserIdentity"
    }

}
