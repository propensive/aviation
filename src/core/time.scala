/*
    Aviation, version [unreleased]. Copyright 2023 Jon Pretty, Propensive OÜ.

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
import spectacular.*
import digression.*
import gossamer.*
import contextual.*
import anticipation.*
import quantitative.*

import scala.quoted.*
import java.util as ju
import java.time as jt

package calendars:
  given julian: RomanCalendar() with
    def leapYear(year: Y): Boolean = year%4 == 0
    def leapYearsSinceEpoch(year: Int): Int = year/4

  given gregorian: RomanCalendar() with
    def leapYear(year: Y): Boolean = year%4 == 0 && year%100 != 0 || year%400 == 0
    def leapYearsSinceEpoch(year: Int): Int = year/4 - year/100 + year/400 + 1

def now()(using src: Clock): Instant = src()

abstract class Clock():
  def apply(): Instant

object Clock:
  given current: Clock with
    def apply(): Instant = Instant.of(System.currentTimeMillis)
  
  def fixed(instant: Instant): Clock = new Clock():
    def apply(): Instant = instant

  def offset(diff: Duration): Clock = new Clock():
    def apply(): Instant = Instant.of(System.currentTimeMillis) + diff

enum Weekday:
  case Mon, Tue, Wed, Thu, Fri, Sat, Sun

case class InvalidDateError(text: Text) extends Error(msg"the value $text is not a valid date")

object Dates:
  opaque type Date = Int

  object Date:
    def of(day: Int): Date = day
    
    def apply
        (using cal: Calendar)
        (year: cal.Y, month: cal.M, day: cal.D)
        : Date throws InvalidDateError =
      cal.julianDay(year, month, day)

    //given (using CanThrow[InvalidDateError]): Canonical[Date] = Canonical(parse(_), _.show)
  
    given Ordering[Date] = Ordering.Int
    
    given Show[Date] = d =>
      given RomanCalendar = calendars.gregorian
      t"${d.day.toString.show}-${d.month.show}-${d.year.toString.show}"
    
    def parse(value: Text): Date throws InvalidDateError = value.cut(t"-") match
      case y :: m :: d :: Nil =>
        try
          import calendars.gregorian
          Date(y.s.toInt, MonthName(m.s.toInt), d.s.toInt)
        catch
          case err: NumberFormatException     => throw InvalidDateError(value)
          case err: ju.NoSuchElementException => throw InvalidDateError(value)
      
      case cnt =>
        throw InvalidDateError(value)

  extension (date: Date)
    def day(using cal: Calendar): cal.D = cal.getDay(date)
    def month(using cal: Calendar): cal.M = cal.getMonth(date)
    def year(using cal: Calendar): cal.Y = cal.getYear(date)
    def yearDay(using cal: Calendar): Int = date - cal.zerothDayOfYear(cal.getYear(date))
    def julianDay: Int = date
    
    infix def at(time: Time)(using Calendar): Timestamp = Timestamp(date, time)
    
    @targetName("plus")
    def +(period: Timespan)(using cal: Calendar): Date = cal.add(date, period)

    @targetName("addDays")
    def +(days: Int): Date = date + days

export Dates.Date

trait Calendar:
  type D
  type M
  type Y

  def daysInYear(year: Y): Int

  def getYear(date: Date): Y
  def getMonth(date: Date): M
  def getDay(date: Date): D
  def zerothDayOfYear(year: Y): Date
  def julianDay(year: Y, month: M, day: D): Date throws InvalidDateError
  def add(date: Date, period: Timespan): Date

abstract class RomanCalendar() extends Calendar:
  type Y = Int
  type M = MonthName
  type D = Int

  def leapYear(year: Y): Boolean

  def daysInMonth(month: M, year: Y): Int = month match
    case Jan | Mar | May | Jul | Aug | Oct | Dec => 31
    case Apr | Jun | Sep | Nov                   => 30
    case Feb                                     => if leapYear(year) then 29 else 28

  def add(date: Date, period: Timespan): Date =
    val monthTotal = getMonth(date).ordinal + period.months
    val month2 = MonthName.fromOrdinal(monthTotal%12)
    val year2 = getYear(date) + period.years + monthTotal/12
    unsafely(julianDay(year2, month2, getDay(date)) + period.days)
  
  def leapYearsSinceEpoch(year: Int): Int
  def daysInYear(year: Y): Int = if leapYear(year) then 366 else 365
  def zerothDayOfYear(year: Y): Date = Date.of(year*365 + leapYearsSinceEpoch(year) + 1721059)
  
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
  
  def julianDay(year: Int, month: MonthName, day: Int): Date throws InvalidDateError =
    if day < 1 || day > daysInMonth(month, year)
    then throw InvalidDateError(t"$year-${month.numerical}-$day")
    
    zerothDayOfYear(year) + month.offset(leapYear(year)) + day

class YearMonth[Y <: Nat, M <: MonthName & Singleton](year: Y, month: M):
  import compiletime.ops.int.*
  
  type CommonDays = 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 |
      19 | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28

  type Days <: Nat = M match
    case Jan.type | Mar.type | May.type | Jul.type | Aug.type | Oct.type | Dec.type =>
      CommonDays | 29 | 30 | 31
    
    case Apr.type | Jun.type | Sep.type | Nov.type =>
      CommonDays | 29 | 30
    
    case Feb.type => Y%4 match
      case 0 => Y%100 match
        case 0 => Y%400 match
          case 0 => CommonDays | 29
          case _ => CommonDays
        case _ => CommonDays | 29
      case _ => CommonDays

  @targetName("of")
  inline def -(day: Days): Date = unsafely(calendars.gregorian.julianDay(year, month, day))

extension (year: Nat)
  @targetName("of")
  inline def -(month: MonthName & Singleton): YearMonth[year.type, month.type] =
    new YearMonth(year, month)

object Timing:
  opaque type Instant = Long

  object Instant:
    def of(millis: Long): Instant = millis
    
    given generic: GenericInstant[Timing.Instant] with
      def makeInstant(long: Long): Timing.Instant = long
      def readInstant(instant: Timing.Instant): Long = instant
    
    given ordering: Ordering[Instant] = Ordering.Long

  type Duration = Quantity[Seconds[1]]

  object Duration:

    def of(millis: Long): Duration = Quantity(millis/1000.0)

    given generic: GenericDuration[Timing.Duration] with
      def makeDuration(long: Long): Timing.Duration = Quantity(long.toDouble)
      def readDuration(duration: Timing.Duration): Long = (duration.value*1000).toLong

  extension (instant: Instant)
    @targetName("minus")
    def -(that: Instant): Duration = Quantity((instant - that)/1000.0)
    
    @targetName("plus")
    def +(that: Duration): Instant = instant + (that.value/1000.0).toLong
    
    @targetName("to")
    def ~(that: Instant): Interval = Interval(instant, that)

    def in(using RomanCalendar)(timezone: Timezone): LocalTime =
      val zonedTime = jt.Instant.ofEpochMilli(instant).nn.atZone(jt.ZoneId.of(timezone.name.s)).nn
      
      val date = (zonedTime.getMonthValue: @unchecked) match
        case MonthName(month) => unsafely(Date(zonedTime.getYear, month, zonedTime.getDayOfMonth))
      
      val time = ((zonedTime.getHour, zonedTime.getMinute, zonedTime.getSecond): @unchecked) match
        case (Base24(hour), Base60(minute), Base60(second)) => Time(hour, minute, second)

      LocalTime(date, time, timezone)

  extension (duration: Duration)
    def from(instant: Instant): Interval = Interval(instant, instant + duration)

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

open class TimeSystem[Units <: Denomination]():
  def ambiguousTimes: TimeSystem.AmbiguousTimes = TimeSystem.AmbiguousTimes.Dilate
  def monthArithmetic: TimeSystem.MonthArithmetic = TimeSystem.MonthArithmetic.Scale
  def leapDayArithmetic: TimeSystem.LeapDayArithmetic = TimeSystem.LeapDayArithmetic.PreferFeb28
  def simplify(period: Period): Period = period

given TimeSystem[StandardTime] with
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
  given genericDuration: GenericDuration[Period & FixedDuration] with
    def makeDuration(long: Long): Period & FixedDuration =
      val hours: Int = (long/3600000L).toInt
      val minutes: Int = ((long%3600000L)/60000L).toInt
      val seconds: Int = ((long%60000L)/1000L).toInt
      
      new Period(0, 0, 0, hours, minutes, seconds) with FixedDuration
    
    def readDuration(period: Period & FixedDuration): Long =
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
  @targetName("plus")
  def +(p: Period)(using timeSys: TimeSystem[StandardTime]): Period =
    Period(years + p.years, months + p.months, days + p.days, hours + p.hours, minutes + p.minutes,
        seconds + p.seconds)
  
  @targetName("minus")
  def -(p: Period)(using timeSys: TimeSystem[StandardTime]): Period =
    Period(years - p.years, months - p.months, days - p.days, hours - p.hours, minutes - p.minutes,
        seconds - p.seconds)
  
  def simplify(using timeSys: TimeSystem[StandardTime]): Period = timeSys.simplify(this)

  @targetName("times")
  def *(n: Int): Period = Period(years*n, months*n, days*n, hours*n, minutes*n, seconds*n)

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

case class Timestamp(date: Date, time: Time)(using cal: Calendar):
  @targetName("plus")
  def +(period: Timespan): Timestamp =
    Timestamp(date, time)

object MonthName:
  def apply(i: Int): MonthName = MonthName.fromOrdinal(i - 1)

  def unapply(value: Text): Option[MonthName] =
    try Some(MonthName.valueOf(value.lower.capitalize.s))
    catch case err: IllegalArgumentException => None
  
  def unapply(value: Int): Option[MonthName] =
    if value < 1 || value > 12 then None else Some(fromOrdinal(value))
  
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

given sexagesimal: Chronology with
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

type Base60 = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 |
    19 | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 30 | 31 | 32 | 33 | 34 | 35 | 36 | 37 |
    38 | 39 | 40 | 41 | 42 | 43 | 44 | 45 | 46 | 47 | 48 | 49 | 50 | 51 | 52 | 53 | 54 | 55 | 56 |
    57 | 58 | 59

object Base24:
  def unapply(value: Int): Option[Base24] =
    if value < 0 || value > 23 then None else Some(value.asInstanceOf[Base24])

type Base24 = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 |
    19 | 20 | 21 | 22 | 23

given (using Unapply[Text, Int]): Unapply[Text, Base60] =
  case As[Int](value: Base60) => Some(value)
  case _                      => None

given (using Unapply[Text, Int]): Unapply[Text, Base24] =
  case As[Int](value: Base24) => Some(value)
  case _                      => None

extension (i: Int)
  @targetName("mod24")
  def %%(j: 24): Base24 =
    val x: Int = i%j.pipe { v => if v < 0 then v + 24 else v }
    x match
    case v: Base24 => v
    case _: Int    => throw Mistake("Modular arithmetic should produce value in range")
  
  @targetName("mod60")
  def %%(j: 60): Base60 =
    val x: Int = i%j.pipe { v => if v < 0 then v + 60 else v }
    x match
      case v: Base60 => v
      case _: Int    => throw Mistake("Modular arithmetic should produce value in range")

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
        
        if minutes >= 60 then fail("a time cannot have a minute value above 59", lit.pos)
        if hour < 0 then fail("a time cannot be negative", lit.pos)
        if hour > 12 then fail("a time cannot have an hour value above 12", lit.pos)
        
        val h: Base24 = (hour + (if pm then 12 else 0)).asInstanceOf[Base24]
        val length = lit.pos.endColumn - lit.pos.startColumn
        
        if (hour < 10 && length != 4) || (hour >= 10 && length != 5)
        then fail("the time should have exactly two minutes digits", lit.pos)
        
        val m: Base60 = minutes.asInstanceOf[Base60]
        '{Time(${Expr(h)}, ${Expr(m)}, 0)}
      
      case _ =>
        fail("expected a literal double value")

case class Timezone(name: Text) 

case class InvalidTimezoneError(name: Text)
extends Error(msg"the name $name does not refer to a known timezone")

case class LocalTime(date: Date, time: Time, timezone: Timezone):
  def instant(using RomanCalendar): Instant =
    val ldt = jt.LocalDateTime.of(date.year, date.month.numerical, date.day, time.hour, time.minute,
        time.second)
    
    Instant.of(ldt.nn.atZone(jt.ZoneId.of(timezone.name.s)).nn.toInstant.nn.toEpochMilli)

object Timezone:
  private val ids: Set[Text] = ju.TimeZone.getAvailableIDs.nn.map(_.nn).map(Text(_)).to(Set)

  def apply(name: Text): Timezone throws InvalidTimezoneError =
    if ids.contains(name) then new Timezone(name) else throw InvalidTimezoneError(name)
   
  object Tz extends Verifier[Timezone]:
    def verify(name: Text): Timezone =
      try Timezone(name)
      catch case err: InvalidTimezoneError => throw InterpolationError(err.message.text)

extension (inline context: StringContext)
  inline def tz(): Timezone = ${Timezone.Tz.expand('context)}
