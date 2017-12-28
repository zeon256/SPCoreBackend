package routes.authentication

import com.github.kittinunf.fuel.Fuel
import database.AuthSource
import exceptions.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.ValuesMap
import models.User

fun Route.auth(path: String) = route("$path/auth") {
    post("/login") {
        val form = call.receive<ValuesMap>()
        val isAuth = try {
            validateWithSpice(form)
        } catch (e: WrongSpiceCredentials) {
            call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Wrong Spice Credentials",
                    WRONG_SPICE_CRENDENTIALS))
            null
        } catch (e: LockedOutBySP) {
            call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Wrong Spice Credentials",
                    LOCKED_OUT_BY_SP))
            null
        }

        when (isAuth) {
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Wrong Spice Credentials",
                    WRONG_SPICE_CRENDENTIALS))
            else -> {
                val isUserExist = AuthSource().isUserExist(form["adminNo"].toString())
                if (!isUserExist) {
                    val hasRegistered = AuthSource().registerUser(
                            User(form["adminNo"].toString(), null, null))
                    if (hasRegistered == 1){
                        val jwt = AuthSource().getUserById(form["adminNo"].toString())?.let(JwtConfig::makeToken)
                        if(jwt != null)
                            call.respond(JwtObjForFrontEnd(jwt))
                    }

                }else{
                    val jwt = AuthSource().getUserById(form["adminNo"].toString())?.let(JwtConfig::makeToken)
                    if(jwt != null)
                        call.respond(JwtObjForFrontEnd(jwt))
                }
            }
        }

    }
    /**
     * TODO("Change to multipart to accept images")
     */
    put("/updateUser") {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        when (user) {
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val hasUpdated = AuthSource().updateUser(
                            User(user.adminNo,
                                    form["username"].toString(),
                                    form["fullname"].toString()
                            ))
                    if(hasUpdated == 1)
                        call.respond("${user.adminNo} has been updated!")

                }catch (e: DuplicateFound){
                    call.respond(HttpStatusCode.BadRequest,ErrorMsg("Username already taken!", DUPLICATE_FOUND))
                }
            }
        }
    }

}

/**
 * Perform a HTTP POST to SP's server
 * @param form
 * @return Boolean isAuth
 * @throws
 */
fun validateWithSpice(form: ValuesMap): Boolean {
    val url = "https://mobileweb.sp.edu.sg/pkmslogin.form"
    var isAuth = false
    Fuel.Companion.post(url, listOf(
            "username" to form["adminNo"],
            "password" to form["password"],
            "login-form-type" to "pwd"
    )).response { _, response, _ ->
        run {
            val resString = response.toString()
            when {
                resString.contains("isWebSEALError") -> throw WrongSpiceCredentials("Incorrect Password")
                resString.contains("locked out") -> throw LockedOutBySP("Locked out")
                resString.contains("200") -> isAuth = true
            }

        }
    }
    return isAuth
}
