package database

import exceptions.DuplicateFound
import models.User
import java.sql.SQLException

class AuthSource {
    fun registerUser(user: User): Int {
        val sql = "INSERT INTO user VALUES (?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            ps.setNullIfNull(2,user.userName)
            ps.setNullIfNull(3, user.displayName)
            val rs = ps.executeUpdate()
            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    fun updateUser(user:User): Int {
        val sql = "UPDATE user SET username = ?, displayName = ? WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.userName)
            ps.setString(2,user.displayName)
            ps.setNullIfNull(3,user.adminNo)
            val rs = ps.executeUpdate()
            ps.close()
            conn.close()

            rs
        }catch (e:SQLException){
            if(e.toString().contains("Duplicate"))
                throw DuplicateFound("Duplicate username")
            0
        }
    }

    fun isUserExist(adminNo: String): Boolean {
        val sql = "SELECT * FROM user WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, adminNo)
            val rs = ps.executeQuery()
            val result = rs.next()

            ps.close()
            rs.close()
            conn.close()

            result
        }catch (e:SQLException){
            e.printStackTrace()
            false
        }
    }

    fun getUserById(adminNo: String): User? {
        val sql = "SELECT * FROM user WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, adminNo)
            val rs = ps.executeQuery()
            var user:User? = null
            if(rs.next())
                user = rs.toUser()

            ps.close()
            rs.close()
            conn.close()

            user
        }catch (e:SQLException){
            e.printStackTrace()
            null
        }
    }

    fun insertDeviceId(adminNo: String,deviceId: String): Int {
        val sql = "INSERT INTO userdevice values (?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,adminNo)
            ps.setString(2,deviceId)
            val rs = ps.executeUpdate()
            ps.close()
            conn.close()

            rs
        }catch (e:SQLException){
            e.printStackTrace()
            if(e.toString().contains("Duplicate"))
                return -1

            0
        }
    }

    fun getUserDevices(adminNo: String): ArrayList<String> {
        val finalRes = ArrayList<String>()
        val sql = "SELECT * FROM userdevice WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,adminNo)

            val rs = ps.executeQuery()
            while(rs.next()){
                finalRes.add(rs.getString("deviceId"))
            }


            finalRes
        }catch (e:SQLException){
            e.printStackTrace()
            finalRes
        }

    }
}