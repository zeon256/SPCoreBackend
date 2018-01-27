package firebase

import java.util.*
import kotlin.concurrent.timerTask

class Firebase {

    /**
     * Http POST to https://fcm.googleapis.com/fcm/send
     * Check Postman for details
     *
     * This triggers every 15 minutes and checks if user has any lessons 15 minutes before
     * lesson begins
     *
     * So, take ALL lesson records (epoch) startTime minus currentTime and if == 15 minutes before
     * lesson begin, sends POST request to Firebase with devices that is going to have
     * that particular lesson
     *
     * Example: Time now is 8.15am Monday, anybody that has lessons at 8.30am will get
     * notification about their lesson at 8.30am
     *
     * Basically the server has to constantly query the database every 15 minutes interval
     * and only POST to Firebase if record exist
     *
     * This function has to be called only at start of server
     *
     * Start checking at 7.45am
     */
    fun sendNotification() {
        Timer().schedule(timerTask { /*
        call function that has to be called every 15min
        1. check lessons that are starting in 15min time which returns adminNo and Lesson object
        2. Http POST to google's server to send notifications with arrayList of deviceId for user &
        the lessonObject
        3. Spawn the same number of coroutines of the number of lessons available
        Initial starting time should be 7.45am on a Monday but for testing this can be faked
        */ }, 900000) //15min delay


    }
}