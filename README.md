
# API Gateway and Shared Session/User Store
   As monolithic-based applications have given way to more and more applications following the microservice architecture pattern, new challenges for session management design as well as authentication and authorization have arisen. Choices must be made between a single externalized session management system, versus independent internalized session stores contained within each microservice. Tradeoffs must be examined carefully between the usage of a centralized authorization server and the delegation of permission guarding to individual microservices. While the microservice architecture boasts advantages of scalability, fault isolation, and flexibility in application development, it also carries with it the complexities of developing distributed systems and the coordination of independent modules. Amidst all these possible courses of actions within microservice architecture design, it can be difficult for developers to began developing their own microservices. Add to this the plethora of frameworks, programming languages, databases, and servers available, and you are faced with an onslaught of countless decisions to be made. Luckily, we have already made these choices, allowing developers to hit the ground up and running so that they can focus on creating finely-grained  lightweight customer-centered services.  
	By encapsulating the anatomy of the applications behind a single API gateway entry point, we gain an additional layer of security along with ease of deployment and scaling. While gateways provide many benefits, it is of course yet another possible developmental bottleneck for developers. Thus we have set up and configured the API gateway, abstracting away the drawbacks yet still providing the benefits to users of our service. We have chosen to provide a shared session store along with our gateway service, thus minimizing the individual complexities of each microservice as well as alleviating the issue of session and data interoperability between many loosely coupled services.  Our service also supplies a convenient authentication service that includes features such as Github single sign on and guest login, and combines session based authentication with token based authentication. Important account information is compactly and securely shared by Json Web Tokens, allowing microservices to easily incorporate our authentication service with their service code.  The use of these tokens also allows microservices the flexibility of guarding their own resources while removing the need to create their own permission and role stores. By choosing to delegate the enforcement of permissions and roles to microservices, we achieve the bonus of customizable authorization handling as well as authorization fault isolation. 
	Ultimately, our service provides ease of continuous development, scalability, and less redundancy for developers who want to get started immediately in deploying microservices via the microservice architecture. </br>
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
To check if working, go to http://127.0.0.1:8080/rest/public/gateway/sample/hello </br>
This should bounce you automatically to the login page at: http://127.0.0.1:8080/rest/public/gateway/session/login </br>
## URL SCHEMA
### 1. Microservices URL Schema
Please configure and implement your microservice urls by following this schema. All microservices internal or public should be prefixed by `/rest/`, followed by **public** or **internal**, followed by microservice name (in our case "gateway"), and then followed by any modules. The rest of the URL is up to you. For example our login page lies at: http://127.0.0.1:8080/rest/public/gateway/session/login. For more examples please look at the [Reference.md](Reference.md) file.
## DOCKER
### 1. Brief Notes On Making Changes To This Gateway's  Java Jar File </br>
This security gateway and all of its features have been bundled together into a Java jar file that is mounted into a DockerFile, which is then included within the docker-compose.yml file under the container srvjavausers. The Java jar was built using the library shadowJar using the command:
```
./gradlew shadowJar
```
The built jar is automatically placed within ./build/libs/rest-1.0-SNAPSHOT-all.jar. When local changes are made to the security gateway, after docker-compose up has been called, you must run this gradle command in order to bundle changes into a new Java jar. Enter the command:
```
docker-compose restart srvjavausers
```
This will restart the container housing the gateway, replacing the original jar file bundled within the DockerFile with the new one. Thus allowing the code changes to be applied.
## STATIC CONTENT DEPLOYMENT
### 1. Saving Your Static Content </br>
Save static content into /resources/static folder inside of the downloaded project folder to easily serve static files. Or save into folder of your choice.
### 2. Mount Static Folder Into Docker Volume </br>
The /resources/static folder will be automatically mounted within docker-compose.yml file as a volume to the NGINX docker container. If you would prefer to put your static files within another directory, please make sure to mount the directory as a volume as well. The docker-compose.yml file is located at the project root. </br>
To mount your own static content folder, modify this line in docker-compose.yml: </br>
```
- ____your static folder path______:/var/lib/static

```
### 3.  NGINX Configuration for Static Content </br>
 Please locate the site.conf.template NGINX configuration file within the project directory at /settings/nginx/conf.d. The static commented section in the NGINX configuration is where static content location blocks should be placed. There is no authentication required for accessing static pages. For more information regarding configuring static content, please go to https://www.nginx.com/resources/admin-guide/serving-static-content/ </br>
  When serving static content, a root or alias path must be set in the location block. If you placed your static content within the /resources/static folder in step 1 and mapped it to /var/lib/static, you can copy the alias of the example in the config.  Otherwise please replace the alias with the volume path you mapped into (*Right hand side of colon*).  Also pass in your static file name as the index. </br>
