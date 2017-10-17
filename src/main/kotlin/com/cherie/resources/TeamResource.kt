package com.cherie.resources

import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.Response
import javax.ws.rs.core.MediaType.APPLICATION_JSON
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.Cookie

//import org.apache.shiro.subject.Subject

@Path("teams")
@Produces(APPLICATION_JSON)
class TeamResource{
    companion object {
        var teams = HashMap<String, Team>()
    }


    init{
        val games = ArrayList<Game>()
        games.add(Game("7:00PM", "ClevelandCavaliers", "2017-09-21", "Oakland", true))
        games.add(Game("12:00PM", "NewYorkKnicks", "2017-09-23", "NewYork", false))
        games.add(Game("6:00PM", "Houston Rockets", "2017-09-29", "Oakland", true))
        teams.put("GSW", Team("GSW", 20, false, games))
    }

    /*fun isSessionValid(sessionId: String): Boolean {
        val requestedSubject = Subject.Builder().sessionId(sessionId).buildSubject()
        return requestedSubject.getSession(false) != null
    }*/

    @GET
    @Produces(APPLICATION_JSON)
    @Path("{teamname}")
    fun getTeam(@PathParam("teamname") teamname: String, @Context request: HttpServletRequest, @CookieParam("JSESSIONID") cookie: Cookie?): Response {
        /*if((cookie == null) || ((cookie != null) && isSessionValid(cookie.getValue()) != true))
        {
            //not valid we should redirect to login page
            //session was null or is no longer valid.
            return Response.temporaryRedirect(URI("http://127.0.0.1:8080/rest/login")).build()
        }*/

        if(teams.containsKey(teamname))
        {
            return Response.status(Status.OK).entity(teams[teamname]).build()
        }
        return Response.status(Status.NOT_FOUND).build()
    }


    @POST
    @Consumes(APPLICATION_JSON)
    @Path("{teamname}")
    fun createTeam(@PathParam("teamname") teamname:String, team: String): Response{
        println(teams.size)
        val mapper = ObjectMapper()
        mapper.registerModule(KotlinModule())
        val team1: Team = mapper.readValue(team)
        if(teams.containsKey(teamname))
        {
            return Response.status(Status.CONFLICT).type("text/plain").entity( "Sorry").build()
        }
        teams.put(teamname, team1)
        println(teams.containsKey(teamname))
        println(teams.size)
        return Response.status(Status.CREATED).entity(teams[teamname]).build()

    }

    /* @PUT
     @Path("{teamname}")
     fun updateTeam(@PathParam("teamname") teamname: String, team: com.cherie.resources.Team){
         teams.put(team.name, team)
     }*/

    @DELETE
    @Path("{teamname}")
    fun deleteTeam(@PathParam("teamname") teamname: String): Team?{
        return teams.remove(teamname)
    }

    //don't need to implement HEAD or OPTIONS because jersey automatically using GET


}

