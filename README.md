
# Nginx Gateway and Jetty-based Session/User Store
An api gateway for microservices that utilizes Nginx, Jetty, and Jersey. Provides a session store, user store, along with 
authorization and authentication features. Contained within Docker Containers.
## Request Diagram
This diagram shows the callstack for handling an Http Request. </br>
![Alt text](diagram.png?raw=true "Request Class Path Diagram")
## SETUP
### 1. Download this github repo
```
git clone https://github.com/ch3riee/Security.git .
```
### 2. Inside of the project folder, run command for Docker-Compose to start up the gateway/server
```
docker-compose up
```
### 3. Example Rest Endpoint 
To check if working, go to http://127.0.0.1:8080/rest/hello </br>
This should bounce you automatically to the login page at: http://127.0.0.1:8080/rest/login </br>

## Deploying Your Third Party Microservice and Using  Shared SessionStore

### 1. Add Microservice to Docker-Compose
Please read the following documentation at https://docs.docker.com/compose/ to learn about docker-compose. </br>
In order to add your microservice to the api gateway, a separate container for your microservice must be added to the
docker-compose file located in the downloaded project folder. Please use the existing containers as examples if needed. </br>
#### Public Key Volume Mount Requirement
For every microservice Docker container added to docker-compose, you must mount the shared public key folder as a volume. 
This volume can be copied from the volume in docker container srv-java-users. It allows the microservice to have access to the public key, in order to decrypt JWT token generated by this service. </br>
```
volumes:
  - ./src/main/resources/pki/Public.key:/var/lib/pki-public
```
#### Depends-On Requirement
Please add your container name to the depends_on section of the web container (nginx container). 

### 2. Adding Microservices to NGINX site.conf.template
Within the project folder, locate **./settings/nginx/conf.d/site.conf.template**This file holds the NGINX configuration that is copied into default.conf by the NGINX container at command docker-compose up. Each microservice must be added to this configuration in order for them to be deployed along with the existing API gateway. </br>
#### Section: WEB
This commented section in the NGINX configuration is where any web applications (microservices) meant for web users is placed. There is already a sample web application within this section. Any microservice added to this section must have the 
```
#Web Section
if ($http_cookie !~ "JwtToken=")
{
  return 301 $scheme://$host:8080/rest/login ;
}
```
check in order to authenticate/authorize users that will be accessing these endpoints. If this JwtToken does not exist in the
user's request header, NGINX should proxy redirect to the login endpoint.
#### Section: INTERNAL
This commented section in the NGINX configuration is where any APIS for microservices should be placed. For example currently the Session get/set api for microservices is housed in this section. All endpoints in this section should check to see if an authorization header exists. The actual bearer token will be checked/validated within any of the APIs themselves. If the bearer token header does not exist, return 403 Forbidden Status.
```
#Internal Section
if($Http_authorization= ""){
   return 403;
}
```
#### Section: STATIC
  This commented section in the NGINX configuration is where static content location blocks should be placed. For more information regarding configuring static content, please go to </br>
https://www.nginx.com/resources/admin-guide/serving-static-content/ </br>
  When serving static content, a root or alias path must be set in the location block. If you would like to use the current static folder to serve your content, without altering any docker-compose volume mounts, please place all static content within the /resources/static folder in the project folder. This will be mapped to /var/lib/static/ within the NGINX docker container. </br>
  **NOTE**: If static content is placed somewhere else, please set this as a volume mount within the NGINX docker container in docker-compose file. The path within the docker container itself is the one that should be passed as the alias or root. </br>
EXAMPLE: Looks for /var/lib/static/index.html file and serves the content for context root -> http://127.0.0.1:8080/
```
#Static Section
location / {
    alias /var/lib/static/ ;
    index index.html ;
}
```
### 3. Get Service Account Token
**NOTE**: Please make sure your microservice has already been created by an admin prior to this step!

---

**I. VIA ADMIN API:**
http://127.0.0.1:8080/rest/service/getServiceToken?name=_______&tempSecret=______  </br>
  tempSecret may be left out initially from the query param. However the name of the microservice is required (name you registered account under), to make sure you retrieve the right JWT token. <br/>
  If tempSecret is left out, you will receive the tempSecret string that has been encrypted with the public key (that you registered). <br/>
  In order to get the actual JWT token for your service account, you must decrypt the tempSecret string with your own private key (from the public key/private key pair) and call this API endpoint again with the decrypted string passed in tempSecret query param. If this tempSecret matches the one given, you will receive the JWT token inside of the Response body within a JSON object. </br>
  
