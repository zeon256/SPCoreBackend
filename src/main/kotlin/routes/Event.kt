package routes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import database.AuthSource
import database.ScheduleBlockSource
import database.Utils
import exceptions.*
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.ValuesMap
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import models.*
import routes.authentication.requireLogin
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// Prevent quadruple nested generics
private typealias FuelRRR = Triple<Request, Response, Result<TimetableFromSpice, FuelError>>

fun Route.event(path: String) = route("$path/event") {
    get {
        TODO("get events that user is part of")
    }

    get("myCreatedEvents"){
        val user = requireLogin()
        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                call.respond(ScheduleBlockSource().getMyCreatedEvents(user))
            }
        }
    }

    get("lesson") {
        val user = requireLogin()
        when (user) {
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                val source = ScheduleBlockSource()
                // if user is getting data for the first time, user will be added to database for filtration
                val filterResults = source.checkFilter(user)
                if(filterResults == -1)
                    // if cap reached call from server instead
                    call.respond(source.getLessons(user))
                else {
                    val lessons = getTimeTableFromSpice(user.adminNo.substring(1, 8),null)
                    lessons.forEach { source.insertLessons(it,user) }
                    call.respond(lessons)
                }

                // there should another table that store lesson_student
                // which stores adminNo and hash of lesson
                // so that lessons can be queried

                // also there will be another table that contains the number of time the student has
                // queried SP's server at the month to prevent spams. It should be cap to max of 5
                // queries per month and it will reset the next month
            }
        }
    }

    post {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()

        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val event = Event(
                            Utils.md5(form.toString()),
                            form["title"].toString(),
                            form["location"].toString(),
                            form["startTime"]!!.toLong(),
                            form["endTime"]!!.toLong(),
                            user)
                    val source = ScheduleBlockSource()

                    val res = source.createEvent(event)
                    if(res == 1)
                        call.respond(event.id)
                    else
                        call.respond(HttpStatusCode.BadRequest, ErrorMsg("Make sure all fields are filled in", BAD_REQUEST))

                }catch (e:NullPointerException){
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Make sure all fields are filled in", BAD_REQUEST))
                }catch (e:SQLException){
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Duplicate found", DUPLICATE_FOUND))
                }
            }
        }
    }

    post("inviteGuest") {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()

        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                try {
                    val eventId = form["eventId"]
                    val userSource = AuthSource()
                    val scheduleSource = ScheduleBlockSource()
                    if(eventId.isNullOrBlank())
                        call.respond(HttpStatusCode.BadRequest, ErrorMsg("Event Id missing!", BAD_REQUEST))
                    else{
                        val invitedGuestAdminNo = form["invitedGuestAdminNo"].toUserList()
                        val event = scheduleSource.getEvent(eventId!!)

                        // check if event is created by user that is sending this request
                        if(event?.creator?.adminNo != user.adminNo)
                            call.respond(HttpStatusCode.Unauthorized, ErrorMsg("$user is not event host!", NOT_EVENT_HOST))
                        else{
                            val invitedGuest = ArrayList<User>()

                            // Get user based on adminNumbers that are in invitedGuestAdminNo
                            // if user adds their own adminNo it will automatically be discarded
                            invitedGuestAdminNo?.forEach { userSource.getUserById(it)?.let {
                                it1 -> if(it1.adminNo != user.adminNo ) invitedGuest.add(it1)
                            } }

                            // return guest that was successfully invited
                            // to show that people that have been added
                            // will not get reinvited more than once
                            val succesfullyInvitedGuest = ArrayList<String>()

                            invitedGuest.forEach {
                                val res = scheduleSource.invitePeople(eventId,it)
                                if (res == 1)
                                    succesfullyInvitedGuest.add(it.adminNo)
                            }

                            call.respond("Successfully invited $succesfullyInvitedGuest")
                        }
                    }

                }catch (e: SQLException){
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Make sure all fields are filled in", BAD_REQUEST))
                }

            }
        }
    }

    put {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        val eventId = form["eventId"]

        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                // first check if event is created by the same user
                val source = ScheduleBlockSource()
                if(eventId.isNullOrBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Event Id missing!", BAD_REQUEST))
                else {
                    val event = source.getEvent(eventId!!)

                    if(event?.creator?.adminNo != user.adminNo){
                        call.respond(HttpStatusCode.Unauthorized, ErrorMsg("$user is not event host!", NOT_EVENT_HOST))
                    }else {
                        val updatedEvent = Event(
                                event.id,
                                form["title"].toString(),
                                form["location"].toString(),
                                form["startTime"]!!.toLong(),
                                form["endTime"]!!.toLong(),
                                user
                        )
                        val res = source.updateEvent(updatedEvent)
                        if(res == 0)
                            call.respond(HttpStatusCode.BadRequest, ErrorMsg("Bad Request!", BAD_REQUEST))
                        else
                            call.respond("Succesfully updated ${event.id}")

                    }
                }

            }
        }
    }

    delete {
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        val eventId = form["eventId"]

        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                // first check if event is created by the same user
                val source = ScheduleBlockSource()
                if(eventId.isNullOrBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Event Id missing!", BAD_REQUEST))
                else {
                    val event = source.getEvent(eventId!!)

                    if(event?.creator?.adminNo != user.adminNo){
                        call.respond(HttpStatusCode.Unauthorized, ErrorMsg("$user is not event host!", NOT_EVENT_HOST))
                    }else {
                        val res = source.deleteEvent(eventId)
                        if(res == 0)
                            call.respond(HttpStatusCode.BadRequest, ErrorMsg("Bad Request!", BAD_REQUEST))
                        else
                            call.respond("Succesfully deleted ${event.id} and all guest has been uninvited from the event.")

                    }
                }
            }
        }
    }

    // DeletedEvent, Going, NotGoing
    post("attendance"){
        // depending on the input -> call diff sql fn
        // -1 -> deleted invite
        // 0 -> not going
        // 1 -> going
        val user = requireLogin()
        val form = call.receive<ValuesMap>()
        val eventId = form["eventId"].toString()
        val attendance = form["attendance"].toString()

        when(user){
            null -> call.respond(HttpStatusCode.Unauthorized, ErrorMsg("Missing JWT", MISSING_JWT))
            else -> {
                if(eventId.isBlank())
                    call.respond(HttpStatusCode.BadRequest, ErrorMsg("Event Id missing!", BAD_REQUEST))
                else {
                    val source = ScheduleBlockSource()
                    var res = false


                    // for createIsNotGoing, createIsGoing & createDeletedInvite
                    // user has to be removed from the eventhaventrespond table
                    when(attendance){
                        "0" -> res = source.createIsNotGoing(user,eventId)
                        "1" -> res = source.createIsGoing(user,eventId)
                        "-1" -> res = source.createDeletedInvite(user,eventId)
                        else -> call.respond(HttpStatusCode.BadRequest, ErrorMsg("Invalid event attendance code!", BAD_REQUEST))
                    }
                    if(res){
                        val pair = HashMap<String,String>()
                        pair.put("0","not going")
                        pair.put("1","going")
                        pair.put("-1","deleted Invite")

                        call.respond("You have executed ${pair[attendance]}!")
                    }else {
                        call.respond(
                                ErrorMsg("Database Error", DATABASE_ERROR))
                    }
                }
            }
        }

    }

}

