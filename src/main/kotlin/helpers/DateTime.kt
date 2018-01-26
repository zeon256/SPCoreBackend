package com.spcore.helpers

import java.util.*

/**
 * Convert a [Long] timestamp to a [Calendar]
 */
fun Long.toCalendar(): Calendar {
    return Calendar.getInstance().apply { timeInMillis = this@toCalendar }
}

fun Date.toCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal.time = this
    return cal
}

fun Calendar.toDate(): Date {
    return Date(this.timeInMillis)
}

fun Calendar.startOfDay(): Calendar {
    val cal = this.clone() as Calendar
    cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.HOUR_OF_DAY, 0)

    return cal
}

/**
 * @param month Note that the `month` param is 0-based
 */
fun newCalendar(year: Int, month: Int, day: Int, hours: Int = 0, minutes: Int = 0, seconds: Int = 0, millis: Int = 0) : Calendar {
    return Calendar.getInstance().apply {
        set(year, month, day, hours, minutes, seconds)
        set(Calendar.MILLISECOND, millis)
    }
}

fun Calendar.isYesterday(): Boolean {
    return this.startOfDay() + Duration(days=1) == Calendar.getInstance().startOfDay()
}
fun Calendar.isToday(): Boolean {
    return this.startOfDay() == Calendar.getInstance().startOfDay()
}
fun Calendar.isTomorrow(): Boolean {
    return this.startOfDay() - Duration(days=1) == Calendar.getInstance().startOfDay()
}

fun humanReadableTimeRange(time1: Calendar, time2: Calendar) : String {
    val now = Calendar.getInstance()

    // The general idea here is that if a date component can be inferred,
    // don't display it

    // If this year is the same as time1's year and time2's year, no need to show
    // year because it is implied that the current year is year
    val showSecondYear = time1.get(Calendar.YEAR) != time2.get(Calendar.YEAR)
    val showFirstYear = showSecondYear || now.get(Calendar.YEAR) != time1.get(Calendar.YEAR)

    val firstYearString = if(showFirstYear) " " + time1.get(Calendar.YEAR).toString() else ""
    val secondYearString = if(showSecondYear) " " + time2.get(Calendar.YEAR).toString() else ""

    val showSecondDate = time1.startOfDay() != time2.startOfDay()

    val firstDateStr =
            when {
                time1.isYesterday() -> "Yesterday"
                time1.isToday() -> "Today"
                time1.isTomorrow() -> "Tomorrow"
                else -> time1.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) + " " +
                        time1.get(Calendar.DAY_OF_MONTH).toString() + " " +
                        time1.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
            }

    val secondDateStr =
            if(!showSecondDate) ""
            else if(time2.isYesterday()) "Yesterday"
            else if(time2.isToday()) "Today"
            else if(time2.isTomorrow()) "Tomorrow"
            else
                time2.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) + " " +
                time2.get(Calendar.DAY_OF_MONTH).toString() + " " +
                time2.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

    val firstTimeStr = time1.getHumanReadableTime(false)

    val secondTimeStr = time2.getHumanReadableTime(false)

    return if(showSecondDate) {
        "$firstDateStr$firstYearString, $firstTimeStr -\n" +
        "$secondDateStr$secondYearString, $secondTimeStr"
    } else {
        "$firstDateStr$firstYearString\n" +
        "$firstTimeStr - $secondTimeStr"
    }
}

fun Calendar.getHumanReadableDate(shortened: Boolean) : String {
    val formatStyle =
            if(shortened)
                Calendar.SHORT
            else
                Calendar.LONG

    val day = this.get(Calendar.DAY_OF_MONTH)
    val month = getDisplayName(Calendar.MONTH, formatStyle, Locale.getDefault())
    val year = get(Calendar.YEAR)
    return "$day $month $year"
}

