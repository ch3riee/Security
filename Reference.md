## ADMIN API REFERENCE
### Service Account Crud API
*NOTE: ASSIGNING NEW ROLES TO SERVICE ACCOUNT IS DONE VIA ROLE CRUD API*
#### 1. CREATE SERVICE ACCOUNT
Example  POST endpoint:
http://127.0.0.1:8080/rest/public/gateway/service/sname=_____ </br>
**The SNAME query parameter**: Pass in the name that you desire the Microservice to be called.<br/>
**Request body**: Pass in the Microservice's own RSA public key (please generate your own public key/private key pair) as part of a json object with the key name being "publickey". <br/>
Your passed in Microservice's public key, will be used in order to encrypt a randomized tempSecret string. This is to make sure that any requests for the JWT Service Account token are only retrieved/used by the authorized Service Account. <br/>
*Example Body Format*
```
{ 
  "publickey": "Your public key"
}
```
#### 2. READ SERVICE ACCOUNT(S)
Example GET endpoint:
http://127.0.0.1:8080/rest/public/gateway/service?sname=_____ </br>
**The SNAME query parameter (optional)**: Pass in specific Service Account name you would like to retrieve information on. If left out, all services will be returned in a json object. </br>
#### UPDATE SERVICE ACCOUNT
Example PUT endpoint:
http://127.0.0.1:8080/rest/public/gateway/service?sname=_____ </br>
**The SNAME query paranmeter**: Pass in name of service account you would like to update with a new public key. </br>
**Request body**: Pass in Microservice's own RSA public key (please generate your own public key/private key pair), and pass it in a json object in the same format as creating a service account.
*Example Body Format*
```
{
   "publickey": "Your public key"
}
```
#### 3. DELETE SERVICE ACCOUNT
EXAMPLE DELETE endpoint:
http://127.0.0.1:8080/rest/public/gateway/service?sname=_____ </br>
**The SNAME query parameter**: Pass in specific Service Account name you would like to delete 

---
### User Account Crud API
*NOTE: ASSIGNING NEW ROLES TO USER ACCOUNT IS DONE VIA ROLE CRUD API*
#### 1. CREATE USER ACCOUNT
EXAMPLE POST endpoint:
http://127.0.0.1:8080/rest/public/gateway/user </br>
**Request body**: Pass in both the "username" and "password" within a json object in the format of the below example. </br>
*Example Body Format*
```
{
   "username" : "example@gmail.com",
   "password" : "mypassword
}
```
#### 2. READ USER ACCOUNT
EXAMPLE GET endpoint:
http://127.0.0.1:8080/rest/public/gateway/user?uname=_____ </br>
**The UNAME query parameter (optional)**: Pass in a specific User Account you would like to retrieve information from. If this
query parameter is left out, will retrieve information for all user accounts. </br>
#### 3. UPDATE USER ACCOUNT
EXAMPLE PUT endpoint:
http://127.0.0.1:8080/rest/public/gateway/user?uname=_____ </br>
**The UNAME query parameter**: Pass in User Account name that you would like to update with a new password </br>
**Request Body**: Within a json object, using key "password", pass in your desired new password. </br>
*Example Body Format*
```
{
  "password": "my new password"
}
```
#### 4. DELETE USER ACCOUNT
EXAMPLE DELETE endpoint:
http://127.0.0.1:8080/rest/public/gateway/user?uname=_____ </br>
**The UNAME query parameter**: Pass in a specific User Account you would like to delete </br>

---
### Role Crud API 
This API allows someone with an Admin role, to create new roles (will save in DB), delete roles, update existing roles with new permissions, or assign roles to specific service accounts or user accounts(identified by name).
#### 1. CREATE ROLE
EXAMPLE POST endpoint:
http://127.0.0.1:8080/rest/public/gateway/role </br>
**Request Body **: Name of the new role you want to add. Please set value to "rname" key inside of Json Object, within the POST request body. 
#### 2. DELETE ROLE
EXAMPLE DELETE endpoint:
http://127.0.0.1:8080/rest/public/gateway/role?rname=______&name=______&type=_______ </br>
**rname query param**: Name of role you want to delete from DB.
**name query param (optional)**: Specific User Account or Service Account you want to delete this role from
**type query param (needed if passing in name query)**: Pass in "service" to specify for Service Accounts, any other string will count as User Account.
#### 3. ASSIGN ROLE
EXAMPLE GET endpoint:
http://127.0.0.1:8080/rest/public/gateway/role/assign?rname=_______&name=______&type=______ </br>
**rname**: Role name to assign  <br/>
**name**: Either service account name or user's username <br/>
**type**: service or user. <br/> **Must put "service" to identify it is a service account. Else it will always look in user accounts.**
#### 4. UPDATE ROLE
EXAMPLE PUT endpoint:
http://127.0.0.1:8080/rest/public/gateway/role?rname=_______ </br>
**rname**: Role name to update permissions on. All previous permissions will be replaced for this role. <br/>
**Request body**: Please pass in a JSON array with the permission names. The whole array of permissions will be set to this role. If you specify permissions that do not already exist in the Permission table, these will be ignored.  <br/>

---
