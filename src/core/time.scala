/*
    Aviation, version [unreleased]. Copyright 2024 Jon Pretty, Propensive OÜ.

    The primary distribution site is: https://propensive.com/

    Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
    file except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific language governing permissions
    and limitations under the License.
*/

package aviation

import rudiments.*
import vacuous.*
import hypotenuse.*
import fulminate.*
import spectacular.*
import symbolism.*
import contingency.*
import anticipation.*
import gossamer.*
import contextual.*
import quantitative.*

import scala.quoted.*
import java.util as ju
import java.time as jt

package calendars:
  given RomanCalendar as julian:
    def leapYear(year: Year): Boolean = year%4 == 0
    def leapYearsSinceEpoch(year: Int): Int = year/4

  given RomanCalendar as gregorian:
    def leapYear(year: Year): Boolean = year%4 == 0 && year%100 != 0 || year%400 == 0
    def leapYearsSinceEpoch(year: Int): Int = year/4 - year/100 + year/400 + 1

def now()(using clock: Clock): Instant = clock()
def today()(using clock: Clock, calendar: RomanCalendar, timezone: Timezone): Date = (now() in timezone).date

abstract class Clock():
  def apply(): Instant

object Clock:
  given Clock as current:
    def apply(): Instant = Instant.of(System.currentTimeMillis)

  def fixed(instant: Instant): Clock = new Clock():
    def apply(): Instant = instant

  def offset(diff: Duration): Clock = new Clock():
    def apply(): Instant = Instant.of(System.currentTimeMillis) + diff

enum Weekday:
  case Mon, Tue, Wed, Thu, Fri, Sat, Sun

case class DateError(text: Text) extends Error(m"the value $text is not a valid date")

object Dates:
  opaque type Date = Int

  object Date:
    erased given Underlying[Date, Int] as underlying = ###
    def of(day: Int): Date = day

    def apply(using cal: Calendar)(year: cal.Year, month: cal.Month, day: cal.Day): Date raises DateError =
      cal.julianDay(year, month, day)

    given Date is Showable as show = d =>
      given RomanCalendar = calendars.gregorian
      t"${d.day.toString.show}-${d.month.show}-${d.year.toString.show}"

    given (using Tactic[DateError]) => Decoder[Date] as decoder = parse(_)

    given (using RomanCalendar) => Encoder[Date] as encoder = date =>
      import hieroglyph.textMetrics.uniform
      t"${date.year.toString.tt}-${date.month.numerical.toString.tt.pad(2, Rtl, '0')}-${date.day.toString.tt.pad(2, Rtl, '0')}"

    inline given Orderable[Date, Date] as orderable:
      inline def compare(inline left: Date, inline right: Date, inline strict: Boolean, inline greaterThan: Boolean): Boolean =
        if left == right then !strict else (left < right)^greaterThan

    given ordering: Ordering[Date] = Ordering.Int

    given (using calendar: Calendar) => Date is Addable as plus:
      type Result = Date
      type Operand = Period
      def add(date: Date, period: Period): Date = calendar.add(date, period)

    def parse(value: Text)(using Tactic[DateError]): Date = value.cut(t"-").to(List) match
      // FIXME: This compiles successfully, but never seems to match
      //case As[Int](year) :: As[Int](month) :: As[Int](day) :: Nil =>
      case y :: m :: d :: Nil =>
        try
          import calendars.gregorian
          Date(y.s.toInt, MonthName(m.s.toInt), d.s.toInt)
        catch
          case error: NumberFormatException =>
            raise(DateError(value), Date(using calendars.gregorian)(2000, MonthName(1), 1))

          case error: ju.NoSuchElementException =>
            raise(DateError(value), Date(using calendars.gregorian)(2000, MonthName(1), 1))

      case cnt =>
        raise(DateError(value), Date(using calendars.gregorian)(2000, MonthName(1), 1))

  extension (date: Date)
    def day(using calendar: Calendar): calendar.Day = calendar.getDay(date)
    def month(using calendar: Calendar): calendar.Month = calendar.getMonth(date)
    def year(using calendar: Calendar): calendar.Year = calendar.getYear(date)
    def yearDay(using calendar: Calendar): Int = date - calendar.zerothDayOfYear(calendar.getYear(date))
    def julianDay: Int = date
    def addDays(count: Int): Date = date + count

    infix def at (time: Time)(using Calendar): Timestamp = Timestamp(date, time)

    @targetName("plus")
    infix def + (period: Period)(using Calendar): Date = Date.plus.add(date, period)

    @targetName("gt")
    infix def > (right: Date): Boolean = date > right

    @targetName("lt")
    infix def < (right: Date): Boolean = date < right

    @targetName("lte")
    infix def <= (right: Date): Boolean = date <= right

    @targetName("gte")
    infix def >= (right: Date): Boolean = date >= right

