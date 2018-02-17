
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

@Path("public/gateway/service")
class ServiceAccountResource {


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    fun createService(@QueryParam("sname") name: String, body: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val key = obj.get("publickey").asText()
        var jwt: String? = null
        val node = mapper.createObjectNode()
        var count = 0
        var id:Int = -1
        var error:String? = "No Operations Executed"
        transaction {
            //by default services only have sessionOperator role when created.
            //can assign more roles using the role resource
            val results = (Roles innerJoin RolePerm innerJoin Permissions)
                    .select { (Roles.id.eq(RolePerm.roleid)) and (Roles.name.eq("sessionOperator")) and (Permissions.id.eq(RolePerm.pid)) }
            jwt = generateJWT(name, results.map{ it[Roles.name]} as ArrayList<String>,
                    results.map{ it[Permissions.operation]} as ArrayList<String> )
            val row = results.elementAt(0)
            val rid = row[Roles.id]
            try{
                id = Services.insert {
                    it[sname] = name
                    it[token] = jwt
                    it[pubKey] = key
                    it[secret] = getRandom()
                } get Services.id
            }catch (e: org.postgresql.util.PSQLException)
            {
                error = e.message
                return@transaction
            }

            //create new ServiceRole entry
                try{
                    ServiceRole.insert {
                        it[sid] = id
                        it[roleid] = rid
                    }
                    count += 1
                } catch( e: org.postgresql.util.PSQLException)
                {
                    error = e.message
                }


        }
        if(count == 0 || id == -1)
        {
            node.put("Error", error)
        }
        node.put("Create Service Count", count)
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
    fun checkService(@QueryParam("tempSecret") temp: String?, @QueryParam("sname") name: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        var theString: String? = null
        var keypub: String? = null
        var jwt: String? = null
        val mapper = ObjectMapper()
        val root = mapper.createObjectNode()
        transaction {
            Services.select {
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


    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    fun deleteService(@QueryParam("sname") sName: String): Response {
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        transaction {
            val ret = Services.deleteWhere {
                Services.sname.eq(sName)
            }
            if(ret == 0){
                node.put("Error", "No Operations Executed")
            }
            node.put("Delete Service Count", ret)
        }

        return Response.ok().entity(node).build()
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateService(@QueryParam("sname") sName: String, body: String): Response {
        //body holds the update password
        val mapper = ObjectMapper()
        val obj = mapper.readTree(body)
        val key = obj.get("publickey").asText()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        val node = mapper.createObjectNode()
        transaction {
            val res= Services.update({Services.sname eq sName}) {
                it[pubKey] = key
            }

            if (res == 0) {
                node.put("Error", "No Operations Executed")
                node.put("Update Service Count", 0)
                return@transaction
            }

            node.put("Update Service Count", 1)
            //maybe add update roles in the future
        }
        return Response.ok().entity(node).build()
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun readServices(@QueryParam("name") sName: String?): Response {
        val mapper = ObjectMapper()
        val node = mapper.createObjectNode()
        Database.connect(InitialContext().lookup("java:comp/env/jdbc/userStore") as DataSource)
        transaction {
            if (sName != null) {
                val res = Services.select {
                    Services.sname.eq(sName)
                }
                if(res.count() == 0)
                {
                    node.put("Error", "No Operations Executed")
                    node.put("Read Service Count", 0)
                    return@transaction
                }
                val objNode = mapper.createObjectNode()
                val results = (Roles innerJoin ServiceRole innerJoin Services)
                        .select { (Services.sname.eq(sName)) and (Services.id.eq(ServiceRole.sid)  and (Roles.id.eq(ServiceRole.roleid))) }
                        .map { it[Roles.name] }
                val roleNode: ArrayNode = mapper.valueToTree(results)
                node.set(sName, objNode.set("roles", roleNode))
                node.put("Read Service Count", 1)
            } else {
                //print all of them?
                var count = 0
                for (service in Services.selectAll()) {
                    val objNode: ObjectNode = mapper.createObjectNode()
                    val results = (Roles innerJoin ServiceRole innerJoin Services)
                            .select { (Services.sname.eq(service[Services.sname])) and (Services.id.eq(ServiceRole.sid) and (Roles.id.eq(ServiceRole.roleid))) }
                            .map { it[Roles.name] }
                    val roleNode: ArrayNode = mapper.valueToTree(results)
                    node.set(service[Services.sname], objNode.set("roles", roleNode))
                    count += 1
                }
                node.put("Read Service Count", count)
            }
        }

        return Response.ok().entity(node).build()
    }
}



