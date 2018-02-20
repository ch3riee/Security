import com.mashape.unirest.http.Unirest
import org.json.JSONObject
import org.json.JSONArray
import org.junit.BeforeClass
import org.junit.FixMethodOrder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runners.MethodSorters
import org.junit.Test

//Tests are run in alphabetical order!!"
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class MyTests {

    companion object{
        init{

        }
        lateinit var jwtToken: String
        lateinit var jsessionId: String


        @BeforeClass @JvmStatic fun setUp(){
            val tokenResponse = Unirest.post("http://127.0.0.1:8080/rest/callback/gateway/localcallback")
                    .field("j_username", "admin@gmail.com")
                    .field("j_password", "j")
                    .asJson()
            val cookies = tokenResponse.headers.getFirst("Set-Cookie")

            jsessionId = cookies.substringBefore(";").removePrefix("JSESSIONID=")
            jwtToken = cookies.substringAfter("Path=/").removePrefix("JwtToken=")

        }
    }

    @Test
    fun testCreateService(){
        val str = "fake public key to be updated over"
        val jsonResponse = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("sname", "testservice")
                .body("{\"publickey\":\"" + str + "\"}") //will be updated to real public key later
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Create Service Count"))
        val jsonResponse2 = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("sname", "testservice")
                .body("{\"publickey\":\"fakepublickey\"}")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Create Service Count"))
    }

    @Test
    fun testCreateUser(){
        val jsonResponse = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .body("{\"username\":\"test@gmail.com\", \"password\":\"j\"}")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Create User Count"))
        val jsonResponse2 = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .body("{\"username\":\"test@gmail.com\", \"password\":\"j\"}")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Create User Count"))
    }

    @Test
    fun testLogin(){
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/session/login")
                .asString()
        val str = jsonResponse.body
        val p = """(?<=name=\"testid\" content=\")[^\"]*(?=\")""".toRegex().find(str)?.groupValues?.get(0)
        assertEquals("5629ba47d624b2d2688c0a0340b29344", p)

    }


    @Test
    fun testRoleCreate() {
        val jsonResponse = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .body("{\"rname\":\"testrole\"}")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Create Role Count"))
        val jsonResponse2 = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .body("{\"rname\":\"testrole\"}")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        //if not logged in will have expected array error
        assertEquals(0, obj2.getInt("Create Role Count"))
        assertNotNull(obj2.getString("Error"))
    }

    @Test
    fun testRolePermUpdate(){
        val jsonResponse = Unirest.put("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("rname", "testrole")
                .body("[\"guest\", \"session:read\", \"user:modify\"]")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        //guest is ignored so not 3
        assertEquals(2, obj.getInt("Update Role Count"))
    }

    @Test
    fun testRoleServiceAssign(){
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "testservice")
                .queryString("type", "service")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Assign Role Count"))
        val jsonResponse2 = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "testservice")
                .queryString("type", "service")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Assign Role Count"))
    }



    @Test
    fun testRoleUserAssign(){
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "test@gmail.com")
                .queryString("type", "user")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Assign Role Count"))
        val jsonResponse2 = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "test@gmail.com")
                .queryString("type", "user")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Assign Role Count"))

    }

    @Test
    fun testRoleServiceDelete() {
        val jsonResponse = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "testservice")
                .queryString("type", "service")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Delete Role Count"))
        val jsonResponse2 = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "testservice")
                .queryString("type", "service")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Delete Role Count"))
    }

    @Test
    fun testRoleUserDelete() {
        val jsonResponse = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "test@gmail.com")
                .queryString("type", "user")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Delete Role Count"))
        val jsonResponse2 = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .queryString("name", "test@gmail.com")
                .queryString("type", "user")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Delete Role Count"))
    }

    @Test
            //need this to be the last role delete test run hence the x
    fun testRoleXDelete() {
        val jsonResponse = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Delete Role Count"))
        val jsonResponse2 = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/role")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "testrole")
                .asJson()
        val obj2:JSONObject = jsonResponse2.body.`object`
        //if not logged in will have expected array error
        assertEquals(0, obj2.getInt("Delete Role Count"))
        assertNotNull(obj2.getString("Error"))
    }

    //now testing service api, service testservice exists at this point
    //Create service is already tested at the beginning
    @Test
    fun testServiceRead(){
        //read all services should only be one
        val oneResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .asJson()
        val oneArr = oneResponse.body.`object`.get("testservice") as JSONObject
        assertEquals("sessionOperator", oneArr.getJSONArray("roles").get(0))
        Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "guest")
                .queryString("name", "testservice")
                .queryString("type", "service")
                .asJson()
        //second read specific service account information
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`.get("testservice") as JSONObject
        val arr = JSONArray(arrayOf("sessionOperator", "guest"))
        val retobj = obj.getJSONArray("roles")
        assertTrue(arr.similar(retobj))
    }

    @Test
    fun testServiceUpdate(){
        val str = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAscGE+UA46dCkx" +
                "TNwoWDichQCEjddGbN8Jl07eOJYdRpM+AhAZkANBG1JzUZV8A77Wli5qiq6" +
                "tUpEHjzHkFixMl89hdYmpj/xnkyJBHuCbUewq8damAn30veJsRjbliRmpG0" +
                "yDMb+fpMBFtzoHhNne+Xkz96oBF1HLKnD+4JuWzvuaxzfNS8P9QgmUD3M+X" +
                "ETv0PQ5b7JOzPSYHah7HFxQeG6BtqQG7POajlSyQuTP4Un48hsmJuhqDHnP" +
                "NFFTADiibjczcAWnwtFkGZa4x5VRLj8xJA7c8fnK0B5iCOfcGijnThqmarC" +
                "Ci/SkohKv6eTFt+tFO1pfnXGgOQgH6cpKwIDAQAB"
        val jsonResponse = Unirest.put("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("sname", "testservice")
                .body("{\"publickey\":\"" + str + "\"}")
                .asJson()
        val obj = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Update Service Count"))
        //bottom one should fail
        val jsonResponse2 = Unirest.put("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("sname", "fakeservice")
                .body("{\"publickey\":\"New Fake Public Key\"}")
                .asJson()
        val obj2 = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Update Service Count"))
        assertEquals("No Operations Executed", obj2.getString("Error"))
    }

    //testing internal session dao service
    @Test
    fun testSessionASet(){
        val tempResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .asJson()
        val temp = tempResponse.body.`object`
        val tempResponse2 = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/service/decryptSecret")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .body(temp)
                .asString()
        val tempSecret = tempResponse2.body
        val nodeid = jsessionId.substringBefore(".")
        val tokenResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .queryString("tempSecret", tempSecret)
                .asJson()
        val token = tokenResponse.body.`object`.getString("BearerToken")
        val jsonResponse = Unirest.post("http://127.0.0.1:8080/rest/internal/gateway/session")
                .header("Authorization", "bearer "  + token)
                .header("Content-Type", "application/json")
                .queryString("key", "a.b.c")
                .queryString("id", nodeid) //some valid session id
                .body("{\"hello\":\"how are you\", \"array\":[\"first\", \"second\"]}")
                .asJson()
        val obj = jsonResponse.body.`object`.get("b") as JSONObject
        val c =obj.get("c") as JSONObject
        assertEquals("how are you", c.get("hello"))
    }

    @Test
    fun testSessionGet(){
        val tempResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .asJson()
        val temp = tempResponse.body.`object`
        val tempResponse2 = Unirest.post("http://127.0.0.1:8080/rest/public/gateway/service/decryptSecret")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .body(temp)
                .asString()
        val tempSecret = tempResponse2.body
        val tokenResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .queryString("tempSecret", tempSecret)
                .asJson()
        val token = tokenResponse.body.`object`.getString("BearerToken")
        val nodeid = jsessionId.substringBefore(".")
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/internal/gateway/session")
                .header("Authorization", "bearer "  + token)
                .queryString("key", "a.b.c")
                .queryString("id", nodeid) //some valid session id
                .asJson()
        val arr = jsonResponse.body.`object`.getJSONArray("array")
        assertEquals("first", arr[0])
        assertEquals("second", arr[1])
    }



    @Test
    fun testUserRead(){
        //only two users: admin@gmail.com and test@gmail.com
        //read all users
        val twoResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .asJson()
        val emptyArr = twoResponse.body.`object`.get("test@gmail.com") as JSONObject
        assertTrue(emptyArr.getJSONArray("roles").length() == 0)
        Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/assign")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("rname", "guest")
                .queryString("name", "test@gmail.com")
                .queryString("type", "user")
                .asJson()
        //second read specific user account information
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("uname", "test@gmail.com")
                .asJson()
        val obj:JSONObject = jsonResponse.body.`object`.get("test@gmail.com") as JSONObject
        val arr = JSONArray(arrayOf("guest"))
        val retobj = obj.getJSONArray("roles")
        assertTrue(arr.similar(retobj))
    }

    @Test
    fun testUserUpdate(){
        val jsonResponse = Unirest.put("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("uname", "test@gmail.com")
                .body("{\"password\":\"New Fake Password\"}")
                .asJson()
        val obj = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Update User Count"))
        //bottom one should fail
        val jsonResponse2 = Unirest.put("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .header("Content-Type", "application/json")
                .queryString("uname", "fakeuser")
                .body("{\"password\":\"New Fake Password\"}")
                .asJson()
        val obj2 = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Update User Count"))
        assertEquals("No Operations Executed", obj2.getString("Error"))
    }

    //need this to be the last method for checking service api
    @Test
    fun testXServiceDelete(){
        val jsonResponse = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "testservice")
                .asJson()
        val obj = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Delete Service Count"))
        val jsonResponse2 = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/service")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("sname", "fakeservice")
                .asJson()
        val obj2 = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Delete Service Count"))
        assertEquals("No Operations Executed", obj2.getString("Error"))
    }

    @Test
    fun testUserXDelete(){
        val jsonResponse = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("uname", "test@gmail.com")
                .asJson()
        val obj = jsonResponse.body.`object`
        assertEquals(1, obj.getInt("Delete User Count"))
        val jsonResponse2 = Unirest.delete("http://127.0.0.1:8080/rest/public/gateway/user")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("uname", "test@gmail.com")
                .asJson()
        val obj2 = jsonResponse2.body.`object`
        assertEquals(0, obj2.getInt("Delete User Count"))
        assertEquals("No Operations Executed", obj2.getString("Error"))
    }

    //must be last or all other tests will fail
    @Test
    fun testXXLogout(){
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/session/logout")
                .asString()
        val str = jsonResponse.body
        val p = """(?<=name=\"testid\" content=\")[^\"]*(?=\")""".toRegex().find(str)?.groupValues?.get(0)
        assertEquals("5629ba47d624b2d2688c0a0340b29344", p)
    }
}
