CREATE TABLE IF NOT EXISTS Users (id serial PRIMARY KEY, username VARCHAR(50) NOT NULL, pwd VARCHAR(50));

CREATE TABLE IF NOT EXISTS Roles (id serial PRIMARY KEY, rolename VARCHAR(50) NOT NULL);

CREATE TABLE IF NOT EXISTS UserRole (uid INT references Users(id), roleid INT references Roles(id));

CREATE TABLE IF NOT EXISTS Permissions (id serial PRIMARY KEY, operation VARCHAR(50) NOT NULL);

CREATE TABLE IF NOT EXISTS RolePerm (pid INT references Permissions(id), roleid INT references Roles(id));

INSERT INTO Roles(rolename) VALUES ('guest');

INSERT INTO Roles(rolename) VALUES ('admin');

INSERT INTO Roles(rolename) VALUES ('poweruser');

INSERT INTO Permissions(operation) VALUES ('user:create');

INSERT INTO Permissions(operation) VALUES ('user:modify');

INSERT INTO Permissions(operation) VALUES ('user:delete');

INSERT INTO Permissions(operation) VALUES ('device:create');

INSERT INTO Permissions(operation) VALUES ('device:modify');

INSERT INTO Permissions(operation) VALUES ('device:delete');

INSERT INTO Permissions(operation) VALUES ('policy:create');

INSERT INTO Permissions(operation) VALUES ('policy:modify');

INSERT INTO Permissions(operation) VALUES ('policy:delete');

WITH u1 AS (
   SELECT id FROM Permissions WHERE operation = 'user:create'
), guest AS (
   SELECT id FROM Roles WHERE rolename = 'guest'
)
INSERT INTO RolePerm(pid, roleid) SELECT u1.id, guest.id from u1, guest; --guest user:create

WITH u2 AS (
   SELECT id FROM Permissions WHERE operation = 'user:modify'
), guest AS (
   SELECT id FROM Permissions WHERE operation = 'guest')
INSERT INTO RolePerm(pid, roleid) SELECT u2.id, guest.id from u2, guest; --guest user:modify

WITH u3 AS (
   SELECT id FROM Permissions WHERE operation = 'user:delete'
), guest AS (
   SELECT id FROM Permissions WHERE operation = 'guest')
INSERT INTO RolePerm(pid, roleid) SELECT u3.id, guest.id from u3, guest; --guest user:delete

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), d1 AS (
   SELECT id FROM Permissions WHERE operation = 'device:create'
)
-- admin has device crud permissions
INSERT INTO RolePerm(pid, roleid) SELECT d1.id, ad.id from d1, ad; --admin device:create

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), d2 AS (
   SELECT id FROM Permissions WHERE operation = 'device:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT d2.id, ad.id from d2, ad; --admin device:modify

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), d3 AS (
   SELECT id FROM Permissions WHERE operation = 'device:delete'
)
INSERT INTO RolePerm(pid, roleid) SELECT d3.id, ad.id from d3, ad; --admin device:delete

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin'
), p1 AS (
   SELECT id FROM Permissions WHERE operation = 'policy:create'
)INSERT INTO RolePerm(pid, roleid) SELECT p1.id, ad.id from p1, ad; --admin policy:create

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, p2 AS (
   SELECT id FROM Permissions WHERE operation = 'policy:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT p2.id, ad.id from p2, ad; --admin policy:modify

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, p3 AS (
   SELECT id FROM Permissions WHERE operation = 'policy:delete'
)INSERT INTO RolePerm(pid, roleid) SELECT p3.id, ad.id from p3, ad; --admin policy:delete

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, u1 AS (
   SELECT id FROM Permissions WHERE operation = 'user:create'  --admin user:create
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, ad.id from u1, ad;

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, u2 AS (
   SELECT id FROM Permissions WHERE operation = 'user:modify'  --admin user:modify
)INSERT INTO RolePerm(pid, roleid) SELECT u2.id, ad.id from u2, ad;

WITH ad AS (
   SELECT id FROM Roles WHERE rolename = 'admin')
, u2 AS (
   SELECT id FROM Permissions WHERE operation = 'user:delete'  --admin user:delete
)INSERT INTO RolePerm(pid, roleid) SELECT u2.id, ad.id from u2, ad;



WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'user:create'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu;   --poweruser user:create

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'user:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --poweruser user:modify

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'user:delete'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --poweruser user:delete

WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'device:create'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --poweruser device:create


WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'device:modify'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --poweruser device:modify


WITH pu AS(
   SELECT id FROM Roles WHERE rolename = 'poweruser'
),u1 AS (
   SELECT id FROM Permissions WHERE operation = 'device:delete'
)INSERT INTO RolePerm(pid, roleid) SELECT u1.id, pu.id from u1, pu; --poweruser device:delete
