package routes

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import database.ScheduleBlockSource
import exceptions.ErrorMsg
import exceptions.MISSING_JWT
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import models.TimeTable
import models.TimetableFromSpice
import models.toLesson
import routes.authentication.requireLogin
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.collections.ArrayList

// Prevent quadruple nested generics
private typealias FuelRRR = Triple<Request, Response, Result<TimetableFromSpice, FuelError>>

fun Route.event(path: String) = route("$path/event") {
    get {
        TODO()
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