fun Calendar.getHumanReadableTime(_24hr: Boolean) : String {
    val hour =
            if(_24hr)
                this.get(Calendar.HOUR_OF_DAY)
            else
                this.get(Calendar.HOUR).let {
                    if(it == 0) 12
                    else it
                }

    val minute =
            this.get(Calendar.MINUTE).let {
                if(it == 0)
                    ""
                else
                    ":%02d".format(it)
            }

    val am_pm =
            if(!_24hr)
                " " + if(this.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            else
                ""

    return "$hour$minute$am_pm"
}

/**
 * Infix method chaining builder for checking whether a [Calendar] value is
 * between some start and end time
 *
 * @param cal The time in question
 * @param start the start time
 * @param startExclusive whether or not the start bound is inclusivve or exclusive
 */
class CalendarRangeBuilder internal constructor(val cal: Calendar, val start: Calendar, val startInclusive: Boolean) {
    /**
     * Inclusive upper bounds
     *
     * For exclusive upper bounds see [upTo]
     */
    infix fun to(end: Calendar): Boolean {
        return !cal.after(end) &&   if(startInclusive)
                                        !cal.before(start)
                                    else
                                        cal.after(start)
    }

    /**
     * Exclusive upper bounds
     *
     * For inclusive upper bounds see [to]
     */
    infix fun upTo(end: Calendar): Boolean {
        return cal.before(end) &&   if(startInclusive)
                                        !cal.before(start)
                                    else
                                        cal.after(start)
    }
}

/**
 * Exclusive lower bounds
 *
 * For inclusive lower bounds, see [isFrom]
 */
infix fun Calendar.isBetween(start: Calendar): CalendarRangeBuilder {
    return CalendarRangeBuilder(this, start, false)
}

/**
 * Inclusive lower bounds
 *
 * For exclusive lower bounds, see [isBetween]
 */
infix fun Calendar.isFrom(start: Calendar): CalendarRangeBuilder {
    return CalendarRangeBuilder(this, start, true)

}

fun Calendar.getTimeAsDuration() : Duration {
    return Duration(
            0,
            get(Calendar.HOUR_OF_DAY).toLong(),
            get(Calendar.MINUTE).toLong(),
            get(Calendar.SECOND).toLong(),
            get(Calendar.MILLISECOND).toDouble())
}

fun Calendar.setTimeAsDuration(duration: Duration) {
    set(Calendar.HOUR_OF_DAY, duration.hours.toInt())
    set(Calendar.MINUTE, duration.minutes.toInt())
    set(Calendar.SECOND, duration.seconds.toInt())
    set(Calendar.MILLISECOND, duration.millis.toInt())
}

/**
 * Honestly because Java datetime and calendar sucks so much
 */
class Duration {
    var days: Long = 0
        private set
    var hours: Long = 0
        private set
    var minutes: Long = 0
        private set
    var seconds: Long = 0
        private set
    var millis: Double = 0.0
        private set

    val uncarriedDays: Double
        get() = toMillisAccurate()/1000/60/60/24

    val uncarriedHours: Double
        get() = toMillisAccurate()/1000/60/60

    val uncarriedMinutes: Double
        get() = toMillisAccurate()/1000/60

    val uncarriedSeconds: Double
        get() = toMillisAccurate()/1000


    constructor(
            days: Long = 0,
            hours: Long = 0,
            minutes: Long = 0,
            seconds: Long = 0,
            millis: Double = 0.0
    ) {
        this.millis = millis % 1000
        this.seconds = seconds + millis.toInt() / 1000

        this.minutes = minutes + this.seconds / 60
        this.seconds %= 60

        this.hours = hours + this.minutes / 60
        this.minutes %= 60

        this.days = days + this.hours / 24
        this.hours %= 24
    }

    fun toMillis(): Long {
        return  millis.toInt() +
                seconds * 1000 +
                minutes * 1000 * 60 +
                hours   * 1000 * 60 * 60 +
                days    * 1000 * 60 * 60 * 24
    }

    fun toMillisAccurate(): Double {
        return  millis +
                seconds * 1000 +
                minutes * 1000 * 60 +
                hours   * 1000 * 60 * 60 +
                days    * 1000 * 60 * 60 * 24
    }

    fun getAbsolute(): Duration {
        return  if (this.toMillisAccurate() < 0)
                    -this
                else
                    this
    }

    /**
     * Divides the duration into spans of size [span], and returns the
     * hypothetical upper bounds of the last partial span
     *
     * Or in plain english, round up.
     *
     * Usage: `oneHour30Mins.roundUpToNearest(Duration(hours=1))` will return 2 hours
     */
    fun roundUpToNearest(span: Duration): Duration {
        val rem = (this % span)
        return this - rem + if(rem == ZERO || this < ZERO) ZERO else span
    }

    fun roundUpToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Duration {
        val span = Duration(days, hours, minutes, seconds, millis)
        return roundUpToNearest(span)
    }
    /**
     * Divides the duration into spans of size [span], and returns the
     * lower bounds of the last partial span
     *
     * Or in plain english, round down.
     *
     * Usage: `oneHour30Mins.roundUpToNearest(Duration(hours=1))` will return 2 hours
     */
    fun roundDownToNearest(span: Duration): Duration {
        val rem = this % span
        return this - rem - if(this < ZERO && rem != ZERO) span else ZERO
    }

    fun roundDownToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Duration {
        val span = Duration(days, hours, minutes, seconds, millis)
        return roundDownToNearest(span)
    }
    /**
     * Divides the duration into spans of size [span], and returns the
     * hypothetical upper or lower bounds of the last partial span, depending on whether the
     * duration tends closer to the hypothetical upper or lower bounds respectively.
     *
     * Or in plain english, normal standard rounding.
     *
     * Usage: `oneHour30Mins.roundUpToNearest(Duration(hours=1))` will return 2 hours
     *
     */
    fun roundToNearest(span: Duration): Duration {
        val abs = this.getAbsolute()
        val rem = abs % span
        val roundUp = rem >= span / 2
        val absRnd = abs - rem + if (roundUp) span else ZERO
        return if(this < ZERO) -absRnd else absRnd
    }

    fun roundToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Duration {
        val span = Duration(days, hours, minutes, seconds, millis)
        return roundToNearest(span)
    }

    private operator fun unaryMinus(): Duration {
        return Duration(millis = -this.toMillisAccurate())
    }

    operator fun plus(that: Duration): Duration {
        return Duration(millis = this.toMillisAccurate() + that.toMillisAccurate())
    }

    operator fun minus(that: Duration): Duration {
        return Duration(millis = this.toMillisAccurate() - that.toMillisAccurate())
    }

    operator fun times(scalarMultiple: Double): Duration {
        return Duration(millis = this.toMillisAccurate() * scalarMultiple)
    }

    operator fun times(scalarMultiple: Int): Duration {
        return Duration(millis = this.toMillisAccurate() * scalarMultiple)
    }

    operator fun div(scalarDenominator: Double): Duration {
        return Duration(millis = this.toMillisAccurate() / scalarDenominator)
    }

    operator fun div(scalarDenominator: Int): Duration {
        return Duration(millis = this.toMillisAccurate() / scalarDenominator)
    }

    operator fun rem(that: Duration): Duration {
        return Duration(millis = this.toMillisAccurate() % that.toMillisAccurate())
    }

    operator fun compareTo(duration: Duration): Int {
        return this.toMillisAccurate().compareTo(duration.toMillisAccurate())
    }

    override fun toString(): String {
        return "$days days, $hours hours, $minutes minutes, and $seconds seconds"
    }


    companion object {
        val ZERO = Duration()
    }
}

/**
 * Provides a way to do basic addition of a timespan to a [Calendar] object
 * without killing it
 *
 * Usage: `calendar1 + Duration(minutes=1, seconds=3)`
 */
operator fun Calendar.plus(duration: Duration): Calendar {
    val newCalendar = this.clone() as Calendar
    newCalendar.add(Calendar.MILLISECOND, duration.toMillis().toInt())
    return newCalendar
}

/**
 * Provides a way to do basic subtraction of a timespan to a [Calendar] object
 * without killing it
 *
 * Usage: `calendar1 - Duration(minutes=1, seconds=3)`
 */
operator fun Calendar.minus(duration: Duration): Calendar {
    val newCalendar = this.clone() as Calendar
    newCalendar.add(Calendar.MILLISECOND, -duration.toMillis().toInt())
    return newCalendar
}

/**
 * Gets the difference between two [Calendar] objects
 *
 * Usage: `val durationTaken: Duration = endCalendar - startCalendar`
 *
 * @returns A [Duration] representing the signed (non-absolute) difference of [this] minus [that]
 */
operator fun Calendar.minus(that: Calendar): Duration {
    return Duration(millis=this.timeInMillis.toDouble() - that.timeInMillis.toDouble())
}

/**
 * Rounds up the time segment of the calendar to the nearest [duration]
 *
 * NOTE: Values will not roll but will carry over, e.g. 23:59 round up to nearest hour will be 00:00 the next day
 */
fun Calendar.roundUpToNearest(duration: Duration): Calendar {
    return this.startOfDay() + this.getTimeAsDuration().roundUpToNearest(duration)
}

fun Calendar.roundUpToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Calendar {
    val span = Duration(days, hours, minutes, seconds, millis)
    return this.roundUpToNearest(span)
}

/**
 * Rounds down the time segment of the calendar to the nearest [duration]
 */
fun Calendar.roundDownToNearest(duration: Duration): Calendar {
    return this.startOfDay() + this.getTimeAsDuration().roundDownToNearest(duration)
}

fun Calendar.roundDownToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Calendar {
    val span = Duration(days, hours, minutes, seconds, millis)
    return this.roundDownToNearest(span)
}

/**
 * Rounds up or down the time segment of the calendar to the nearest [duration]
 */
fun Calendar.roundToNearest(duration: Duration): Calendar {
    return this.startOfDay() + this.getTimeAsDuration().roundToNearest(duration)
}

fun Calendar.roundToNearest(days: Long = 0, hours: Long = 0, minutes: Long = 0, seconds: Long = 0, millis: Double = 0.0): Calendar {
    val span = Duration(days, hours, minutes, seconds, millis)
    return this.roundToNearest(span)
}