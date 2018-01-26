package database

import io.ktor.util.toLocalDateTime
import models.Event
import models.Filter
import models.TimeTable
import models.User
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

class ScheduleBlockSource {
    fun getLessons(user: User, startTimestamp: Long, endTimestamp: Long): ArrayList<TimeTable.Lesson> {

        val sql = "SELECT id,moduleCode,moduleName,lessonType,location,endTime,startTime\n" +
                "FROM lesson " +
                "JOIN lessonstudents l ON lesson.id = l.lessonId " +
                "WHERE l.adminNo = ? AND " +
                "lesson.startTime >= ? AND " +
                "lesson.endTime <= ?"

        val finalRes = ArrayList<TimeTable.Lesson>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            ps.setLong(2,startTimestamp)
            ps.setLong(3,endTimestamp)

            val rs = ps.executeQuery()

            while (rs.next()) {
                finalRes.add(TimeTable.Lesson(
                        rs.getString("id"),
                        rs.getString("moduleCode"),
                        rs.getString("moduleName"),
                        rs.getString("lessonType"),
                        rs.getString("location"),
                        rs.getLong("endTime"),
                        rs.getLong("startTime")
                ))
            }
            finalRes
        } catch (e: SQLException) {
            e.printStackTrace()
            finalRes
        }
    }

    fun insertLessons(lesson: TimeTable.Lesson, user: User): Boolean {
        val sql = "INSERT IGNORE INTO lesson VALUES (?,?,?,?,?,?,?)"

        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            ps.setString(1, lesson.id)
            ps.setString(2, lesson.moduleCode)
            ps.setString(3, lesson.moduleName)
            ps.setString(4, lesson.lessonType)
            ps.setString(5, lesson.location)
            ps.setLong(6, lesson.endTime)
            ps.setLong(7, lesson.startTime)
            val rs = ps.executeUpdate()

            conn.close()
            ps.close()
            var rs2 = 0

            rs2 = insertLessonStudent(lesson.id, user.adminNo)

            (rs + rs2 == 2)
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    private fun insertLessonStudent(lessonId: String, adminNo: String): Int {
        val sql = "INSERT INTO lessonstudents VALUES (?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, lessonId)
            ps.setString(2, adminNo)
            val rs = ps.executeUpdate()
            conn.close()
            ps.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getEvents(adminNo: String): ArrayList<Event> {
        val sql = "SELECT id,title,location,startTime,endTime,creatorId " +
                "FROM event " +
                "LEFT JOIN eventgoing going on event.id = going.eventId " +
                "LEFT JOIN eventhaventrespond haventres on event.id = haventres.eventId " +
                "LEFT JOIN eventnotgoing notgoing on event.id = notgoing.eventId " +
                "WHERE notgoing.adminNo = ? OR going.adminNo = ? OR haventres.adminNo = ?"
        val events = ArrayList<Event>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, adminNo)
            ps.setString(2, adminNo)
            ps.setString(3, adminNo)
            val rs = ps.executeQuery()
            while (rs.next()) {
                events.add(rs.toEvent())
            }

            events
        } catch (e: SQLException) {
            e.printStackTrace()
            events
        }
    }

    fun getMyCreatedEvents(user: User): ArrayList<Event> {
        val sql = "SELECT * FROM event WHERE creatorId = ?"
        val events = ArrayList<Event>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            val rs = ps.executeQuery()
            while (rs.next()) {
                events.add(rs.toEvent())
            }

            events
        } catch (e: SQLException) {
            e.printStackTrace()
            events
        }
    }

    fun getEvent(eventId: String): Event? {
        val sql = "SELECT * FROM event WHERE id = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)

            val rs = ps.executeQuery()
            var event: Event? = null

            if (rs.next())
                event = rs.toEvent()

            return event

        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    fun createEvent(event: Event): Int {
        val sql = "INSERT INTO event VALUES (?,?,?,?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, event.id)
            ps.setString(2, event.title)
            ps.setString(3, event.location)
            ps.setLong(4, event.startTime)
            ps.setLong(5, event.endTime)
            ps.setString(6, event.creator.adminNo)
            val rs = ps.executeUpdate()

            if (rs >= 1)
                invitePeople(event.id, event.creator)

            ps.close()
            conn.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }

    }

    fun updateEvent(event: Event): Int {
        val sql = "UPDATE event SET " +
                "title = ?, " +
                "location = ?, " +
                "startTime = ?, " +
                "endTime = ? " +
                "WHERE id = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, event.title)
            ps.setString(2, event.location)
            ps.setLong(3, event.startTime)
            ps.setLong(4, event.endTime)
            ps.setString(5, event.id)

            val rs = ps.executeUpdate()

            conn.close()
            ps.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun deleteEvent(eventId: String): Int {
        val sql = "DELETE FROM event WHERE id = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)
            val rs = ps.executeUpdate()

            conn.close()
            ps.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getIsGoing(eventId: String): ArrayList<User> {
        val sql = "SELECT * FROM eventgoing WHERE eventId = ?"
        val users = ArrayList<User>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)
            val rs = ps.executeQuery()
            while (rs.next()) {
                AuthSource().getUserById(rs.getString("adminNo"))?.let { users.add(it) }
            }
            users
        } catch (e: SQLException) {
            e.printStackTrace()
            users
        }
    }

    fun getIsNotGoing(eventId: String): ArrayList<User> {
        val sql = "SELECT * FROM eventnotgoing WHERE eventId= ?"
        val users = ArrayList<User>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)
            val rs = ps.executeQuery()
            while (rs.next()) {
                users.add(AuthSource().getUserById(rs.getString("adminNo"))!!)
            }
            users
        } catch (e: SQLException) {
            e.printStackTrace()
            users
        }
    }

    fun getIsDeletedInvite(eventId: String): ArrayList<User> {
        val sql = "SELECT * FROM eventdeletedinvite WHERE eventId= ?"
        val users = ArrayList<User>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)
            val rs = ps.executeQuery()
            while (rs.next()) {
                users.add(AuthSource().getUserById(rs.getString("adminNo"))!!)
            }
            users
        } catch (e: SQLException) {
            e.printStackTrace()
            users
        }
    }

    fun getHaventRespond(eventId: String): ArrayList<User> {
        val sql = "SELECT * FROM eventhaventrespond WHERE eventId= ?"
        val users = ArrayList<User>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, eventId)
            val rs = ps.executeQuery()
            while (rs.next()) {
                users.add(AuthSource().getUserById(rs.getString("adminNo"))!!)
            }
            users
        } catch (e: SQLException) {
            e.printStackTrace()
            users
        }
    }

    /**
     * @param user          User Object
     * @param eventId       EventId
     * @param response      User response for the particular event.
     *                      -> -1 delete invite
     *                      -> 0 not going
     *                      -> 1 going
     *
     * @return true         if successfully inserted into table & removed from previous table
     *         false        if fail ANY operations
     *
     */
    fun createAttendance(user: User, eventId: String, response: String): Boolean {
        var table = ""
        val event = getEvent(eventId)
        table = when (response) {
            "0" -> "eventnotgoing"
            "1" -> "eventgoing"
            "-1" -> "eventdeleteinvite"
            else -> "" // this will cause SQLException
        }
        val sql = "INSERT INTO $table VALUES (?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            ps.setString(1,eventId)
            ps.setString(2, user.adminNo)
            val rs = ps.executeUpdate()
            var rs2 = -2

            if (rs == 1) {
                // check which attendance user is currently is
                // eg isGoing but changed to notGoing
                // then user has to be moved from eventgoing table
                // to eventnotgoing

                if (event != null) {
                    rs2 = when {
                        event.haventRespond.firstOrNull { it.adminNo == user.adminNo } != null ->
                            removeFromAttendanceTable(user, event.id, "eventhaventrespond")
                        event.notGoing.firstOrNull { it.adminNo == user.adminNo } != null ->
                            removeFromAttendanceTable(user, event.id, "eventnotgoing")
                        else -> removeFromAttendanceTable(user, event.id, "eventgoing")
                    }
                }
            }

            (rs + rs2 == 2)
        } catch (e: SQLException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Users are now allowed to be moved from going/notgoing/haventrespond
     */
    private fun removeFromAttendanceTable(user: User, eventId: String, table: String): Int {
        val sql = "DELETE FROM $table WHERE eventId = ? AND adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            //ps.setString(1, table)
            ps.setString(1, eventId)
            ps.setString(2, user.adminNo)
            val rs = ps.executeUpdate()

            conn.close()
            ps.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    /**
     * When inviting people, they will be added to the "eventhaventRespond" table
     * Once they submit a POST request for going/notGoing/deletedInvite,
     * those will remove the user that is in the "eventhaventRespond" table
     * @param user
     * @throws SQLException
     */
    fun invitePeople(eventId: String, invitedGuest: User): Int {
        val sql = "INSERT INTO eventhaventrespond VALUES (?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            ps.setString(1, eventId)
            ps.setString(2, invitedGuest.adminNo)
            val rs = ps.executeUpdate()
            ps.close()
            conn.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }


    // Notifications stuff

    /**
     * Gets lessons that are starting in 15min
     * 15min = 900000ms
     * WHERE clause, take
     * @param currentTime       in Epoch ms
     */
    fun checkLessonStartTimeAll(currentTime:Long): ArrayList<TimeTable.Lesson>{
        val sql = "SELECT * FROM lesson WHERE startTime = ?"
        val lessonStartTime = currentTime + 900000L
        val lessonsIn15Min = ArrayList<TimeTable.Lesson>()

        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setLong(1,lessonStartTime)

            val rs = ps.executeQuery()
            while (rs.next()){
                lessonsIn15Min.add(rs.toLesson())
            }

            lessonsIn15Min
        }catch (e:SQLException){
            e.printStackTrace()
            lessonsIn15Min
        }

    }

    fun getLessonStudentsByLessonId(lessonId: String): ArrayList<String>{
        val finalRes = ArrayList<String>()
        val sql = "SELECT ls.lessonId, ls.adminNo,ud.deviceId FROM lessonstudents ls \n" +
                "RIGHT JOIN userdevice ud ON  ls.adminNo = ud.adminNo \n" +
                "WHERE lessonId = ?"

        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,lessonId)
            val rs = ps.executeQuery()
            while (rs.next()){

            }

            finalRes
        }catch (e:SQLException){
            e.printStackTrace()
            finalRes
        }



    }


}

fun main(args: Array<String>) {
    val src = ScheduleBlockSource()
    val lessons = src.checkLessonStartTimeAll(1516941900000)
    println(lessons)
}