export Dates.Date

@capability
trait Calendar:
  type Day
  type Month
  type Year

  def daysInYear(year: Year): Int
  def getYear(date: Date): Year
  def getMonth(date: Date): Month
  def getDay(date: Date): Day
  def zerothDayOfYear(year: Year): Date
  def julianDay(year: Year, month: Month, day: Day): Date raises DateError
  def add(date: Date, period: Timespan): Date

@capability
abstract class RomanCalendar() extends Calendar:
  type Year = Int
  type Month = MonthName
  type Day = Int

  def leapYear(year: Year): Boolean

  def daysInMonth(month: Month, year: Year): Int = month match
    case Jan | Mar | May | Jul | Aug | Oct | Dec => 31
    case Apr | Jun | Sep | Nov                   => 30
    case Feb                                     => if leapYear(year) then 29 else 28

  def add(date: Date, period: Timespan): Date =
    val monthTotal = getMonth(date).ordinal + period.months
    val month2 = MonthName.fromOrdinal(monthTotal%12)
    val year2 = getYear(date) + period.years + monthTotal/12

    safely(julianDay(year2, month2, getDay(date)).addDays(period.days)).vouch(using Unsafe)

  def leapYearsSinceEpoch(year: Int): Int
  def daysInYear(year: Year): Int = if leapYear(year) then 366 else 365
  def zerothDayOfYear(year: Year): Date = Date.of(year*365 + leapYearsSinceEpoch(year) + 1721059)

  def getYear(date: Date): Int =
    def recur(year: Int): Int =
      val z = zerothDayOfYear(year).julianDay
      if z < date.julianDay && z + daysInYear(year) > date.julianDay then year else recur(year + 1)

    recur(((date.julianDay - 1721059)/366).toInt)

  def getMonth(date: Date): MonthName =
    val year = getYear(date)
    val ly = leapYear(year)
    MonthName.values.takeWhile(_.offset(ly) < date.yearDay(using this)).last

  def getDay(date: Date): Int =
    val year = getYear(date)
    val month = getMonth(date)
    date.julianDay - zerothDayOfYear(year).julianDay - month.offset(leapYear(year))

  def julianDay(year: Int, month: MonthName, day: Int): Date raises DateError =
    if day < 1 || day > daysInMonth(month, year)
    then raise
     (DateError(t"$year-${month.numerical}-$day"),
      Date(using calendars.julian)(2000, MonthName(1), 1))

    zerothDayOfYear(year).addDays(month.offset(leapYear(year)) + day)

case class YearMonth(year: Int, month: MonthName):
  import compiletime.ops.int.*

object YearMonth:
  given YearMonth is Subtractable as dayOfMonth:
    type Result = Date
    type Operand = Int

    def subtract(yearMonth: YearMonth, day: Int): Date =
      safely(calendars.gregorian.julianDay(yearMonth.year, yearMonth.month, day)).vouch(using Unsafe)