EXAMPLE: Looks for /var/lib/static/index.html file and serves the content for context root 
```
#Static Section
location / {
    alias /var/lib/static/ ;
    index index.html ;
}
```
After configuration, if you set location path as context root and followed the steps above, you should be able to access your static content at http://127.0.0.1:8080/ </br>
## WEB SERVICE DEPLOYMENT
### 1. Add Microservice Container to Docker-Compose.yml </br>
   Locate the docker-compose.yml file at project root. If you are unfamiliar with docker-compose, please read the following documentation at https://docs.docker.com/compose/ . In order to add your microservice to the API gateway, first add a separate container for your microservice within the docker-compose file.  For an example please look at container srv-sample.  </br>
### 2. Add srvjavausers Dependency to Your Microservice Container </br>
    If you are using our shared session store or user store please add a dependency to the srvjavausers container within docker-compose.yml under your microservice container.
```
depends_on:
- srvjavausers
```
### 3. Add Container Dependency to NGINX Container </br>
     Locate the NGINX container with docker-compose.yml and go to the depends-on section. Please add your microservice container name as a depends-on requirement to make sure Docker will deploy containers correctly.   </br>
```
depends_on:
  - srv-my-microservice
```
### 4. Mount Public Key Volume </br>
    Within docker-compose.yml under the configuration for your microservice container, please add the following volume. </br>
```
- ./src/main/resources/pki/Public.key:/var/lib/pki-public
```
This will map the public key of the built-in authentication service into your service container. This public key is needed to decrypt JWT tokens generated by the authentication service, so that you may have access to important account and permission information for the authenticated users. An explanation of how to decrypt JWT tokens is located in the using shared session store section below. </br>
### 5. NGINX Configuration for Web Service Content </br>
  Please locate the site.conf.template NGINX configuration file within the project directory at /settings/nginx/conf.d. This web commented section in the NGINX configuration is where any web applications (microservices) meant for web users will be mapped to. Residing in this section is the web service location block. Within it, the block
```
#Web Section
if ($http_cookie !~ "JwtToken=")
{
  return 301 $scheme://$host:8080/rest/login ;
}
```
guards against unauthenticated users from accessing PUBLIC web service resources. If this JWT token does not exist in the user's request header, NGINX will proxy redirect to the login endpoint. JWT tokens are automatically generated by the authentication service after successful authentication. </br>

#### ADDING YOUR PUBLIC WEB MICROSERVICE TO NGINX
1) First, define your microservice servers under an upstream block within the Nginx configuration file (site.conf.template). Internal and public servers for your microservice can be defined in the same upstream block. However they must be placed in the proper internal or public map block (step 2).
```
upstream microservice-sample {
    server   srvsample:8000;
}

upstream microservice-gateway {
    server   srvjavausers:8081;
}
```
Please look at the Nginx documentation for more information about upstream services. We allow you to add microservices in this way, in order to enable load balancing for better performance. The name you name your upstream server, in this case 
"microservice-sample" and "microservice-gateway, will be used within the map block below to locate the proper server for nginx to proxy to. Please name your upstream server block by following the naming schema "microservice-" plus the name of your microservice. In this case the names were "sample" and "gateway". </br>
2) Second, locate the public web services map block inside of the Nginx configuration file. The default is the gateway server.
```
map $uri $public_services {
    default   http://microservice-gateway ;

    # Add your public microservice URL regex and server location map entry here: ###

    ~^/rest/public/sample/.*   http://microservice-sample ; 
    ~^/rest/public/sample2/.*   http://microservice-sample ;  //just an example
    # ...

    ################################################################################

}
```
There are two map blocks, one for any internal services that microservices can use (ex: session get/set api) and one for public web services for web users. You will be adding to the first map block, which is the public one. There is already an example sample web service that is mapped from the URL to the correct server. Nginx will use the same location block and this map in order to proxy pass to the correct server. The default server will be this gateway hosted at http://srvjavausers:8081, under the upstream server name http://microservice-gateway. For more information please take a look at the Nginx documentation. </br>