---

**II. VIA ONE-TIME PYTHON DUMP SCRIPT**
If you do not have an admin account, you can still obtain the Service Token of a existing Service Account (created by Admin) </br>
STEPS:</br>
a) Start API gateway via command line in detached mode, within the project root. </br>
```
docker-compose up -d
```
b) List all currently running docker containers, after the gateway has started up. </br>
```
docker ps
```
c) Locate the running container for Postgres Name: res_db-account_1. Find the local host port for the container.
EXAMPLE PORT: 0.0.0.0:35085->5432/tcp </br>
In this case port 35085. </br>
d) Within the project directory, change directories into /settings/python. Open dump_token.py and locate the line
```
try:
	conn_string = "host='localhost' port=35073'' dbname='account' user='jetty' password= 'jettypass'"
```
Change the port number into the one you found in the docker ps step. This will ensure you are able to connect to the account
db. Save the file. </br>
e) Run dump_token.py passing in your Service name as command line arg. </br>
```
python dump_token.py sample
```
**NOTE: sample is the Service Account name. This python script requires psycopg2 python module in order to connect to postgres DB** </br>
This step will dump your Service Token into the console. Please save this token somewhere secure. Usage process described below for Service Token.
### 4. Use Service Account Token
  Grab the JWT token from the JSON object you received from http://127.0.0.1:8080/rest/service/getServiceToken, or from the console output, and place this into every request header for your microservice. <br/>
Header name: authorization  
Header content: Bearer [place jwt token here after a single white space]  
**This token is required in order to use any of the Service APIS, such as the Session Get/Set API** </br>
### 5. Utilizing Shared SessionStore
The true benefit of using this API gateway, is that a session store and user store is already available to be used by all microservices deployed with it. This eliminates the problem of having to create/manage a session store within your microservice.
#### Using Provided Login/ Logout
If you have properly configured your microservice within NGINX and Docker, any request to your registered endpoints will bounce unauthenticated users to the shared Login page provided by the API gateway. </br>
There are three separate options for logging in. 
**Login:**
http://127.0.0.1:8080/rest/login </br>
1) Login Locally: First press the signup link to signup for an account. Then follow the directions onscreen to login.
2) Login SSO: Click on the github link to login via SSO through github. 
3) Login Guest: Option to continue as guest role is available, note that any user information will not be saved. However
you will still receive a session and JWT token like the other login options. </br>
**Logout:** 
http://127.0.0.1:8080/rest/logout </br>
After your users have logged in, they will be provided a session along with a JWTtoken saved within Cookie "JwtToken". 
**As the microservice, you must decrypt the JwtToken provided in EACH request by using the shared Public Key (mounted in Docker).** </br>
##### Example Kotlin Code For Decrypting JWT Token
cookie passed in parseClaimsJws(cookie) is the actual JwtToken cookie provided by the login action explained above.
```
 var publicKey = (this::class.java.classLoader).getResource("pki/Public.key").readText().toByteArray()
        publicKey = Base64.getDecoder().decode(publicKey)
        val res = Jwts.parser().setSigningKey(RSAPublicKeyImpl(publicKey)).parseClaimsJws(cookie).body
        return Response.status(Status.OK).entity(res).build()
```        
If you are able to successfully decrypt the token with no exceptions, than this means that the user has been authenticated 
and should be allowed into your service.
#### Accessing SessionStore (Session API)
In order to get/set into a specific user's session, you must have a reference to the user's session id that is passed into
the Session API.
**MUST HAVE SERVICE ACCOUNT TOKEN AS [authorization: Bearer nfjfkjbfkjbefkjebf] in header. You must also have sessionOperator role allowed inside of this token in order to get and set attributes inside of the session.**
##### Get Attributes from Session
http://127.0.0.1:8080/rest/session/get?key=________&id=_________ </br>
**key query param**: This param should be either the name of the attribute stored in the session, or the dotted notation json attribute path to the key you would like to retrieve. <br/>
**id query param**: This param is where you pass in the desired Session ID that you would like to get an attribute from. </br>
For example either myAttribute or myAttribute.a.b.c (the second will go into the attribute myAttribute and return the value of c). 

