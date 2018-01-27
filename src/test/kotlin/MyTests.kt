import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.ObjectMapper
import com.mashape.unirest.http.Unirest
import org.json.JSONObject
import org.junit.BeforeClass
import kotlin.test.assertEquals
import org.junit.Test
import kotlin.test.fail

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
    fun testRoleCreateSuccess() {
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/create")
                .header("JSESSIONID", jsessionId)
                .header("JwtToken", jwtToken)
                .queryString("name", "testrole")
                .asString()
       // val obj:JSONObject = jsonResponse.body.`object`

       // val obj = mapper.readValue(jsonResponse.rawBody, Map::class.java)
        //assertEquals(1, obj.getInt("Create Role Count"))
        fail(jsonResponse.body)
    }

    @Test
    fun testRoleCreateConstraint() {
        val jsonResponse = Unirest.get("http://127.0.0.1:8080/rest/public/gateway/role/create")
                .queryString("name", "testrole")
                .header("authorization", "bearer " + "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJ0ZXN0IiwiVG9rZW5UeX" +
                        "BlIjoic2VydmljZSIsIlBlcm1pc3Npb25zIjpbInNlc3Npb246bW9kaWZ5Iiwic2Vzc2lvbjpyZWFkIl0sI" +
                        "lJvbGVzIjpbInNlc3Npb25PcGVyYXRvciJdfQ.mRjvZxYYZnqzVcevHBIvOkz5izN6Y-SdCW7Bsn_67hQs" +
                        "lpD6T3x2GGarcGO0ZPHLAVSE6CeFS4WfCVLW2H5uuq3GMKkiwzuFqaXvMUDMdaDeq797DfaTIVMTEUTq7H9" +
                        "xWKBXSK2Ft9FXbfAvL4HGy0iCSeynPHBUoGe3zcigjxVMX6WptBAlB7wLJuonMbgTo6ALy3odFd1ueU9Z" +
                        "_1qhG429c1wJsLY8frTNKwXNDkzOcHsW11bnJiZlqD4JrnJWT4YWIq10WZgHk4NXVxO3rO-Ew9HCygFF9" +
                        "nvwsy1C7d4rR6V9roj1NJjQDTtRwA-MECx1l_S7u0kd7j1gWjV1tQ")
                .asJson()
        val obj = jsonResponse.body.getObject()
        assertEquals(0, obj.get("Create Role Count"))
        assert(obj.has("Error"))

    }
}
