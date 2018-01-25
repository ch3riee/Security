#!/usr/bin/env bash
SERVER=db-session
PORT=5432
jar_name=rest-1.0-SNAPSHOT-all.jar
x=-1

#[ is a command equiv to test command. Testing that result is not equal to 0
while [[ $x -ne 0 ]]
do
    echo "Connection to $SERVER on port $PORT failed"
    timeout 2 bash -c "</dev/tcp/$SERVER/$PORT"; x=$?
done
echo "Connection to $SERVER on port $PORT succeeded"
sleep 3s
java -jar $jar_name
