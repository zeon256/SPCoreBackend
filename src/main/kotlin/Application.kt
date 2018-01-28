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

    /*Timer().schedule(timerTask { /*
        call function that has to be called every 15min
        1. check lessons that are starting in 15min time which returns adminNo and Lesson object
        2. Http POST to google's server to send notifications with arrayList of deviceId for user &
        the lessonObject
        3. Spawn the same number of coroutines of the number of lessons available
        Initial starting time should be 7.45am on a Monday but for testing this can be faked


        */ }, 900000) //15min delay*/

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