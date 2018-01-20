package database

import models.TimeTable
import java.sql.SQLException

class ScheduleBlockSource{
    suspend fun insertLessons(lesson:TimeTable.Lesson): Int{
        val sql = "INSERT IGNORE INTO lesson values (?,?,?,?,?,?,?)"
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
            val rs = ps.executeUpdate()
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