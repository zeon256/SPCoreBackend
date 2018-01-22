package routes.authentication

import com.github.kittinunf.fuel.httpPost
import database.AuthSource
import exceptions.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.ValuesMap
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import models.User

fun Route.auth(path: String) = route("$path/auth") {
    post("/login") {
        val form = call.receive<ValuesMap>()
        val isAuth = runBlocking { validateWithSpice(form) }

        when (isAuth) {
            2 -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Locked out due to too many attempts",
                    LOCKED_OUT_BY_SP))
            3 -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Wrong Spice Credentials",
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
suspend fun validateWithSpice(form: ValuesMap): Int {
    val url = "https://mobileweb.sp.edu.sg/pkmslogin.form"
    var isAuth = 0
    url.httpPost(listOf(

            "username" to form["adminNo"],
            "password" to form["password"],
            "login-form-type" to "pwd"
    )).responseString {
        request, response, result ->
        isAuth = when {
            response.headers.containsKey("Set-Cookie") -> 1
            response.toString().contains("locked out") -> 2
            else -> 3
        }
    }

    while (isAuth == 0) {
        delay(200)
    }

    return isAuth
}
