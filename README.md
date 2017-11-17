
![Alt text](diagram.png?raw=true "Request Class Path Diagram")


# Nginx Gateway and Jetty-based Session/User Store
An api gateway for microservices that utilizes Nginx, Jetty, and Jersey. Provides a session store, user store, along with 
authorization and authentication features. Contained within Docker Containers.
## SETUP
### 1. Download this github repo
```
git clone https://github.com/ch3riee/Security.git .
```
### 2. Inside of the project folder, run command for Docker-Compose to start up the gateway/server
```
docker-compose up
```
### 3. Example Rest Endpoint to check if working, go to
```
http://127.0.0.1:8080/rest/hello
```
This should bounce you automatically to the login page at: http://127.0.0.1:8080/rest/login
## WEB USAGE
### SIGNUP/LOGIN
There are three separate options for logging in. 
1) Login Locally: First press the signup link to signup for an account. Then follow the directions onscreen to login.
2) Login SSO: Click on the github link to login via SSO through github. 
3) Login Guest: Option to continue as guest role is available, note that any user information will not be saved. However
you will still receive a session and JWT token like the other login options.
```
http://127.0.0.1:8080/rest/login
```
### LOGOUT
```
http://127.0.0.1:8080/rest/logout
```
Will logout of any sessions. Used for user accounts not service accounts!
### WEB JWT TOKEN:
JWT Token Web Types: web:local, web:sso, web:guest <br/>

Once you login via one of these methods, the JWTToken will be placed inside of Cookie "JwtToken". To check the contents of
this token, you can go to the http://127.0.0.1:8080/rest/checkJWT endpoint to see a printout of the contents.  <br/>
NOTE: THIS IS ONLY FOR NORMAL USER/GUEST ACCOUNTS. NOT FOR SERVICE ACCOUNT JWTTOKENS!!!! <br/>
## MICROSERVICE REGISTRATION/ SERVICE TOKEN
NOTE: MUST BE AN ADMIN ROLE (Have user account with admin role assigned to it), IN ORDER TO REGISTER A NEW SERVICE ACCOUNT AND GET THE JWT TOKEN (Authorization bearer token)
### Register new service account (as admin)
Example  POST endpoint:
```
http://127.0.0.1:8080/rest/services/register?name=_____
```
The NAME query parameter: Pass in the name that you desire the Microservice to be called.<br/>
Post body: Pass in the Microservice's own RSA public key (please generate your own public key/private key pair). <br/>
Your passed in Microservice's public key, will be used in order to encrypt a randomized tempSecret string. This is to make sure that any requests for the JWT Service Account token are only retrieved/used by the authorized Service Account. <br/>
### Get the JWT Service Account Token
```
http://127.0.0.1:8080/rest/services/getServiceToken?name=_______&tempSecret=______
```
  tempSecret may be left out initially from the query param. However the name of the microservice is required (name you registered), to make sure you retrieve the right JWT token. <br/>
  If tempSecret is left out, you will receive the tempSecret string that has been encrypted with the public key (that you registered). <br/>
  In order to get the actual JWT token for your service account, you must decrypt the tempSecret string with your own private key (from the public key/private key pair) and call this API endpoint again with the decrypted string passed in tempSecret query param. If this tempSecret matches the one given, you will receive the JWT token inside of the Response body within a JSON object. 

### USE THE JWT SERVICE ACCOUNT TOKEN
  Grab the JWT token from the JSON object you received from http://127.0.0.1:8080/rest/services/getServiceToken and place this into every
request header for your microservice. <br/>
Header name: authorization  
Header content: Bearer [place jwt token here after a single white space]  
**This token is required in order to use any of the Service APIS, such as the Session Get/Set API

## SESSION GET/SET API
**MUST HAVE SERVICE ACCOUNT TOKEN AS [authorization: Bearer nfjfkjbfkjbefkjebf] in header. You must also have sessionOperator role allowed inside of this token in order to get and set attributes inside of the session.
### Get Attributes from Session
```
http://127.0.0.1:8080/rest/session/get?key=________
```
key query param: This param should be either the name of the attribute stored in the session, or the dotted notation json attribute path to the key you would like to retrieve. <br/>
For example either myAttribute or myAttribute.a.b.c (the second will go into the attribute myAttribute and return the value of c). 

