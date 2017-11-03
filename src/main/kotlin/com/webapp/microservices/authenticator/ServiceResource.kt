package com.webapp.microservices.authenticator

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import sun.security.rsa.RSAPrivateCrtKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.naming.InitialContext
import javax.sql.DataSource
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.annotation.security.RolesAllowed
import javax.xml.bind.DatatypeConverter
import kotlin.collections.ArrayList


@Path("services")
class ServiceResource{

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("register")
    @RolesAllowed("admin")
    fun registerService(@QueryParam("name") name: String, key: String): Response{
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var exists = false
        var jwt: String? = null
        transaction {
            val c = Services.select {
                Services.sname.eq(name)
            }.count()
            if (c > 0) {
                exists = true
                return@transaction
            }
            val rList = ArrayList<String>()
            val pList = HashSet<String>()
            rList.add("sessionOperator")
            //does not exist yet must create an entry
            val roles = Roles.select{
                Roles.name.inList(rList)
            }

            roles.forEach{
                RolePerm.select{
                    RolePerm.roleid.eq(it[Roles.id])
                }.forEach{
                    Permissions.select{
                        Permissions.id.eq(it[RolePerm.pid])
                    }.forEach{
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
        }
        if(exists)
            return Response.ok() //what should the status be if name already exists
                .type("text/plain")
                .entity("I'm sorry this name is already taken, try again")
                .build()
        return Response.ok().type("text/plain")
                .entity("Successfully registered the application")
                .build()

    }

    //can change this later, random string generator. This results in a 16 character string
    private fun getRandom(): String {
        val random = SecureRandom()
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return encoder.encodeToString(bytes)
    }


    fun generateJWT(name: String, roles: ArrayList<String>, perms: ArrayList<String>): String {
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
    fun checkToken(@Context headers: HttpHeaders): Response{
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
    fun decrypt(body: String): Response{
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
    fun checkService(@QueryParam("tempSecret") temp: String?, @QueryParam("name") name: String): Response{
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var theString: String? = null
        var keypub: String? = null
        var jwt: String? = null
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        transaction {
            val c = Services.select {
                Services.sname.eq(name)
            }.forEach{
                theString = it[Services.secret]
                keypub = it[Services.pubKey]
                jwt = it[Services.token]
            }
        }
        temp?.let{//if temp is not null
            if(temp.equals(theString)) {
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


}