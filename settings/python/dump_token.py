import psycopg2
import sys

def main():
	#we can find the microservice name within a configured ENV variable. Each microservice will configure this with their own name. Docker-compose? or nginx?
	if(len(sys.argv) < 2):
		print("Please enter in your service name")
		return
	try:
		conn_string = "host='localhost' port=35073'' dbname='account' user='jetty' password= 'jettypass'"
		conn = psycopg2.connect(conn_string)
		cur = conn.cursor()
		try:
			cur.execute("""SELECT servicetoken from Services WHERE servicename='%s'""" % sys.argv[1])
			res = cur.fetchone()
			print(res[0])
		except:
			print("Error finding service, has a service account been created yet?")
	except psycopg2.Error as e:
		print(e)
 
	
if __name__ == "__main__":
    main()