### Set Attributes in Session
```
http://127.0.0.1:8080/rest/session/set?key=________
```
key query param: Same as the get Attribute key query param. Except that it will create the json objects if they do not exist. <br/>
EXAMPLE: A.B.C -> will store whatever json object/arrays you pass in the request body at attribute C, that is within json objects B and A. The attribute name stored in the session itself will be A. <br/>
**You can add/replace json objects but you can only replace json Arrays (cannot add elements into the array, will replace the whole thing)

## ROLE CRUD API (Admin use only)
This API allows someone with an Admin role, to create new roles (will save in DB), delete roles, update existing roles with new permissions, or assign roles to specific service accounts or user accounts(identified by name).
### CREATE ROLE
```
http://127.0.0.1:8080/rest/role/create?name=_______
```
name query param: Name of the new role you want to add. 
### DELETE ROLE
```
http://127.0.0.1:8080/rest/role/delete?name=_______
```
name query param: Name of role you want to delete from DB.
### ASSIGN ROLE
```
http://127.0.0.1:8080/rest/role/assign?rname=_______&name=______&type=______
```
rname: Role name to assign  <br/>
name: Either service account name or user's username <br/>
type: service or user. <br/> **Must put "service" to identify it is a service account. Else it will always look in user accounts.
### UPDATE ROLE
``` 
http://127.0.0.1:8080/rest/role/update?name=_______
```
name: Role name to update permissions on. All previous permissions will be replaced for this role. <br/>
Request body: Please pass in a JSON array with the permission names. The whole array of permissions will be set to this role. <br/>

## DOCKER-COMPOSE/ NGINX GATEWAY 
Adding a microservice to this gateway is a two step process.
### 1. Adding Microservices to Docker-Compose
Please read the following documentation at 
```
https://docs.docker.com/compose/
```
to learn about docker-compose. </br>
In order to add your microservice to the api gateway, a separate container for your microservice must be added to the
docker-compose file located in the downloaded project folder. Please use the existing containers as examples if needed. </br>
#### Public Key Volume Mount Requirement
For every microservice Docker container added to docker-compose, you must mount the shared public key folder as a volume. 
This volume can be copied from the volume in docker container srv-java-users. It allows the microservice to have access to the public key, in order to decrypt JWT token generated by this service. 
```
volumes:
  - ./src/main/resources/pki/Public.key:/var/lib/pki-public
```
#### Depends-On Requirement
Please add your container name to the depends_on section of the web container (nginx container). 

### 2. Adding Microservices to NGINX site.conf.template
Within the project folder, locate ./settings/nginx/conf.d/site.conf.template. This file holds the NGINX configuration that is
copied into default.conf by the NGINX container at command docker-compose up. Each microservice must be added to this configuration in order for them to be deployed along with the existing API gateway. 
#### Section: WEB
This commented section in the NGINX configuration is where any web applications (microservices) meant for web users is placed. There is already a sample web application within this section. Any microservice added to this section must have the 
```
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
if($Http_authorization= ""){
   return 403;
}
```
#### Section: STATIC
  This commented section in the NGINX configuration is where static content location blocks should be placed. For more information regarding configuring static content, please go to </br>
https://www.nginx.com/resources/admin-guide/serving-static-content/ </br>
  When serving static content, a root or alias path must be set in the location block. If you would like to use the current static folder to serve your content, without altering any docker-compose volume mounts, please place all static content within the /resources/static folder in the project folder. This will be mapped to /var/lib/static/ within the NGINX docker container. </br>
  NOTE: If static content is placed somewhere else, please set this as a volume mount within the NGINX docker container in docker-compose file. The path within the docker container itself is the one that should be passed as the alias or root. </br>
EXAMPLE: Looks for /var/lib/static/index.html file and serves the content
```
    location /static/ {
        alias /var/lib/static/ ;
        index index.html ;
    }
```



