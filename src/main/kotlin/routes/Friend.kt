package routes

import database.FriendSource
import database.Utils
import exceptions.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.ValuesMap
import routes.authentication.requireLogin
import java.sql.SQLException

fun Route.friend(path: String) = route("$path/friends") {
    get {
        val user = requireLogin()
        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val fs = FriendSource()
                    call.respond(fs.getFriends(user))
                }catch (e:SQLException){
                    call.respond(
                            ErrorMsg("Database Error", DATABASE_ERROR))
                }
            }
        }
    }

    get("/request"){
        val user = requireLogin()
        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val fs = FriendSource()
                    val fRequest = fs.getFriendRequests(user)

                    call.respond(fRequest)
                }catch (e:SQLException){
                    call.respond(
                            ErrorMsg("Database Error", DATABASE_ERROR))
                }
            }
        }
    }

    post("/request") {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        when(user) {
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val fs = FriendSource()
                    val userExistence = fs.getUserByUsername(form["username"].toString())
                    when(userExistence){
                        null -> call.respond("User does not exist!")
                        else -> {
                            val hasInserted = fs.insertFriendRequest(user,userExistence.adminNo)
                            if(!hasInserted)
                                call.respond(
                                        ErrorMsg("Cannot make friend request to ${userExistence.userName}", ALREADY_FRIENDS))
                            else
                                call.respond("Successfully requested to add ${userExistence.userName}")
                        }
                    }
                }catch (e:SQLException){
                    call.respond(
                            ErrorMsg("Database Error", DATABASE_ERROR))
                }catch (e:AlreadyFriends){
                    call.respond(
                            ErrorMsg("Already friends", ALREADY_FRIENDS))
                }catch (e:CannotAddSelfAsFriend){
                    call.respond(
                            ErrorMsg("Cannot add yourself as friend", CANNOT_ADD_SELF_AS_FRIEND))
                }catch (e:PersonAlreadySentRequest){
                    call.respond(
                            ErrorMsg("The other party already sent you a friend request", OTHER_PARTY_ALREADY_SENT_REQ))
                }
            }
        }
    }

    post("/requestState"){
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val isAccepting = form["isAccepting"]?.toInt()
                    val requestId = form["requestId"].toString()

                    val fs = FriendSource()
                    var result = false
                    var resString = ""

                    when(isAccepting){
                        null -> call.respond(HttpStatusCode.BadRequest,ErrorMsg("Invalid isAccepting value", BAD_REQUEST))
                        0 -> {
                            resString = "declined"
                            result = fs.declineFriendRequest(requestId)
                        }
                        1 -> {
                            resString = "accepted"
                            result = fs.acceptFriendRequest(user,requestId)
                        }
                    }

                    if(!result)
                        call.respond(
                                ErrorMsg("Database Error", DATABASE_ERROR))
                    else
                        call.respond("$resString friend")

                }catch (e:SQLException){
                    call.respond(
                            ErrorMsg("Database Error", DATABASE_ERROR))
                }
            }
        }
    }

    delete {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val username = form["username"].toString()
                    val fs = FriendSource()
                    val result = fs.removeFriend(user,username)
                    when(result){
                        false -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("$username is not a friend", NOT_FRIENDS))
                        true -> call.respond("Successfully removed $username as friend")
                    }

                }catch (e:SQLException){
                    call.respond(
                            ErrorMsg("Database Error", DATABASE_ERROR))
                }
            }
        }
    }
}