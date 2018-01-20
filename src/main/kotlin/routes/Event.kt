package routes

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpPost
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.util.ValuesMap
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import models.TimeTable
import models.TimetableFromSpice
import models.toLesson
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.*
import kotlin.collections.ArrayList

fun Route.event(path: String) = route("$path/auth") {





}

/**
 * Gets the current month, and generates date in format for ddMMyy to fit SP's API.
 * Afterwards, HTTP GET in a loop for the whole month and put them in a Timetable Object.
 * Which will then get send out to the frontend.
 */
fun getTimeTableFromSpice() {
    val calendarInstance = Calendar.getInstance()

    val noOfDaysInMonth = YearMonth.of(
            calendarInstance.get(Calendar.YEAR) ,
            calendarInstance.get(Calendar.MONTH) + 1).lengthOfMonth()

    val targetDateFormat = SimpleDateFormat("ddMMyy")
    val originalFormat = SimpleDateFormat("yyyy-MM-dd")
    val arListOfDates = ArrayList<String>()
    val arrListOfLesson = ArrayList<TimeTable.Lesson>()
    var dayNo = 1

    //generates date in ddMMyy since first day of the current month to end of the month
    do {
        val temp= LocalDate.now().withDayOfMonth(dayNo).toString()
        val original= originalFormat.parse(temp)
        val dateStr = targetDateFormat.format(original)

        //arListOfDates.add(targetDateFormat.format(original))
        val url = "http://mobileappnew.sp.edu.sg/spTimetable/source/sptt.php?DDMMYY=$dateStr&id=1626175"

        val (_, response, res) =
                url.httpPost().responseObject(TimetableFromSpice.Deserializer())
        println(dayNo)
        //this has to be reversed because SP server returns the last lesson first
        res.get().timetable.asReversed().forEach { it.toLesson(dateStr)?.let { it1 -> arrListOfLesson.add(it1) } }

        dayNo ++
    }while (dayNo-1 < noOfDaysInMonth)

    println(arrListOfLesson)


}