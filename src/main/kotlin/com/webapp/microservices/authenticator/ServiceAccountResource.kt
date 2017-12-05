
package com.webapp.microservices.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import sun.security.rsa.RSAPrivateCrtKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.SecureRandom
import java.util.*
import javax.annotation.security.RolesAllowed
import javax.crypto.Cipher
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.xml.bind.DatatypeConverter

@Path("service")
class ServiceAccountResource {


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("create")
    @RolesAllowed("admin")
    fun createService(@QueryParam("name") name: String, body: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val key = obj.get("publickey").asText()
        var exists = false
        var jwt: String? = null
        val node = mapper.createObjectNode()
        transaction {
            val c = Services.select {
                Services.sname.eq(name)
            }.count()
            if (c > 0) {
                node.put("Create Service Count", "0, Service already exists")
                return@transaction
            }
            val rList = ArrayList<String>()
            val pList = HashSet<String>()
            rList.add("sessionOperator")
            //does not exist yet must create an entry
            val roles = Roles.select {
                Roles.name.inList(rList)
            }

            roles.forEach {
                RolePerm.select {
                    RolePerm.roleid.eq(it[Roles.id])
                }.forEach {
                    Permissions.select {
                        Permissions.id.eq(it[RolePerm.pid])
                    }.forEach {
                        pList.add(it[Permissions.operation])
                    }
                }
            }

            jwt = generateJWT(name, rList, ArrayList<String>(pList))
            val id = Services.insert {
                it[sname] = name
                it[token] = jwt
                it[pubKey] = key
                it[secret] = getRandom()
            } get Services.id

            //create new ServiceRole entry
            roles.forEach {
                val sessionId = it[Roles.id]
                ServiceRole.insert {
                    it[sid] = id
                    it[roleid] = sessionId
                }
            }
            node.put("Create Service Count", 1)
        }

        return Response.ok().entity(node).build()

    }

    //can change this later, random string generator. This results in a 16 character string
    private fun getRandom(): String {
        val random = SecureRandom()
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return encoder.encodeToString(bytes)
    }


    private fun generateJWT(name: String, roles: ArrayList<String>, perms: ArrayList<String>): String {
        var privateKey = (this::class.java.classLoader).getResource("pki/Private.key")
                .readText()
                .toByteArray()
        privateKey = Base64.getDecoder().decode(privateKey)
        val myMap = HashMap<String, Any>()
        myMap.put("Roles", roles.toTypedArray())
        myMap.put("Permissions", perms.toTypedArray())
        myMap.put("TokenType", "service")
        val jwt = Jwts.builder()
                .setClaims(myMap)
                .setSubject(name)
                .signWith(SignatureAlgorithm.RS512, RSAPrivateCrtKeyImpl.newKey(privateKey))
                .compact()
        return jwt
    }

    @GET
    @Path("checkServiceToken")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkToken(@Context headers: HttpHeaders): Response {
        val temp = headers.getRequestHeader("authorization").get(0) as String
        val jwt = temp.substring(temp.indexOf(' ') + 1) //trims out bearer
        var publicKey = (this::class.java.classLoader).getResource("pki/Public.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        val res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(jwt).body
        return Response.status(Response.Status.OK).entity(res).build()
    }


    //PURELY FOR TESTING PURPOSES: to decrypt the temp string
    @POST
    @Path("decryptSecret")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    fun decrypt(body: String): Response {
        val mapper = ObjectMapper()
        val root = mapper.readTree(body)
        var privateKey = (this::class.java.classLoader).getResource("pki/sample/Private.key")
                .readText()
                .toByteArray()
        privateKey = Base64.getDecoder().decode(privateKey)
        val cipher2 = Cipher.getInstance("RSA")
        cipher2.init(Cipher.PRIVATE_KEY, RSAPrivateCrtKeyImpl.newKey(privateKey))
        val ret = cipher2.doFinal(DatatypeConverter.parseBase64Binary(root.get("tempSecret").textValue()))
        return Response.ok().entity(String(ret)).build()
    }

    @GET
    @Path("getServiceToken")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkService(@QueryParam("tempSecret") temp: String?, @QueryParam("name") name: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var theString: String? = null
        var keypub: String? = null
        var jwt: String? = null
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        transaction {
            val c = Services.select {
                Services.sname.eq(name)
            }.forEach {
                theString = it[Services.secret]
                keypub = it[Services.pubKey]
                jwt = it[Services.token]
            }
        }
        temp?.let {
            //if temp is not null
            if (temp.equals(theString)) {
                root.put("BearerToken", jwt)
                return Response.ok().entity(root).build() //what if it does not work? empty json?
            }
            return Response.status(403).build()

        }
        //grab the temporary string in service account db and encrypt it. Then return it to them
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.PUBLIC_KEY, RSAPublicKeyImpl(Base64.getDecoder().decode(keypub)))
        val s = DatatypeConverter.printBase64Binary(cipher.doFinal(theString?.toByteArray()))
        root.put("tempSecret", s)
        return Response.ok().entity(root).build()
    }

