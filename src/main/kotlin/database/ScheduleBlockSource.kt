package database

import io.ktor.util.toLocalDateTime
import models.Filter
import models.TimeTable
import models.User
import java.sql.SQLException
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayList

class ScheduleBlockSource{
    private fun filterRegistration(user: User):Int {
        // register user for filtering to prevent spamming of SP server
        val sql = "INSERT ignore INTO filter values (?,?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.adminNo)
            ps.setInt(2,0)
            ps.setInt(3,5)
            ps.setLong(4,Instant.now().toEpochMilli())
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            rs
        }catch (e: SQLException){
            e.printStackTrace()
            0
        }
    }

    private fun getFilter(user: User): Filter?{
        val sql = "SELECT * FROM filter WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.adminNo)
            val rs = ps.executeQuery()

            var filter: Filter? = null
            if(rs.next())
                filter = Filter(rs.getString("adminNo"),
                    rs.getInt("queries"),
                    rs.getInt("cap"),
                    rs.getLong("updatedAt"))

            rs.close()
            ps.close()
            conn.close()

            filter
        }catch (e: SQLException){
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
    fun checkFilter(user:User): Int{
        val filter = getFilter(user)
        when(filter){
            null -> {
                val rs=  filterRegistration(user)
                val newFilter = getFilter(user)
                if(newFilter != null)
                    return updateFilter(newFilter,user)
            }
            else -> return updateFilter(filter,user)
        }
        return 0
    }

    private fun updateFilter(filter:Filter,user:User): Int{
        val sql = "UPDATE filter set queries = ?, cap = ?, updatedAt = ? WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            val lastUpdatedMonth = Date(filter.updatedAt).toLocalDateTime().monthValue
            val currentDateMonth = Date().toLocalDateTime().monthValue
            var newCap = filter.cap
            val afterQuery = filter.queries + 1

            // if person reaches
            if(filter.queries >= filter.cap)
                return -1

            if(currentDateMonth > lastUpdatedMonth){
                // update the cap
                newCap = filter.cap + 5
            }

            // logic for filtration
            // eg. Current month is january,
            // if they query and the queries is the same
            // as cap, they can query anymore
            // else they can keep on querying till they reach a cap
            // the cap + 5 if they query the in following month

            ps.setInt(1,afterQuery)
            ps.setInt(2,newCap)
            ps.setLong(3,Date().toInstant().toEpochMilli())
            ps.setString(4,user.adminNo)
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            return rs
        }catch (e: SQLException){
            e.printStackTrace()
            0
        }
    }


    suspend fun getLessons(user:User): ArrayList<TimeTable.Lesson>{
        val sql = "SELECT * FROM lesson WHERE adminNo = ?"
        val finalRes = ArrayList<TimeTable.Lesson>()
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.adminNo)
            val  rs = ps.executeQuery()

            while(rs.next()) {
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
        }catch (e:SQLException){
            e.printStackTrace()
            finalRes
        }
    }

    suspend fun insertLessons(lesson:TimeTable.Lesson,user: User): Int{
        val sql = "INSERT IGNORE INTO lesson values (?,?,?,?,?,?,?,?)"

        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)

            ps.setString(1,lesson.id)
            ps.setString(2,lesson.moduleCode)
            ps.setString(3,lesson.moduleName)
            ps.setString(4,lesson.lessonType)
            ps.setString(5,lesson.location)
            ps.setLong(6,lesson.endTime)
            ps.setLong(7,lesson.startTime)
            ps.setString(8,user.adminNo)
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            rs
        }catch (e:SQLException){
            e.printStackTrace()
            0
        }
    }

    suspend fun getEvents(){

    }

    suspend fun createEvent(){

    }

    suspend fun updateEvent(){

    }

    suspend fun deleteEvent(){

    }
}