object Timing:
  opaque type Instant = Long
  opaque type TaiInstant = Long

  object TaiInstant:
    erased given underlying: Underlying[TaiInstant, Long] = ###
    given Timing.TaiInstant is GenericInstant as generic:
      def instant(millisecondsSinceEpoch: Long): Timing.TaiInstant = millisecondsSinceEpoch
      def millisecondsSinceEpoch(instant: Timing.TaiInstant): Long = instant


  object Instant:
    def apply[InstantType: GenericInstant](instant: InstantType): Instant = of(instant.millisecondsSinceEpoch)

    erased given underlying: Underlying[Instant, Long] = ###
    def of(millis: Long): Instant = millis

    given Timing.Instant is GenericInstant as generic:
      def instant(millisecondsSinceEpoch: Long): Timing.Instant = millisecondsSinceEpoch
      def millisecondsSinceEpoch(instant: Timing.Instant): Long = instant

    inline given Orderable[Instant, Instant] as orderable:
      inline def compare
          (inline left: Instant, inline right: Instant, inline strict: Boolean, inline greaterThan: Boolean)
              : Boolean =
        if left == right then !strict else (left < right)^greaterThan

    given ordering: Ordering[Instant] = Ordering.Long

    given Instant is Addable as plus:
      type Result = Instant
      type Operand = Duration
      def add(instant: Instant, duration: Duration): Instant = instant + (duration.value/1000.0).toLong

    given Instant is Subtractable as minus:
      type Result = Duration
      type Operand = Instant
      def subtract(left: Instant, right: Instant): Duration = Quantity((left - right)/1000.0)

  type Duration = Quantity[Seconds[1]]

  object Duration:
    def of(millis: Long): Duration = Quantity(millis/1000.0)

    given Timing.Duration is (GenericDuration & SpecificDuration) as generic =
      new GenericDuration with SpecificDuration:
        type Self = Timing.Duration
        def duration(milliseconds: Long): Timing.Duration = Quantity(milliseconds.toDouble)
        def milliseconds(duration: Timing.Duration): Long = (duration.value*1000).toLong

  extension (instant: Instant)
    @targetName("to")
    infix def ~ (that: Instant): Interval = Interval(instant, that)

    def tai: TaiInstant = LeapSeconds.tai(instant)

    //@targetName("plus")
    //infix def + (duration: Duration): Instant = Instant.plus(instant, duration)

    //@targetName("minus")
    //infix def - (duration: Instant): Duration = Instant.minus(instant, duration)

    infix def in (using RomanCalendar)(timezone: Timezone): LocalTime =
      val zonedTime = jt.Instant.ofEpochMilli(instant).nn.atZone(jt.ZoneId.of(timezone.name.s)).nn

      val date = (zonedTime.getMonthValue: @unchecked) match
        case MonthName(month) => unsafely(Date(zonedTime.getYear, month, zonedTime.getDayOfMonth))

      val time = ((zonedTime.getHour, zonedTime.getMinute, zonedTime.getSecond): @unchecked) match
        case (Base24(hour), Base60(minute), Base60(second)) => Time(hour, minute, second)

      LocalTime(date, time, timezone)

  extension (duration: Duration)
    def from(instant: Instant): Interval = Interval(instant, Instant.plus.add(instant, duration))

export Timing.{Instant, Duration}

case class Interval(from: Instant, to: Instant):
  def duration: Duration = to - from

trait Denomination

enum StandardTime extends Denomination:
  case Second, Minute, Hour, Day, Week, Month, Year

object TimeSystem:
  enum AmbiguousTimes:
    case Throw, Dilate, PreferEarlier, PreferLater

  enum MonthArithmetic:
    case Scale, Overcount, Fixed

  enum LeapDayArithmetic:
    case Throw, PreferFeb28, PreferMar1

open class TimeSystem[DenominationType <: Denomination]():
  def ambiguousTimes: TimeSystem.AmbiguousTimes = TimeSystem.AmbiguousTimes.Dilate
  def monthArithmetic: TimeSystem.MonthArithmetic = TimeSystem.MonthArithmetic.Scale
  def leapDayArithmetic: TimeSystem.LeapDayArithmetic = TimeSystem.LeapDayArithmetic.PreferFeb28
  def simplify(period: Period): Period = period

