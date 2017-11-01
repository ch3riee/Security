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
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


@Path("services")
class ServiceResource{

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("register")
    @RolesAllowed("superadmin")
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
            val myList = ArrayList<String>()
            myList.add("service")
            //does not exist yet must create an entry
             jwt = generateJWT(name, myList)
            val id = Services.insert {
                it[sname] = name
                it[token] = jwt
                it[pubKey] = key
                it[secret] = getRandom()
            } get Services.id

            //create a new row in UserRole table
            //find the role id for admin

            Roles.select {
                Roles.name.inList(myList)
            }.forEach {
                val adminId = it[Roles.id]
                ServiceRole.insert {
                    it[sid] = id
                    it[roleid] = adminId
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

    private fun getRandom(): String {
        val random = SecureRandom()
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        val encoder = Base64.getUrlEncoder().withoutPadding()
        return encoder.encodeToString(bytes)
    }


    fun generateJWT(name: String, roles: ArrayList<String>): String {
        var privateKey = (this::class.java.classLoader).getResource("pki/Private.key")
                .readText()
                .toByteArray()
        privateKey = Base64.getDecoder().decode(privateKey)
        val myMap = HashMap<String, Any>()
        myMap.put("Roles", roles.toTypedArray())
        //myMap.put("Permissions", perms.toTypedArray())
        val type = "service"
        myMap.put("TokenType", type)
        val jwt = Jwts.builder()
                .setClaims(myMap)
                .setSubject(name)
                .signWith(SignatureAlgorithm.RS512, RSAPrivateCrtKeyImpl.newKey(privateKey))
                .compact()
        return jwt
    }

    @GET
    @Path("checkServiceToken")
    @RolesAllowed("superadmin")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkToken(@Context headers: HttpHeaders): Response{
        //assumes that the microservice has already decrypted the public key encryption on the jwt
        //otherwise means that the microservice may not be valid!!
        val jwt = headers.getRequestHeader("authorization").get(0) as String
        println("Before actually check jwt: " + jwt)
        var publicKey = (this::class.java.classLoader).getResource("pki/Public.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        val res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(jwt).body
        return Response.status(Response.Status.OK).entity(res).build()
    }

    @POST
    @Path("decryptSecret")
    @RolesAllowed("superadmin")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    fun decrypt(body: String): Response{
        val mapper = ObjectMapper()
        val root = mapper.readTree(body)
        val s = root.get("tempSecret").asText()

        var privateKey = (this::class.java.classLoader).getResource("pki/sample/Private.key")
                .readText()
                .toByteArray()
        privateKey = Base64.getDecoder().decode(privateKey)
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, RSAPrivateCrtKeyImpl.newKey(privateKey))
        println(s.toByteArray().size)
        val ret = cipher.doFinal(s.toByteArray())
        return Response.ok().type("text/plain").entity(ret).build()

    }

    @GET
    @Path("getServiceToken")
    @RolesAllowed("superadmin") //testing for microservices
    @Produces(MediaType.APPLICATION_JSON)
    fun checkService(@QueryParam("tempSecret") temp: String?, @QueryParam("name") name: String): Response{
        //need to first grab the encrypted sym as well as the encrypted token
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
        if(temp == null){
            //grab the temporary string in service account db and encrypt it. Then return it to them
            val pkey = Base64.getDecoder().decode(keypub)
            val key  = RSAPublicKeyImpl(pkey)
            val cipher = Cipher.getInstance("RSA")
            cipher.init(Cipher.PUBLIC_KEY, key)
            println(theString?.toByteArray()?.size)
            println(theString)
            val tempString = cipher.doFinal(theString?.toByteArray())

            root.put("tempSecret", tempString)
            return Response.ok().entity(root).build() //they need to decrypt it with private key
        }
        //temp has supposedly been decrypted and returned
        if(temp.equals(theString)) root.put("BearerToken", jwt)
        return Response.ok().entity(root).build() //what if it does not work?

    }





}