CREATE TABLE IF NOT EXISTS Users (id serial PRIMARY KEY, username VARCHAR(50) NOT NULL UNIQUE, pwd VARCHAR(50));

CREATE TABLE IF NOT EXISTS Roles (id serial PRIMARY KEY, rolename VARCHAR(50) NOT NULL UNIQUE);

CREATE TABLE IF NOT EXISTS UserRole (uid INT references Users(id) ON DELETE CASCADE, roleid INT references Roles(id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS Permissions (id serial PRIMARY KEY, operation VARCHAR(50) NOT NULL UNIQUE);

CREATE TABLE IF NOT EXISTS RolePerm (pid INT references Permissions(id) ON DELETE CASCADE, roleid INT references Roles(id) ON DELETE CASCADE);

CREATE TABLE IF NOT EXISTS Services (id serial PRIMARY KEY, servicename VARCHAR(50) NOT NULL UNIQUE, servicetoken text, publickey text, tempsecret VARCHAR(20));

CREATE TABLE IF NOT EXISTS ServiceRole (sid INT references Services(id) ON DELETE CASCADE, roleid INT references Roles(id) ON DELETE CASCADE);


-- User Roles
INSERT INTO Roles(rolename) VALUES ('guest'), ('admin'), ('sessionOperator'), ('user'); --can add more operators later


INSERT INTO Permissions(operation) VALUES ('user:read'), ('user:modify'), ('device:read'),('device:modify')
 , ('policy:read'),('policy:modify'), ('session:read'), ('session:modify'), ('demo:read'), ('demo:modify')  ;
 --modify (create, update, delete)

INSERT INTO Users(username, pwd) VALUES ('admin@gmail.com', 'j');

WITH superad AS (
   SELECT id FROM Users WHERE username = 'admin@gmail.com'
), r AS(
   SELECT id FROM Roles where rolename = 'admin'
) INSERT INTO UserRole(uid, roleid) SELECT superad.id, r.id from superad, r;



WITH u1 AS (
   SELECT id FROM Permissions WHERE operation = 'session:read'
), r AS (
   SELECT id FROM Roles WHERE rolename = 'sessionOperator'
)
INSERT INTO RolePerm(pid, roleid) SELECT u1.id, r.id from u1, r; --sessionOperator session:read


WITH u1 AS (
   SELECT id FROM Permissions WHERE operation = 'session:modify'
), r AS (
   SELECT id FROM Roles WHERE rolename = 'sessionOperator'
)
INSERT INTO RolePerm(pid, roleid) SELECT u1.id, r.id from u1, r; --sessionOperator session:modify

WITH u1 AS (
   SELECT id FROM Permissions WHERE operation = 'session:read'
), r AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
)
INSERT INTO RolePerm(pid, roleid) SELECT u1.id, r.id from u1, r; --admin session:read

WITH u1 AS (
   SELECT id FROM Permissions WHERE operation = 'session:modify'
), r AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
)
INSERT INTO RolePerm(pid, roleid) SELECT u1.id, r.id from u1, r; --admin session:modify

WITH u2 AS (
   SELECT id FROM Permissions WHERE operation = 'user:modify'
), r AS (
   SELECT id FROM Permissions WHERE operation = 'admin')
INSERT INTO RolePerm(pid, roleid) SELECT u2.id, r.id from u2, r; --admin user:modify

WITH u3 AS (
   SELECT id FROM Permissions WHERE operation = 'user:read'
), r AS (
   SELECT id FROM Permissions WHERE operation = 'admin')
INSERT INTO RolePerm(pid, roleid) SELECT u3.id, r.id from u3, r; --admin user:read

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), d1 AS (
   SELECT id FROM Permissions WHERE operation = 'device:read'
)
-- admin has device crud permissions
INSERT INTO RolePerm(pid, roleid) SELECT d1.id, ad.id from d1, ad; --admin device:read

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), d2 AS (
   SELECT id FROM Permissions WHERE operation = 'device:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT d2.id, ad.id from d2, ad; --admin device:modify


WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), p1 AS (
   SELECT id FROM Permissions WHERE operation = 'policy:read'
)INSERT INTO RolePerm(pid, roleid) SELECT p1.id, ad.id from p1, ad; --admin policy:read

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, p2 AS (
   SELECT id FROM Permissions WHERE operation = 'policy:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT p2.id, ad.id from p2, ad; --admin policy:modify

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'user'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'demo:read'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu;   --user demo:read

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'user'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'demo:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --user demo:modify

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'guest'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'demo:read'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --guest demo:read

