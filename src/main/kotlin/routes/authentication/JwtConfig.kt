package routes.authentication

import database.AuthSource
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.pipeline.PipelineContext
import io.ktor.request.header
import models.User


object JwtConfig {
    private const val secret = "SPCoreNumbaWan"
    private const val issuer = "SPCore"

    fun parse(token: String): String = Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .body
            .let({ it["adminNo"].toString() })

    fun makeToken(user: User): String = Jwts.builder()
            .setSubject("Authentication")
            .setIssuer(issuer)
            .claim("adminNo", user.adminNo)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact()
}

data class JwtObjForFrontEnd(val token: String)

fun PipelineContext<Unit, ApplicationCall>.jwtAuth() {
    val token = call.request.header("Authorization")?.removePrefix("Bearer ") ?: return
    val userId = JwtConfig.parse(token)
    val user = AuthSource().getUserById(userId)
    if (user != null)
        call.attributes.put(User.key, user)
}


fun PipelineContext<*, ApplicationCall>.requireLogin(): User? = try {
    optionalLogin()
}catch (e:IllegalStateException){
    null
}

fun PipelineContext<*, ApplicationCall>.optionalLogin(): User? = call.attributes[User.key]