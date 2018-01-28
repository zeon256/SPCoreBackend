import firebase.Firebase
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import routes.authentication.auth
import routes.authentication.jwtAuth
import routes.event
import routes.friend
import java.util.*
import kotlin.concurrent.timerTask

fun main(args: Array<String>) {
    startServer()
}

private fun startServer() = embeddedServer(Netty, 8080) {
    install(ContentNegotiation) {
        gson { setPrettyPrinting() }
    }
    //notifications
    Timer().schedule(Firebase(),Date(1517183100))

    val path = "/api/dev"
    routing {
        intercept(ApplicationCallPipeline.Infrastructure) {
            jwtAuth()
        }

        auth(path)
        friend(path)
        event(path)
    }


}.start(wait = true)