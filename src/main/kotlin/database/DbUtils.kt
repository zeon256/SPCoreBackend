package database

import models.FriendRequest
import models.User
import java.security.MessageDigest
import java.sql.PreparedStatement
import java.sql.ResultSet


class Utils {
    companion object {
        fun sha512(input: String) = hashString("SHA-512", input)
        fun sha256(input: String) = hashString("SHA-256", input)
        fun sha1(input: String) = hashString("SHA-1", input)
        fun md5(input: String) = hashString("md5",input)

        /**
         * You can add on more hashing algorithm @ DbUtils.kt
         * @param hashAlgo
         * @param input
         * @return hashedString
         */
        private fun hashString(hashAlgo: String, input: String) =
                MessageDigest.getInstance(hashAlgo)
                        .digest(input.toByteArray()).joinToString(separator = "") {
                    String.format("%02X", it).toLowerCase() }
    }
}


fun <T> PreparedStatement.setNullIfNull(parameterIndex: Int, t:T){
    when (t) {
        null -> this.setNull(parameterIndex, java.sql.Types.NULL)
        is String -> {
            if(t.isBlank())
                this.setString(parameterIndex, null)
            else
                this.setString(parameterIndex, t)
        }
        is Int -> this.setInt(parameterIndex, t)
        else -> throw NotImplementedError("This feature haven't been implemented")
    }
}

fun ResultSet.toUser() = User(
        this.getString("adminNo"),
        this.getString("username"),
        this.getString("displayName")
)

fun ResultSet.toFriendRequest() = FriendRequest(
        this.getString("requestId"),
        this.getString("requestee"),
        this.getString("receiver")
)