    @GET
    @Path("delete")
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteService(@QueryParam("name") sName: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        transaction {
            val c = Services.select {
                Services.sname.eq(sName)
            }.count()
            if (c == 0) {
                node.put("Delete Service Count", "0, service name does not exist")
                return@transaction
            }
            Services.deleteWhere {
                Services.sname.eq(sName)
            }
            node.put("Delete Service Count", 1)
        }

        return Response.ok().entity(node).build()
    }

    @POST
    @Path("update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateService(@QueryParam("name") sName: String, body: String): Response {
        //body holds the update password
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val key = obj.get("publickey").asText()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val node = mapper.createObjectNode()
        transaction {
            //update public key for service account
            val res = Services.select {
                Services.sname.eq(sName)
            }
            if (res.count() == 0) {
                node.put("Update Service Count", "0, Service does not exist")
                return@transaction
            }

            res.forEach {
                it[Services.pubKey] = key
            }
            node.put("Update User Count", 1)
            //maybe add update roles in the future
        }

        return Response.ok().entity(node).build()
    }

    @GET
    @Path("read")
    @Produces(MediaType.APPLICATION_JSON)
    fun readServices(@QueryParam("name") sName: String?): Response {
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {
            if (sName != null) {
                val objNode = mapper.createObjectNode()
                var uid = 0
                val rList = ArrayList<Int>()
                val roles = ArrayList<String>()
                //read this one service
                val res = Services.select {
                    Services.sname.eq(sName)
                }
                if(res.count() == 0)
                {
                    node.put("Read Count", "0, No such service")
                    return@transaction
                }
                res.forEach {
                    //grab service info + role info
                    uid = it[Services.id]
                    node.set(sName, objNode)
                }

                ServiceRole.select {
                    ServiceRole.sid.eq(uid)
                }.forEach {
                    rList.add(it[ServiceRole.roleid])
                }
                rList.forEach({ e: Int ->
                    Roles.select {
                        Roles.id.eq(e)
                    }.forEach {
                        roles.add(it[Roles.name])
                    }
                })
                val roleNode: ArrayNode = mapper.valueToTree(roles)
                objNode.set("roles", roleNode)
            } else {
                //print all of them?
                for (service in Services.selectAll()) {
                    val rList = ArrayList<Int>()
                    val roles = ArrayList<String>()
                    ServiceRole.select {
                        ServiceRole.sid.eq(service[Services.id])
                    }.forEach {
                        rList.add(it[ServiceRole.roleid])
                    }
                    rList.forEach({ e: Int ->
                        Roles.select {
                            Roles.id.eq(e)
                        }.forEach {
                            roles.add(it[Roles.name])
                        }
                    })
                    val roleNode: ArrayNode = mapper.valueToTree(roles)
                    val objNode: ObjectNode = mapper.createObjectNode()
                    node.set(service[Services.sname], objNode.set("roles", roleNode))

                }
            }
        }
        return Response.ok().entity(node).build()
    }
}