/**
 * Gets the current month, and generates date in format for ddMMyy to fit SP's API.
 * Afterwards, HTTP GET in a loop for the whole month and put them in a Timetable Object.
 * Which will then get send out to the frontend.
 * @param adminNo           eg. 1626175
 * @param noOfDays          eg. 5 -> means first 5 days. If it is null then it calculate the whole month
 */
private fun getTimeTableFromSpice(adminNo: String, noOfDays: Int?): ArrayList<TimeTable.Lesson> {
    val calendarInstance = Calendar.getInstance()

    var noOfDaysInMonth = 0

    noOfDaysInMonth = when(noOfDays){
        null -> YearMonth.of(
                calendarInstance.get(Calendar.YEAR),
                calendarInstance.get(Calendar.MONTH) + 1).lengthOfMonth()
        else -> noOfDays
    }


    val targetDateFormat = SimpleDateFormat("ddMMyy")
    val originalFormat = SimpleDateFormat("yyyy-MM-dd")
    val arrListOfLesson = ArrayList<TimeTable.Lesson>()
    var dayNo = 1

    val asyncResponses =
            mutableListOf<Deferred<Pair<FuelRRR, String>>>()

    // generates date in ddMMyy since first day of the current month to end of the month
    do {
        val temp = LocalDate.now().withDayOfMonth(dayNo).toString()
        val original = originalFormat.parse(temp)
        val dateStr = targetDateFormat.format(original)

        val url = "http://mobileappnew.sp.edu.sg/spTimetable/source/sptt.php?DDMMYY=$dateStr&id=$adminNo"
        asyncResponses.add(
                async {
                    println("Async: $dateStr")
                    Pair(url.httpPost().responseObject(TimetableFromSpice.Deserializer()),
                            dateStr)
                })

        dayNo++
    } while (dayNo - 1 < noOfDaysInMonth)

    asyncResponses.forEach {
        val (fuelRRR, dateStr) =
                runBlocking { it.await() }

        println("Awaited: $dateStr")

        val (_, _, res) = fuelRRR

        res.get().timetable.asReversed()
                .forEach {
                    it.toLesson(dateStr)?.apply {
                        arrListOfLesson.add(this)
                    }
                }
    }

    return arrListOfLesson
}
private fun String?.toUserList(): List<String>? = when (this.isNullOrBlank()) {
    true -> null
    else -> ArrayList<String>(this
            ?.split(","))
            .map { c: String -> c.trim() }
            .toList()
}
