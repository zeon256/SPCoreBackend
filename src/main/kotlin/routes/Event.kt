package routes

import io.ktor.routing.Route
import io.ktor.routing.route

fun Route.event(path: String) = route("$path/auth") {

}