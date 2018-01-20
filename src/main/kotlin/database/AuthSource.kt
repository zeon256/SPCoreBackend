package database

import exceptions.DuplicateFound
import models.User
import java.sql.SQLException

class AuthSource {
    suspend fun registerUser(user: User): Int {
        val sql = "INSERT INTO user VALUES (?,?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            ps.setNullIfNull(2,user.userName)
            ps.setNullIfNull(3, user.fullName)
            ps.setNullIfNull(4,user.pp)
            val rs = ps.executeUpdate()
            rs
        } catch (e: SQLException) {
            e.printStackTrace()
            0
        }
    }

    suspend fun updateUser(user:User): Int {
        val sql = "UPDATE user SET username = ?, fullname = ?, pp = ? WHERE adminNo = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.userName)
            ps.setString(2,user.fullName)
            ps.setString(3,user.pp)
            ps.setNullIfNull(4,user.adminNo)
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

    suspend fun isUserExist(adminNo: String): Boolean {
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

    suspend fun getUserById(adminNo: String): User? {
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
}