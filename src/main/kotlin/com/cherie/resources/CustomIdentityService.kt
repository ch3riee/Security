package com.cherie.resources

import org.eclipse.jetty.security.IdentityService
import org.eclipse.jetty.security.RoleRunAsToken
import org.eclipse.jetty.security.RunAsToken
import org.eclipse.jetty.server.UserIdentity
import java.security.Principal
import javax.security.auth.Subject


/* ------------------------------------------------------------ */
/**
 * Default Identity Service implementation.
 * This service handles only role reference maps passed in an
 * associated [org.eclipse.jetty.server.UserIdentity.Scope].  If there are roles
 * refs present, then associate will wrap the UserIdentity with one
 * that uses the role references in the
 * [org.eclipse.jetty.server.UserIdentity.isUserInRole]
 * implementation. All other operations are effectively noops.
 *
 */
/* ------------------------------------------------------------ */
class CustomIdentityService : IdentityService {

    /* ------------------------------------------------------------ */
    /**
     * If there are roles refs present in the scope, then wrap the UserIdentity
     * with one that uses the role references in the [UserIdentity.isUserInRole]
     */
    override fun associate(user: UserIdentity?): Any? {
        return null
    }

    /* ------------------------------------------------------------ */
    override fun disassociate(previous: Any?) {}

    /* ------------------------------------------------------------ */
    override fun setRunAs(user: UserIdentity?, token: RunAsToken?): Any? {
        return token
    }

    /* ------------------------------------------------------------ */
    override fun unsetRunAs(lastToken: Any?) {}

    /* ------------------------------------------------------------ */
    override fun newRunAsToken(runAsName: String): RunAsToken? {
        return RoleRunAsToken(runAsName)
    }

    /* ------------------------------------------------------------ */
    override fun getSystemUserIdentity(): UserIdentity? {
        return null
    }

    /* ------------------------------------------------------------ */
    override fun newUserIdentity(subject: Subject, userPrincipal: Principal, roles: Array<String>): UserIdentity {
        return CustomUserIdentity(subject, userPrincipal, roles)
    }

}
