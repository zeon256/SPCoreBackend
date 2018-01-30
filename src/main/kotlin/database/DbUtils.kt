package database

import firebase.Firebase
import models.Event
import models.FriendRequest
import models.TimeTable
import models.User
import java.security.MessageDigest
import java.sql.PreparedStatement
import java.sql.ResultSet


class Utils {
    companion object {
        fun sha512(input: String) = hashString("SHA-512", input)
        fun sha256(input: String) = hashString("SHA-256", input)
        fun sha1(input: String) = hashString("SHA-1", input)
        fun md5(input: String) = hashString("md5", input)

        /**
         * You can add on more hashing algorithm @ DbUtils.kt
         * @param hashAlgo
         * @param input
         * @return hashedString
         */
        private fun hashString(hashAlgo: String, input: String) =
                MessageDigest.getInstance(hashAlgo)
                        .digest(input.toByteArray()).joinToString(separator = "") {
                    String.format("%02X", it).toLowerCase()
                }
    }
}


fun <T> PreparedStatement.setNullIfNull(parameterIndex: Int, t: T) {
    when (t) {
        null -> this.setNull(parameterIndex, java.sql.Types.NULL)
        is String -> {
            if (t.isBlank())
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

fun ResultSet.toEvent(): Event {
    val source = ScheduleBlockSource()
    val eventId = this.getString("id")

    return Event(eventId,
            this.getString("title"),
            this.getString("location"),
            this.getLong("startTime"),
            this.getLong("endTime"),
            AuthSource().getUserById(this.getString("creatorId"))!!,
            source.getIsDeletedInvite(eventId),
            source.getIsGoing(eventId),
            source.getIsNotGoing(eventId),
            source.getHaventRespond(eventId))
}

fun ResultSet.toLesson(): TimeTable.Lesson = TimeTable.Lesson(id = this.getString("id"),
        moduleCode = this.getString("moduleCode"),
        moduleName = this.getString("moduleName"),
        lessonType = this.getString("lessonType"),
        location = this.getString("location"),
        endTime = this.getLong("endTime"),
        startTime = this.getLong("startTime"))

fun ResultSet.toLessonDevice(): Firebase.LessonDevice = Firebase.LessonDevice(
        lessonId = this.getString("lessonId"),
        adminNo = this.getString("adminNo"),
        deviceId = this.getString("deviceId")
)