#### ADDING YOUR INTERNAL MICROSERVICE TO NGINX
1) First, define your microservice servers under an upstream block within the Nginx configuration file (site.conf.template). Internal and public servers for your microservice can be defined in the same upstream block. However they must be placed in the proper internal or public map block (step 2).
```
upstream microservice-sample {
    server   srvsample:8000;
}

upstream microservice-gateway {
    server   srvjavausers:8081;
}
```
Please look at the Nginx documentation for more information about upstream services. We allow you to add microservices in this way, in order to enable load balancing for better performance. The name you name your upstream server, in this case 
"microservice-sample" and "microservice-gateway, will be used within the map block below to locate the proper server for nginx to proxy to. Please name your upstream server block by following the naming schema "microservice-" plus the name of your microservice. In this case the names were "sample" and "gateway". </br>
2) Locate the internal web services map block inside of the Nginx configuration file. The default is also the gateway server.
```
map $uri $internal_services{
    default     http://microservice-gateway ;
    ~^/rest/internal/gateway/.*   http://microservice-gateway ;
}

```
There are two map blocks, one for any internal services that microservices can use (ex: session get/set api) and one for public web services for web users. You will be adding to the second map block, which is the internal one. Here you can see how our internal gateway services have been mapped. Nginx will use the same location block and this map in order to proxy pass to the correct server. The default internal server will be this gateway hosted at http://srvjavausers:8081, under the upstream server name http://microservice-gateway. For more information please take a look at the Nginx documentation. </br>

## Using Shared Session Store, Authentication, Authorization
### 1. Create Service Account (Requires Admin Role)
If you have admin role access, please use this API in order to register your microservice. Otherwise please have an admin follow these steps. Must have a service account in order to use the shared session store. </br>
Example  POST endpoint:
http://127.0.0.1:8080/rest/public/gateway/service/create?sname=_____ </br>
**The SNAME query parameter**: Pass in the name that you desire the Microservice to be called.<br/>
**Request body**: Pass in the Microservice's own RSA public key (please generate your own public key/private key pair) as part of a json object with the key name being "publickey". <br/>
Your passed in Microservice's public key, will be used in order to encrypt a randomized tempSecret string. This is to make sure that any requests for the JWT Service Account token are only retrieved and used by the authorized Service Account. (One of the ways to get the service account token, admin role required) <br/>
*Example Body Format*
```
{ 
  "publickey": "Your public key"
}
```
### 2. Retrieve Service Account Token (via Admin)
http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken?sname=_______&tempSecret=______  </br>
  tempSecret may be left out initially from the query param. However the name of the microservice is required (name you registered account under), to make sure you retrieve the right JWT token. <br/>
  If tempSecret is left out, you will receive the tempSecret string that has been encrypted with the public key (that you registered). <br/>
  In order to get the actual JWT token for your service account, you must decrypt the tempSecret string with your own private key (from the public key/private key pair) and call this API endpoint again with the decrypted string passed in tempSecret query param. If this tempSecret matches the one given, you will receive the JWT token inside of the Response body within a JSON object.  Please save this token somewhere secure. </br>