##### Set Attributes in Session
http://127.0.0.1:8080/rest/session/set?key=________&id=__________ </br>
**key query param**: Same as the get Attribute key query param. Except that it will create the json objects if they do not exist. <br/>
**id query param**: This param is where you pass in the desired Session ID that you would like to set attribute into.
EXAMPLE: A.B.C -> will store whatever json object/arrays you pass in the request body at attribute C, that is within json objects B and A. The attribute name stored in the session itself will be A. <br/>
**You can add/replace json objects but you can only replace json Arrays (cannot add elements into the array, will replace the whole thing)** </br>
## Admin Section

---
### Service Account Crud API
*NOTE: ASSIGNING NEW ROLES TO SERVICE ACCOUNT IS DONE VIA ROLE CRUD API*
#### CREATE SERVICE ACCOUNT
Example  POST endpoint:
http://127.0.0.1:8080/rest/service/create?name=_____ </br>
**The NAME query parameter**: Pass in the name that you desire the Microservice to be called.<br/>
**Request body**: Pass in the Microservice's own RSA public key (please generate your own public key/private key pair) as part of a json object with the key name being "publickey". <br/>
Your passed in Microservice's public key, will be used in order to encrypt a randomized tempSecret string. This is to make sure that any requests for the JWT Service Account token are only retrieved/used by the authorized Service Account. <br/>
*Example Body Format*
```
{ 
  "publickey": "Your public key"
}
```
#### READ SERVICE ACCOUNT(S)
Example GET endpoint:
http://127.0.0.1:8080/rest/service/read?name=_____ </br>
**The NAME query parameter (optional)**: Pass in specific Service Account name you would like to retrieve information on. If left out, all services will be returned in a json object. </br>
#### UPDATE SERVICE ACCOUNT
Example POST endpoint:
http://127.0.0.1:8080/rest/service/update?name=_____ </br>
**The NAME query paranmeter**: Pass in name of service account you would like to update with a new public key. </br>
**Request body**: Pass in Microservice's own RSA public key (please generate your own public key/private key pair), and pass it in a json object in the same format as creating a service account.
*Example Body Format*
```
{
   "publickey": "Your public key"
}
```
#### DELETE SERVICE ACCOUNT
EXAMPLE GET endpoint:
http://127.0.0.1:8080/rest/service/delete?name=_____ </br>
**The NAME query parameter**: Pass in specific Service Account name you would like to delete 

---
### User Account Crud API
*NOTE: ASSIGNING NEW ROLES TO USER ACCOUNT IS DONE VIA ROLE CRUD API*
#### CREATE USER ACCOUNT
EXAMPLE POST endpoint:
http://127.0.0.1:8080/rest/user/create </br>
**Request body**: Pass in both the "username" and "password" within a json object in the format of the below example. </br>
*Example Body Format*
```
{
   "username" : "example@gmail.com",
   "password" : "mypassword
}
```
#### READ USER ACCOUNT
EXAMPLE GET endpoint:
http://127.0.0.1:8080/rest/user/read?name=_____ </br>
**The NAME query parameter (optional)**: Pass in a specific User Account you would like to retrieve information from. If this
query parameter is left out, will retrieve information for all user accounts. </br>
#### UPDATE USER ACCOUNT
EXAMPLE POST endpoint:
http://127.0.0.1:8080/rest/user/update?name=_____ </br>
**The NAME query parameter**: Pass in User Account name that you would like to update with a new password </br>
**Request Body**: Within a json object, using key "password", pass in your desired new password. </br>
*Example Body Format*
```
{
  "password": "my new password"
}
```
#### DELETE USER ACCOUNT
EXAMPLE GET endpoint:
http://127.0.0.1:8080/rest/user/delete?name=_____ </br>
**The NAME query parameter**: Pass in a specific User Account you would like to delete </br>