given TimeSystem[StandardTime]:
  override def simplify(timespan: Timespan): Timespan =
    val timespan2 = if timespan.seconds < 60 then timespan else
      val adjust = timespan.seconds/60
      timespan + adjust.minutes - (adjust*60).seconds

    val timespan3 = if timespan2.minutes < 60 then timespan2 else
      val adjust = timespan2.minutes/60
      timespan2 + adjust.hours - (adjust*60).minutes

    val result = if timespan3.months < 12 then timespan3 else
      val adjust = timespan3.months/12
      timespan3 + adjust.years - (adjust*12).months

    result

type Timespan = Period

trait FixedDuration:
  this: Period =>

object Period:
  given Period & FixedDuration is GenericDuration & SpecificDuration as genericDuration =
    new GenericDuration with SpecificDuration:
      type Self = Period & FixedDuration
      def duration(milliseconds: Long): Period & FixedDuration =
        val hours: Int = (milliseconds/3600000L).toInt
        val minutes: Int = ((milliseconds%3600000L)/60000L).toInt
        val seconds: Int = ((milliseconds%60000L)/1000L).toInt

        new Period(0, 0, 0, hours, minutes, seconds) with FixedDuration

      def milliseconds(period: Period & FixedDuration): Long =
        period.hours*3600000L + period.minutes*60000L + period.seconds*1000L

  def apply(denomination: StandardTime, n: Int): Period = (denomination: @unchecked) match
    case StandardTime.Year   => Period(n, 0, 0, 0, 0, 0)
    case StandardTime.Month  => Period(0, n, 0, 0, 0, 0)
    case StandardTime.Day    => Period(0, 0, n, 0, 0, 0)
    case StandardTime.Hour   => Period(0, 0, 0, n, 0, 0)
    case StandardTime.Minute => Period(0, 0, 0, 0, n, 0)
    case StandardTime.Second => Period(0, 0, 0, 0, 0, n)

  def fixed
      (denomination: (StandardTime.Second.type | StandardTime.Minute.type | StandardTime.Hour.type),
          n: Int): Period & FixedDuration =
    denomination match
      case StandardTime.Hour   => new Period(0, 0, 0, n, 0, 0) with FixedDuration
      case StandardTime.Minute => new Period(0, 0, 0, 0, n, 0) with FixedDuration
      case StandardTime.Second => new Period(0, 0, 0, 0, 0, n) with FixedDuration

  given (using TimeSystem[StandardTime]) => Period is Addable as plus:
    type Result = Period
    type Operand = Period
    def add(left: Period, right: Period): Period =
      Period(left.years + right.years, left.months + right.months, left.days + right.days, left.hours +
          right.hours, left.minutes + right.minutes, left.seconds + right.seconds)

  given (using TimeSystem[StandardTime]) => Period is Subtractable as minus:
    type Result = Period
    type Operand = Period

    def subtract(left: Period, right: Period): Period =
      Period(left.years - right.years, left.months - right.months, left.days - right.days, left.hours -
          right.hours, left.minutes - right.minutes, left.seconds - right.seconds)

trait DiurnalPeriod:
  def years: Int
  def months: Int
  def days: Int

trait TemporalPeriod:
  def hours: Int
  def minutes: Int
  def seconds: Int

case class Period
    (override val years: Int, override val months: Int, override val days: Int, hours: Int,
        minutes: Int, seconds: Int)
extends DiurnalPeriod, TemporalPeriod:
  def simplify(using timeSys: TimeSystem[StandardTime]): Period = timeSys.simplify(this)

  @targetName("times")
  infix def * (n: Int): Period = Period(years*n, months*n, days*n, hours*n, minutes*n, seconds*n)

  @targetName("plus")
  infix def + (right: Period): Period = Period.plus.add(this, right)

  @targetName("minus")
  infix def - (right: Period): Period = Period.minus.subtract(this, right)