### 3. Retrieve Service Account Token (via Python dump script, no Admin required)
If you do not have an admin account, you can still obtain the Service Token of a existing Service Account (created by Admin) </br>
#### a)  Pip Install 
Please locate  requirements.txt within /util/requirements.txt  in project folder, and do a pip install.  (Set up virtual env if desired prior to this step) </br>
This step is only necessary if you do not have psycopg2 version 2.7.3.2 installed. This is used to access the postgres db that holds the account information. </br>
```
pip install -r requirements.txt
```
#### b) Start up docker-compose
Using the command line, start up docker-compose in detached mode, within the project root. </br>
```
docker-compose up -d
```
#### c) Within the project directory, change directories into /settings/python. Run dump_token.py passing in your Service name as command line arg. </br>
```
python dump_token.py sample
```
**NOTE: sample is the Service Account name. This python script requires psycopg2 python module in order to connect to postgres DB** </br>
This step will dump your Service Token into the console. Please save this token somewhere secure. Usage process described below for Service Token. </br>
### 4. Use Service Account JWT Token
Grab the JWT token from the JSON object you received from http://127.0.0.1:8080/rest/public/gateway/service/getServiceToken, or from the console output, and place this into every request header for your microservice. <br/>
**Header name**: authorization  
**Header content**: Bearer [place jwt token here after a single white space]  
*This token is required in order to use any of the Service APIS, such as the Session Get/Set API* </br>
### 5. Use Shared Session Store API
In order to get/set into a specific user's session, you must have a reference to the user's session id that is passed into the Session API. </br>
*Prior to this step please follow step 4 and put [authorization: Bearer nfjfkjbfkjbefkjebf] in header. You must also have sessionOperator role allowed inside of this token in order to get and set attributes inside of the session.* </br>
#### a) Get Attributes from Session
http://127.0.0.1:8080/rest/internal/gateway/session/get?key=________&id=_________ </br>
**key query param**: This param should be either the name of the attribute stored in the session, or the dotted notation json attribute path to the key you would like to retrieve. <br/>
**id query param**: This param is where you pass in the desired Session ID that you would like to get an attribute from. </br>
For example either myAttribute or myAttribute.a.b.c (the second will go into the attribute myAttribute and return the value of c). 
#### b) Set Attributes in Session
http://127.0.0.1:8080/rest/internal/gateway/session/set?key=________&id=__________ </br>
**key query param**: Same as the get Attribute key query param. Except that it will create the json objects if they do not exist. <br/>
**id query param**: This param is where you pass in the desired Session ID that you would like to set attribute into. </br>
**EXAMPLE** : A.B.C -> will store whatever json object/arrays you pass in the request body at attribute C, that is within json objects B and A. The attribute name stored in the session itself will be A. <br/>
**You can add/replace json objects but you can only replace json Arrays (cannot add elements into the array, will replace the whole thing)** </br>
### 6. User Authentication
If you have properly configured your microservice within NGINX and Docker, any request to your registered endpoints will bounce unauthenticated users to the shared Login page provided by the API gateway. </br>
There are three separate options for users to login  at http://127.0.0.1:8080/rest/public/gateway/session/login and one way for users to logout at http://127.0.0.1:8080/rest/public/gateway/session/logout. </br>
#### a) Login Locally
First press the signup link to signup for an account. Then follow the directions onscreen to login. </br>
#### b) Login SSO
Click on the github link to login via SSO through Github.  </br>
#### c) Login Guest
Option to continue as guest role is available, note that any user information will not be saved. However you will still receive a session and JWT token like the other login options. </br>
#### d) Logout
Logout by calling http://127.0.0.1:8080/rest/public/gateway/session/logout. This will invalidate the user’s session </br>
### 7. Use User JWT Token + Authorization
#### a) Decrypt User JWT Token Via JJWT Lib
Grab the user’s JwtToken cookie, which is automatically set by the authentication service for all authenticated users who have logged in. We are using Java JJWT library located at https://github.com/jwtk/jjwt. Please add this library as a gradle dependency prior to attempting to decrypt JWT Token if following our decryption example. </br>
```
var publicKey = (this::class.java.classLoader)
                        .getResource("pki/Public.key")
                        .readText().toByteArray()
publicKey = Base64.getDecoder().decode(publicKey)
val res = Jwts.parser()
                     .setSigningKey(RSAPublicKeyImpl(publicKey))
                     .parseClaimsJws(cookie).body
return Response.status(Status.OK).entity(res).build()
```        
If you are able to successfully decrypt the token with no exceptions, than this means that the user has been authenticated and should be allowed into your service. </br>
#### b) Authorization 
Our gateway service allows all microservices to customize their own authorization handling process. In order to gain access to user permissions, you must first decrypt the user JWT Token (Step a. of this section). Within this decrypted JWT token, you will find information on the user’s roles and permissions. Please use this data to create your own custom authorization validation service within your microservice, in order to enforce permissions. One possible way to do this is to use these permissions in creating your own security annotations. </br>
## BOOTSTRAPPING INITIAL DATA
If you would like to bootstrap the initial Role, User, and Permission data for the database, please append/insert SQL commands into init.sql located at /settings/postgre/sql. The only required table is the Permission table since permissions are static due to the corresponding security annotations being static in the API code. </br>
## ADMIN API REFERENCE
For detailed API reference please look at Reference.md file located within this repository. </br>
## JETTY REQUEST DIAGRAM
If you would like more details on how the Jetty Server (Authentication Server/ Admin API Server) handles requests please look at this following diagram. This diagram shows the callstack for handling an Http Request. </br>
![Alt text](diagram.png?raw=true "Request Class Path Diagram")
</br>
## ADDITIONAL COMMENTS
1. If a call to the APIs returns an NGINX generated 404, this means that NGINX is missing this static content, or there is no existing configuration location for this URI. </br>
2. If a call to the APIs returns a JETTY generated 404, this means that there is a missing API implementation. </br>
3. For SSO login via github, regardless of original request before redirect to login page, a successful login will take you to the Login Success page. For any of the other login options, successful login will redirect you to your original request page. </br>




