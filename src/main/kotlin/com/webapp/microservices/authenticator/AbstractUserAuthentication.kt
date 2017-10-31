package com.webapp.microservices.authenticator

import org.eclipse.jetty.security.ConstraintAware
import org.eclipse.jetty.security.SecurityHandler
import java.io.Serializable

import org.eclipse.jetty.server.Authentication.User
import org.eclipse.jetty.server.UserIdentity
import org.eclipse.jetty.server.UserIdentity.Scope

/**
 * AbstractUserAuthentication
 *
 *
 * Base class for representing an authenticated user.
 */
abstract class AbstractUserAuthentication(protected var _method: String,  protected var _userIdentity: UserIdentity) : User, Serializable {


    override fun getAuthMethod(): String {
        return _method
    }

    override fun getUserIdentity(): UserIdentity {
        return _userIdentity
    }

    override fun isUserInRole(scope: Scope?, role: String): Boolean {
        var roleToTest: String? = null
        if (scope != null && scope.roleRefMap != null)
            roleToTest = scope.roleRefMap[role]
        if (roleToTest == null)
            roleToTest = role
        //Servlet Spec 3.1 pg 125 if testing special role **
        return if ("**" == roleToTest.trim { it <= ' ' }) {
            //if ** is NOT a declared role name, the we return true
            //as the user is authenticated. If ** HAS been declared as a
            //role name, then we have to check if the user has that role
            if (!declaredRolesContains("**"))
                true
            else
                _userIdentity.isUserInRole(role, scope)
        } else _userIdentity.isUserInRole(role, scope)

    }

    fun declaredRolesContains(roleName: String): Boolean {
        val security = SecurityHandler.getCurrentSecurityHandler() ?: return false

        if (security is ConstraintAware) {
            val declaredRoles = (security as ConstraintAware).roles
            return declaredRoles != null && declaredRoles.contains(roleName)
        }

        return false
    }

    companion object {
        private const val serialVersionUID = -6290411814232723403L
    }
}
