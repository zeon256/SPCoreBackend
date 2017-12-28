package models

import io.ktor.util.AttributeKey

data class User(val adminNo: String,
                val userName: String?,
                val fullName: String?,
                val pp: String? = null){
    companion object {
        val key = AttributeKey<User>("user")
    }
}


/**
 * By right this should be a graph but since there
 * is no need to represent it on application level.
 * However it is represented on the database level
 * as a "graph" with certain limitations, as there will
 * be a lot duplicate. Not really ideal to store a graph
 * on a relational database but since SP only have approx
 * 15k students, this isn't really any issue
 */
data class Friends(val friends: ArrayList<User>)