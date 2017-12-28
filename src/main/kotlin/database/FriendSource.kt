package database

import exceptions.AlreadyFriends
import exceptions.CannotAddSelfAsFriend
import exceptions.PersonAlreadySentRequest
import exceptions.UserDoesntExist
import models.FriendRequest
import models.Friends
import models.User
import java.sql.SQLException

class FriendSource {

    fun getFriends(user:User):Friends{
        val sql = "SELECT * FROM friend WHERE originNode = ? OR destNode = ?"
        val result = ArrayList<User>()
        try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,user.adminNo)
            ps.setString(2,user.adminNo)
            val rs = ps.executeQuery()
            while (rs.next()){
                val tempUser = if(rs.getString("originNode") == user.adminNo)
                    AuthSource().getUserById(rs.getString("destNode"))!!
                else
                    AuthSource().getUserById(rs.getString("originNode"))!!

                result.add(tempUser)
            }

            rs.close()
            ps.close()
            conn.close()

            return Friends(result)
        }catch (e:SQLException){
            return Friends(result)
        }
    }

    fun getUserByUsername(username:String): User? {
        val sql = "SELECT * FROM user WHERE username = ?"
        return try{
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,username)
            val rs = ps.executeQuery()

            val user = if(rs.next()) rs.toUser() else null
            rs.close()
            ps.close()
            conn.close()

            user
        }catch (e:SQLException){
            null
        }
    }

    /**
     * Insert Edge only happens when the other user accepts the friend request
     * If they do not accept the friend request, then this does not happen
     * @param user
     * @param _userToBeAdded
     * @return Boolean ofInsertedEdge
     * @throws SQLException
     */
    private fun insertEdge(user:User,requestId:String):Boolean {
        val requesteeAdminNo = getFriendRequestById(requestId)
        val sql = "INSERT INTO friend VALUES (?,?,?)"
        return when(requesteeAdminNo){
            null -> false
            else -> {
                try {
                    val conn = getDbConnection()
                    val ps = conn.prepareStatement(sql)
                    ps.setString(1,Utils.md5( requesteeAdminNo.requesteeId + user.adminNo))
                    ps.setString(2,requesteeAdminNo.requesteeId)
                    ps.setString(3,user.adminNo)
                    val rs = ps.executeUpdate()
                    ps.close()
                    conn.close()

                    rs != 0
                }catch (e:SQLException){
                    false
                }
            }
        }
    }

    fun insertFriendRequest(user: User, receiverId: String): Boolean {
        if(user.adminNo == receiverId)
            throw CannotAddSelfAsFriend("Cannot add self as friend")

        val alreadyFriends = checkEdgeExistence(
                Utils.md5(user.adminNo + receiverId),
                Utils.md5(receiverId + user.adminNo)
        )

        val requestAlreadyExist = checkRequestExistence(
                Utils.md5(user.adminNo + receiverId),
                Utils.md5(receiverId + user.adminNo)
        )

        if(alreadyFriends)
            throw AlreadyFriends("${user.adminNo} is already friends with $receiverId")

        if(requestAlreadyExist)
            throw PersonAlreadySentRequest("Person already sent you a request")


        val sql = "INSERT INTO friendrequest VALUES (?,?,?)"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,Utils.md5(user.adminNo + receiverId))
            ps.setString(2,user.adminNo)
            ps.setString(3,receiverId)
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            rs != 0
        }catch (e:SQLException){
            false
        }
    }

    fun getFriendRequests(user: User): ArrayList<String> {
        val sql = "SELECT * FROM friendrequest WHERE receiver = ?"
        val results = ArrayList<String>()
        try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1, user.adminNo)
            val rs = ps.executeQuery()
            while (rs.next()){
                results.add(rs.getString("requestId"))
            }
            rs.close()
            ps.close()
            conn.close()
        }catch (e:SQLException){
            return results
        }

        return results
    }

    private fun getFriendRequestById(requestId:String): FriendRequest? {
        val sql = "SELECT * FROM friendrequest WHERE requestId = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,requestId)
            val rs = ps.executeQuery()
            val result = if(rs.next()) rs.toFriendRequest() else null

            rs.close()
            ps.close()
            conn.close()

            result
        }catch (e:SQLException){
            null
        }
    }

    fun acceptFriendRequest(user: User,requestId:String):Boolean {
        val edgeInserted = insertEdge(user,requestId)
        val requestDeleted = deleteRequest(requestId)

        return edgeInserted && requestDeleted
    }

    fun declineFriendRequest(requestId: String) = deleteRequest(requestId)

    private fun deleteRequest(requestId: String): Boolean{
        val sql = "DELETE FROM friendrequest WHERE requestId = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,requestId)
            val rs = ps.executeUpdate()

            ps.close()
            conn.close()

            rs != 0
        }catch (e:SQLException){
            false
        }
    }

    /**
     * @param hashOfId is hash of requesteeId + receiverId
     * @param hashOfIdSwapped is just the opposite of hashOfId
     */
    private fun checkEdgeExistence(hashOfId:String, hashOfIdSwapped: String):Boolean {
        val sql = "SELECT * FROM friend WHERE edgeId != ? OR edgeId != ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,hashOfId)
            ps.setString(2,hashOfIdSwapped)
            val rs = ps.executeQuery()
            val edgeExist = rs.next()
            rs.close()
            ps.close()
            conn.close()

            edgeExist
        }catch (e:SQLException){
            true
        }
    }

    private fun checkRequestExistence(hashOfId:String, hashOfIdSwapped: String): Boolean {
        val sql = "SELECT * from friendrequest WHERE requestId != ? OR requestId = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,hashOfId)
            ps.setString(2,hashOfIdSwapped)
            val rs = ps.executeQuery()
            val requestExist = rs.next()

            rs.close()
            ps.close()
            conn.close()

            requestExist
        }catch (e:SQLException){
            true
        }
    }

    fun removeFriend(user:User, username: String): Boolean {
        val userToDelete = getUserByUsername(username)
        return when(userToDelete){
            null -> throw UserDoesntExist("$username doest not exist")
            else -> removeEdge(
                    Utils.md5(user.adminNo + userToDelete.adminNo),
                    Utils.md5(userToDelete.adminNo + user.adminNo)
            )
        }
    }

    private fun removeEdge(hashOfId:String, hashOfIdSwapped: String): Boolean {
        val sql = "DELETE FROM friend WHERE edgeId = ? OR edgeId = ?"
        return try {
            val conn = getDbConnection()
            val ps = conn.prepareStatement(sql)
            ps.setString(1,hashOfId)
            ps.setString(2,hashOfIdSwapped)
            val rs = ps.executeUpdate()
            ps.close()
            conn.close()

            rs != 0
        }catch (e:SQLException){
            false
        }
    }
}