extension (one: 1)
  def year: Timespan = Period(StandardTime.Year, 1)
  def month: Timespan = Period(StandardTime.Month, 1)
  def week: Timespan = Period(StandardTime.Week, 1)
  def day: Timespan = Period(StandardTime.Day, 1)
  def hour: Timespan & FixedDuration = Period.fixed(StandardTime.Hour, 1)
  def minute: Timespan & FixedDuration = Period.fixed(StandardTime.Minute, 1)
  def second: Timespan & FixedDuration = Period.fixed(StandardTime.Second, 1)

extension (int: Int)
  def years: Timespan = Period(StandardTime.Year, int)
  def months: Timespan = Period(StandardTime.Month, int)
  def weeks: Timespan = Period(StandardTime.Week, int)
  def days: Timespan = Period(StandardTime.Day, int)
  def hours: Timespan & FixedDuration = Period.fixed(StandardTime.Hour, int)
  def minutes: Timespan & FixedDuration = Period.fixed(StandardTime.Minute, int)
  def seconds: Timespan & FixedDuration = Period.fixed(StandardTime.Second, int)

case class Time(hour: Base24, minute: Base60, second: Base60 = 0)

object Timestamp:
  given Timestamp is Addable as plus:
    type Result = Timestamp
    type Operand = Timespan
    def add(left: Timestamp, right: Timespan): Timestamp = ???

case class Timestamp(date: Date, time: Time):
  infix def in (timezone: Timezone): LocalTime = LocalTime(date, time, timezone)

object MonthName:
  def apply(i: Int): MonthName = MonthName.fromOrdinal(i - 1)

  def unapply(value: Text): Option[MonthName] =
    try Some(MonthName.valueOf(value.lower.capitalize.s))
    catch case err: IllegalArgumentException => None

  def unapply(value: Int): Option[MonthName] =
    if value < 1 || value > 12 then None else Some(fromOrdinal(value - 1))

  given Int is Subtractable as monthOfYear:
    type Result = YearMonth
    type Operand = MonthName
    def subtract(year: Int, month: MonthName) = new YearMonth(year, month)

enum MonthName:
  case Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec

  def numerical: Int = ordinal + 1

  def offset(leapYear: Boolean): Int = (if leapYear && ordinal > 1 then 1 else 0) + this.match
    case Jan => 0
    case Feb => 31
    case Mar => 59
    case Apr => 90
    case May => 120
    case Jun => 151
    case Jul => 181
    case Aug => 212
    case Sep => 243
    case Oct => 273
    case Nov => 304
    case Dec => 334

export MonthName.{Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec}

trait Chronology:
  type Primary
  type Secondary
  type Tertiary
  type TimeRepr

  def addPrimary(time: Time, n: Primary): Time
  def addSecondary(time: Time, n: Secondary): Time
  def addTertiary(time: Time, n: Tertiary): Time

given Chronology as sexagesimal:
  type Primary = Base24
  type Secondary = Base60
  type Tertiary = Base60
  type TimeRepr = Time

  def addPrimary(time: Time, n: Base24): Time = time.copy(hour = (time.hour + n)%%24)

  def addSecondary(time: Time, n: Base60): Time =
    val minute: Base60 = (time.minute + n)%%60
    val hour: Base24 = (time.hour + (time.minute + n)/60)%%24
    time.copy(hour = hour, minute = minute)

  def addTertiary(time: Time, n: Base60): Time =
    val second: Base60 = (time.second + n)%%60
    val minute: Base60 = (time.minute + (time.second + n)/60)%%60
    val hour: Base24 = (time.hour + (time.minute + (time.second + n)/60)/60)%%24
    Time(hour, minute, second)

object Base60:
  def unapply(value: Int): Option[Base60] =
    if value < 0 || value > 59 then None else Some(value.asInstanceOf[Base60])

