package com.webapp.microservices.authenticator

import java.security.Principal

import javax.security.auth.Subject

import org.eclipse.jetty.server.UserIdentity
import java.io.Serializable


/* ------------------------------------------------------------ */
/**
 * The default implementation of UserIdentity.
 *
 */
class CustomUserIdentity(private val _subject: Subject, private val _userPrincipal: Principal, private val _roles: Array<String>) : UserIdentity, Serializable {

    private  val serialVersionUID = 1L
    override fun getSubject(): Subject {
        return _subject
    }

    override fun getUserPrincipal(): Principal {
        return _userPrincipal
    }

    override fun isUserInRole(role: String, scope: UserIdentity.Scope?): Boolean {
        //Servlet Spec 3.1, pg 125
        if ("*" == role)
            return false

        var roleToTest: String? = null
        if (scope != null && scope.roleRefMap != null)
            roleToTest = scope.roleRefMap[role]

        //Servlet Spec 3.1, pg 125
        if (roleToTest == null)
            roleToTest = role

        for (r in _roles)
            if (r == roleToTest)
                return true
        return false
    }

    override fun toString(): String {
        return CustomUserIdentity::class.java.simpleName + "('" + _userPrincipal + "')"
    }
}
