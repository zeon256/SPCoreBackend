package firebase

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import database.ScheduleBlockSource
import models.TimeTable
import java.time.Instant
import java.util.*
import kotlin.concurrent.timerTask

class Firebase: TimerTask() {
    override fun run() {
        //Timer().schedule(timerTask { sendNotificationTest() }, 10000L)
        sendNotificationTest()
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
    private val authKey = "key=AAAA28AlqmU:APA91bFy0rQ5BeDR0xA0e9rc_C_FsU_7e960bfQs2CYV3tf3kG6GDLgZ2BIuzz9kY72R5RWLH2lqI45bhZ-tGJyBY7JUquEXEoWBiBmAbB19ensy8ZkdI5dGx7JAZJJbk9HrzrEUHVN1"

    fun sendNotification() {
        println("15min passed : Sending notifications ...")

        val currentTimeEpoch = Instant.now().toEpochMilli()
        val scheduleBlockSrc = ScheduleBlockSource()

        val lesson =
                scheduleBlockSrc.checkLessonStartTimeAll(currentTimeEpoch)

        lesson.forEach {
            // returns arrayList of devices to send to
            val lessonToSend = it //Lesson Object

            scheduleBlockSrc.getLessonStudentsByLessonId(it.id).forEach {
                notificationSetup(deviceId = it.deviceId,
                        adminNo = it.adminNo,
                        lesson = lessonToSend,
                        deviceIdArrList = arrayListOf(it.deviceId))
            }

        }

    }

    fun sendNotificationTest(){
        println("10s passed : Sending notifications ...")

        val scheduleBlockSrc = ScheduleBlockSource()

        val lesson = arrayListOf(
                scheduleBlockSrc.getLessonById("03defcf5ddd055f95b1fc4934400334f")!!)

        lesson.forEach {
            // returns arrayList of devices to send to
            val lessonToSend = it //Lesson Object

            scheduleBlockSrc.getLessonStudentsByLessonId(it.id).forEach {
                notificationSetup(deviceId = it.deviceId,
                        adminNo = it.adminNo,
                        lesson = lessonToSend,
                        deviceIdArrList = arrayListOf(it.deviceId))
            }

        }
    }

    fun notificationSetup(deviceId: String,adminNo: String, deviceIdArrList: ArrayList<String>, lesson: TimeTable.Lesson){
        val resCreateDeviceGrp = createDeviceGroup(adminNo,deviceIdArrList)

        var notifKey = ""

        //if NotifKey alr exist -> get NotifKey -> send Notif
        notifKey = if(resCreateDeviceGrp.component2() != null)
            retrieveNotificationKey(adminNo).component1()!!.notification_key
        else
            resCreateDeviceGrp.component1()!!.notification_key

        sendLessonNotification(deviceId,lesson)
    }

    // creating device group
    fun createDeviceGroup(adminNo: String, registration_ids: ArrayList<String>): Result<NotifKey, FuelError> {
        val url = "https://android.googleapis.com/gcm/notification"

        val (req,res,result) = url.httpPost()
                .header(mapOf("Content-Type" to "application/json",
                        "Authorization" to authKey,
                        "project_id" to 943821531749))
                .body(FCMNotifKey(notification_key_name = adminNo,
                        registration_ids = registration_ids).convertToJSON())
                .responseObject(NotifKey.Deserializer())

        println("Creating Device Grp")
        println(req)
        println(res)

        return result
    }

    //getNotifKey
    fun retrieveNotificationKey(adminNo: String): Result<NotifKey, FuelError> {
        val url = "https://android.googleapis.com/gcm/notification?notification_key_name=$adminNo"

        val (req,res,result) = url.httpGet()
                .header(mapOf("Content-Type" to "application/json",
                        "Authorization" to authKey,
                        "project_id" to 943821531749))
                .responseObject(NotifKey.Deserializer())
        println("Retrieving NotifKey")
        println(req)
        println(res)

        return result
    }

    //sendNotif
    fun sendLessonNotification(notifKey: String,lessonToSend:TimeTable.Lesson): Result<String, FuelError> {
        val url = "https://fcm.googleapis.com/fcm/send"

        val dataToSend = FCMRequest(notifKey, FCMRequest.FCMData(lesson = lessonToSend))

        val (req,res,result) = url.httpPost()
                .header(mapOf("Content-Type" to "application/json",
                        "Authorization" to authKey))
                .body(dataToSend.convertToJSON())
                .responseString()

        println("Sending Lesson")
        println(req)
        println(res)

        return result
    }

    data class FCMNotifKey(val operation:String = "create",
                           val notification_key_name: String,
                           val registration_ids: ArrayList<String> = arrayListOf()){
        fun convertToJSON() = Gson().toJson(this)
    }

    data class FCMRequest(val to: String,
                          val data: FCMData){
        data class FCMData(val type: String = "lesson",
                           val lesson: TimeTable.Lesson)
        fun convertToJSON() = Gson().toJson(this)
    }

    data class NotifKey(val notification_key: String) {
        class Deserializer : ResponseDeserializable<NotifKey> {
            override fun deserialize(content: String) =
                    Gson().fromJson(content, NotifKey::class.java)
        }
    }

    data class LessonDevice(val lessonId: String,
                            val adminNo: String,
                            val deviceId: String){
        class Deserializer : ResponseDeserializable<LessonDevice> {
            override fun deserialize(content: String) =
                    Gson().fromJson(content, LessonDevice::class.java)
        }
    }
}