type Base60 = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 |
    21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 30 | 31 | 32 | 33 | 34 | 35 | 36 | 37 | 38 | 39 | 40 | 41 |
    42 | 43 | 44 | 45 | 46 | 47 | 48 | 49 | 50 | 51 | 52 | 53 | 54 | 55 | 56 | 57 | 58 | 59

object Base24:
  def unapply(value: Int): Option[Base24] =
    if value < 0 || value > 23 then None else Some(value.asInstanceOf[Base24])

type Base24 = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 |
    21 | 22 | 23

given (using Unapply[Text, Int]): Unapply[Text, Base60] =
  case As[Int](value: Base60) => Some(value)
  case _                      => None

given (using Unapply[Text, Int]): Unapply[Text, Base24] =
  case As[Int](value: Base24) => Some(value)
  case _                      => None

extension (i: Int)
  @targetName("mod24")
  infix def %% (j: 24): Base24 =
    val x: Int = i%j.pipe { v => if v < 0 then v + 24 else v }
    (x: @unchecked) match
    case v: Base24 => v

  @targetName("mod60")
  infix def %% (j: 60): Base60 =
    val x: Int = i%j.pipe { v => if v < 0 then v + 60 else v }
    (x: @unchecked) match
      case v: Base60 => v

extension (inline double: Double)
  inline def am: Time = ${Aviation.validTime('double, false)}
  inline def pm: Time = ${Aviation.validTime('double, true)}

object Aviation:

  def validTime(time: Expr[Double], pm: Boolean)(using Quotes): Expr[Time] =
    import quotes.reflect.*

    time.asTerm match
      case Inlined(None, Nil, lit@Literal(DoubleConstant(d))) =>
        val hour = d.toInt
        val minutes = ((d - hour) * 100 + 0.5).toInt

        if minutes >= 60 then abandon(m"a time cannot have a minute value above 59", lit.pos)
        if hour < 0 then abandon(m"a time cannot be negative", lit.pos)
        if hour > 12 then abandon(m"a time cannot have an hour value above 12", lit.pos)

        val h: Base24 = (hour + (if pm then 12 else 0)).asInstanceOf[Base24]
        val length = lit.pos.endColumn - lit.pos.startColumn

        if (hour < 10 && length != 4) || (hour >= 10 && length != 5)
        then abandon(m"the time should have exactly two minutes digits", lit.pos)

        val m: Base60 = minutes.asInstanceOf[Base60]
        '{Time(${Expr[Base24](h)}, ${Expr[Base60](m)}, 0)}

      case _ =>
        abandon(m"expected a literal double value")

@capability
case class Timezone private(name: Text)

case class TimezoneError(name: Text)
extends Error(m"the name $name does not refer to a known timezone")

object LocalTime:
  given (using RomanCalendar) => LocalTime is GenericInstant as generic =
    _.instant.millisecondsSinceEpoch

case class LocalTime(date: Date, time: Time, timezone: Timezone):
  def instant(using RomanCalendar): Instant =
    val ldt = jt.LocalDateTime.of(date.year, date.month.numerical, date.day, time.hour, time.minute,
        time.second)

    Instant.of(ldt.nn.atZone(jt.ZoneId.of(timezone.name.s)).nn.toInstant.nn.toEpochMilli)

object Timezone:
  private val ids: Set[Text] = ju.TimeZone.getAvailableIDs.nn.map(_.nn).map(Text(_)).to(Set)

  def apply(name: Text)(using Tactic[TimezoneError]): Timezone = parse(name)

  def parse(name: Text)(using Tactic[TimezoneError]): Timezone =
    if ids.contains(name) then new Timezone(name)
    else raise(TimezoneError(name), new Timezone(ids.head))

  object Tz extends Verifier[Timezone]:
    def verify(name: Text): Timezone =
      try throwErrors(Timezone.parse(name))
      catch case err: TimezoneError => throw InterpolationError(err.message)

extension (inline context: StringContext)
  inline def tz(): Timezone = ${Timezone.Tz.expand('context)}
