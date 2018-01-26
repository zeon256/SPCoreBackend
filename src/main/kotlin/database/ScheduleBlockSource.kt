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
    private fun filterRegistration(user: User): Int {
        // register user for filtering to prevent spamming of SP server
        val sql = "INSERT IGNORE INTO filter VALUES (?,?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            ps.setInt(2, 0)
            ps.setInt(3, 5)
            ps.setLong(4, Instant.now().toEpochMilli())
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun getFilter(user: User): Filter? {
        val sql = "SELECT * FROM filter WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            val rs = ps.executeQuery()

            var filter: Filter? = null
            if (rs.next())
                filter = Filter(rs.getString("adminNo"),
                        rs.getInt("queries"),
                        rs.getInt("cap"),
                        rs.getLong("updatedAt"))

            rs.close()
            ps.close()
            conn.close()

            filter
        } catch (e: SQLException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * checkFilter checks if user already exist in the filter table
     * if user doesn't exist in filter table, user is inserted into
     * the table. Else it just updates the table by increasing the number of
     * queries by 1. If they are querying in a new month compared to the epoch
     * they will receive 5 free queries per month
     * checkFilter is the only method that should be called by the webservice
     * @param user
     */
    fun checkFilter(user: User): Int {
        val filter = getFilter(user)
        when (filter) {
            null -> {
                val rs = filterRegistration(user)
                val newFilter = getFilter(user)
                if (newFilter != null)
                    return updateFilter(newFilter, user)
            }
            else -> return updateFilter(filter, user)
        }
        return 0
    }

    private fun updateFilter(filter: Filter, user: User): Int {
        val sql = "UPDATE filter SET queries = ?, cap = ?, updatedAt = ? WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            val lastUpdatedMonth = Date(filter.updatedAt).toLocalDateTime().monthValue
            val currentDateMonth = Date().toLocalDateTime().monthValue
            var newCap = filter.cap
            val afterQuery = filter.queries + 1

            // if person reaches
            if (filter.queries >= filter.cap)
                return -1

            if (currentDateMonth > lastUpdatedMonth) {
                // update the cap
                newCap = filter.cap + 5
            }

            // logic for filtration
            // eg. Current month is january,
            // if they query and the queries is the same
            // as cap, they can query anymore
            // else they can keep on querying till they reach a cap
            // the cap + 5 if they query the in following month

            ps.setInt(1, afterQuery)
            ps.setInt(2, newCap)
            ps.setLong(3, Date().toInstant().toEpochMilli())
            ps.setString(4, user.adminNo)
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            return rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    suspend fun getLessons(user: User, startTimestamp: Long, endTimestamp: Long): ArrayList<TimeTable.Lesson> {
        val sql = "SELECT id,moduleCode,moduleName,lessonType,location,endTime,startTime\n" +
                "FROM lesson " +
                "JOIN lessonstudents l ON lesson.id = l.lessonId " +
                "WHERE l.adminNo = ? AND" +
                "lesson.startTime >= startTimestamp" +
                "lesson.endTime <= endTimestamp"

        val finalRes = ArrayList<TimeTable.Lesson>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
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

    suspend fun getEvents(adminNo: String): ArrayList<Event> {
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

    suspend fun getMyCreatedEvents(user: User): ArrayList<Event> {
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

    suspend fun getEvent(eventId: String): Event? {
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

    //crud events
    suspend fun createEvent(event: Event): Int {
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

    suspend fun updateEvent(event: Event): Int {
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

    suspend fun deleteEvent(eventId: String): Int {
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

    // event attendance
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

    suspend fun createAttendance(user: User, eventId: String, response: String): Boolean {
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

            //ps.setString(1,table)
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
    suspend fun invitePeople(eventId: String, invitedGuest: User): Int {
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
}