---
### Role Crud API 
This API allows someone with an Admin role, to create new roles (will save in DB), delete roles, update existing roles with new permissions, or assign roles to specific service accounts or user accounts(identified by name).
#### CREATE ROLE
http://127.0.0.1:8080/rest/role/create?name=_______ </br>
**name query param**: Name of the new role you want to add. 
#### DELETE ROLE
http://127.0.0.1:8080/rest/role/delete?rname=______&name=______&type=_______ </br>
**rname query param**: Name of role you want to delete from DB.
**name query param (optional)**: Specific User Account or Service Account you want to delete this role from
**type query param (needed if passing in name query)**: Pass in "service" to specify for Service Accounts, any other string will count as User Account.
#### ASSIGN ROLE
http://127.0.0.1:8080/rest/role/assign?rname=_______&name=______&type=______ </br>
**rname**: Role name to assign  <br/>
**name**: Either service account name or user's username <br/>
**type**: service or user. <br/> **Must put "service" to identify it is a service account. Else it will always look in user accounts.**
#### UPDATE ROLE
http://127.0.0.1:8080/rest/role/update?name=_______ </br>
**name**: Role name to update permissions on. All previous permissions will be replaced for this role. <br/>
**Request body**: Please pass in a JSON array with the permission names. The whole array of permissions will be set to this role. <br/>

---
### Bootstrapping Initial Data
##### If you would like to bootstrap the initial Role, User, and Permission data for the database, please append/insert SQL commands into init.sql. 

---
### EXAMPLE USAGE FOR MICROSERVICES (WIP)
Please take a look at the SampleService repository located at https://github.com/ch3riee/SampleService/ for reference </br>
#### 1. Please first register your microservice name and public key via an admin account.
Follow instructions as described up above to register microservice.
**EXAMPLE FOR GENERATING PUBLIC/PRIVATE KEY PAIR (WIP)** </br>
```
KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
kpg.initialize(2048);
KeyPair kp = kpg.generateKeyPair();
Key pub = kp.getPublic();
Key pvt = kp.getPrivate();
```
#### 2. Next you must first get your auto-generated encrypted tempSecret and decrypt it with your private key.
**EXAMPLE for decrypting tempSecret using Cipher:** </br>
```
var privateKey = (this::class.java.classLoader).getResource("pki/sample/Private.key")
                .readText()
                .toByteArray()
privateKey = Base64.getDecoder().decode(privateKey)
val cipher2 = Cipher.getInstance("RSA")
cipher2.init(Cipher.PRIVATE_KEY, RSAPrivateCrtKeyImpl.newKey(privateKey))
val ret = cipher2.doFinal(DatatypeConverter.parseBase64Binary(root.get("tempSecret").textValue()))
return String(ret)
```
Follow previous instructions for using Service Account API in order to exchange decrypted tempSecret string for Service Token
#### 3. Save this Service Token somewhere secure and add as Authorization Bearer Token to every Service Account request.
**EXAMPLE for making Service Account request to Session API Set using Mashape Unirest Library (WIP) **
```  
//grab the JSESSIONID cookie in order to get the session ID for current user.
//here is an example of what it looks like and how to parse
val cookie = node0za9a8csxs4xi1ri5w49vo97qd1.node0 
//drop the .node0 to get correct session id
val cartResponse= Unirest.post("http://srvjavausers:8081/rest/session/set/")
                         .queryString("key", "a")
                         .queryString("id", cookie.split('.').get(0))
                         .header("Content-Type", "application/json")
                         .header("authorization", "bearer " + "eyJhbGciOiJSUzUxMiJ9.eyJzdWIiOiJzYW1UiLCJUb9.J2CXyY1gkQT6azg")
                         .body(mapper.writeValueAsString(mapper.readTree(body)))
                         .asString() //using .asJson() requires knowing the proper json format, safer to use asString
val jsonObj = cartResponse.rawBody
```
*NOTE:* Example Service Account JWT Token has been truncated for example purposes </br>

---
### ADDITIONAL COMMENTS
1. If a call to the APIs returns an NGINX generated 404, this means that NGINX is missing this static content, or there is no existing configuration location for this URI. </br>
2. If a call to the APIs returns a JETTY generated 404, this means that there is a missing API implementation. </br>
3. For SSO login via github, regardless of original request before redirect to login page, a successful login will take you to the Login Success page. For any of the other login options, successful login will redirect you to your original request page.



