package firebase

import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson
import database.ScheduleBlockSource
import models.TimeTable
import java.time.Instant
import java.util.*
import kotlin.concurrent.timerTask
import java.text.SimpleDateFormat
import java.text.DateFormat



class Firebase: TimerTask() {
    override fun run() {
        Timer().schedule(timerTask { sendNotification() }, 900000)
    }

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
    private val url = "https://fcm.googleapis.com/fcm/send"
    private val authKey = "key=AAAA28AlqmU:APA91bFy0rQ5BeDR0xA0e9rc_C_FsU_7e960bfQs2CYV3tf3kG6GDLgZ2BIuzz9kY72R5RWLH2lqI45bhZ-tGJyBY7JUquEXEoWBiBmAbB19ensy8ZkdI5dGx7JAZJJbk9HrzrEUHVN1"
    fun sendNotification() {
        println("15min passed : Sending notifications ...")
        val currentTimeEpoch = Instant.now().toEpochMilli()
        val scheduleBlockSrc = ScheduleBlockSource()

        val lesson = scheduleBlockSrc.checkLessonStartTimeAll(currentTimeEpoch)

        lesson.forEach {
            // returns arrayList of devices to send to
            val lessonToSend = it //Lesson Object

            scheduleBlockSrc.getLessonStudentsByLessonId(it.id).forEach {
                //it in this case is the registrationId
                val dataToSend = FCMRequest(it, FCMRequest.FCMData(lesson = lessonToSend))
                url.httpPost().body(dataToSend.convertToJSON()).response {
                    request, response, result ->
                    println("Request" + request)
                    println("Response" + response)
                    println("Result" + result)
                }
            }

        }

    }

    fun sendNotificationTest() {
        println("30s passed : Sending notifications ...")
        val currentTimeEpoch = Instant.now().toEpochMilli()
        val scheduleBlockSrc = ScheduleBlockSource()

        //val lesson = scheduleBlockSrc.checkLessonStartTimeAll(currentTimeEpoch)
        // hardcoding lessons for testing
        val lesson = arrayListOf(
                TimeTable.Lesson("05cf76ec0ccde638d662dec48cef51fb","ST0277","DEUI","LAB","T2253",1509332400000,1509343200000),
                TimeTable.Lesson("03defcf5ddd055f95b1fc4934400334f","ST0277","DEUI","LAB","T2253",1509332400000,1509343200000)
        )

        lesson.forEach {
            // returns arrayList of devices to send to
            val lessonToSend = it //Lesson Object

            scheduleBlockSrc.getLessonStudentsByLessonId(it.id).forEach {
                //it in this case is the registrationId
                val dataToSend = FCMRequest(it, FCMRequest.FCMData(lesson = lessonToSend))
                url.httpPost()
                        .header(mapOf("application/json" to "application/json",
                                "Authorization" to authKey ))
                        .body(dataToSend.convertToJSON()).response {
                    request, response, result ->
                    println("Request" + request)
                    println("Response" + response)
                    println("Result" + result)
                }
            }

        }

    }

    data class FCMRequest(val to: String,
                          val data: FCMData){
        data class FCMData(val type: String = "lesson",
                           val lesson: TimeTable.Lesson)
        fun convertToJSON() = Gson().toJson(this)
    }
}

fun test(){
    val dataToSend = Firebase.FCMRequest("APA91bFqRx9rmnu7tXi47cQBhKQNVIkLe_yutQ3vFCtR8SdeItjo1CTGsJYRRj7DDF_ju9Y2WncloQM1Y65n1JXtc_aJrTJXRNrBjowMkmhl-vwC_VdjSQQ",
            Firebase.FCMRequest.FCMData(lesson = TimeTable
                    .Lesson("00e978b6ea2ebee38ec9531347b6998f",
                            "LC8003",
                            "SIP",
                            "TUT",
                            "T1642",
                            1510815600000,
                            1510822800000)))

    "https://fcm.googleapis.com/fcm/send"
            .httpPost()
            .body(dataToSend.convertToJSON()).response { request, response, result ->

        println("Request -> \t $request")
        println("Response -> \t $response")
        println("Result -> \t" + result.get())
    }

}

fun main(args: Array<String>) {
    println("Testing notifications")
    //testing to send every 30s interval starting from 12.50am (28/1/2018)
    //val startTimeSend = 1517072520L
    val firebase = Firebase()
    //while (startTimeSend == Instant.now().toEpochMilli()){
        Timer().schedule(timerTask { firebase.sendNotificationTest() }, 10000)
    //}

}