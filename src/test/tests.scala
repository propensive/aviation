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

import probably.*
import rudiments.*
import gossamer.*
import perforate.*
import spectacular.*

import errorHandlers.throwUnsafely

object Tests extends Suite(t"Aviation Tests"):
  def run(): Unit =
    suite(t"Parsing tests"):
      test(t"Parse a canonical date"):
        Date.parse(t"2011-12-13")
      .check(_ == 2011-Dec-13)
      
      test(t"Parse a date with a single-digit month"):
        Date.parse(t"2011-09-13")
      .assert(_ == 2011-Sep-13)
      
      test(t"Parse a date in the distant past"):
        Date.parse(t"59-09-13")
      .assert(_ == 59-Sep-13)
      
      test(t"Month cannot be higher than 12"):
        capture(Date.parse(t"59-13-13"))
      .assert(_ == InvalidDateError(t"59-13-13"))
      
      test(t"Month cannot be less than 1"):
        capture(Date.parse(t"59-0-13"))
      .assert(_ == InvalidDateError(t"59-0-13"))
      
      test(t"Day cannot be over 31"):
        capture(Date.parse(t"59-11-32"))
      .assert(_ == InvalidDateError(t"59-11-32"))
      
      test(t"Day must exist in month"):
        capture(Date.parse(t"59-11-31"))
      .assert(_ == InvalidDateError(t"59-11-31"))


    suite(t"Gregorian Calendar Tests"):
      test(t"2000 is a leap year"):
        calendars.gregorian.leapYear(2000)
      .assert(_ == true)
      
      test(t"1800, 1900, 2100, 2200 are not leap years"):
        List(1800, 1900, 2100, 2200).map(calendars.gregorian.leapYear)
      .assert(_.forall(!_))
  
      test(t"Years not divisble by 4 are never leap years"):
        List(1985, 2001, 2023, 1843).map(calendars.gregorian.leapYear)
      .assert(_.forall(!_))
    
      test(t"Check recent Julian Day"):
        (2022-Dec-16).julianDay
      .assert(_ == 2459930)
      
      test(t"Check Julian Day in 1950"):
        (1950-Mar-10).julianDay
      .assert(_ == 2433351)
      
      test(t"Check Julian Day in 1650"):
        (1650-Mar-10).julianDay
      .assert(_ == 2323779)
      
      test(t"Check Julian Day in Year 1600"):
        (1600-Jan-1).julianDay
      .assert(_ == 2305449)

      test(t"Check Julian Day in Year 1"):
        (1-Jan-1).julianDay
      .assert(_ == 1721426)

      test(t"Get zeroth day of year"):
        (2010-Jan-1).julianDay -> (calendars.gregorian.zerothDayOfYear(2010).julianDay + 1)
      .assert(_ == _)
      
      test(t"Get days in non-leap-year"):
        calendars.gregorian.daysInYear(1995)
      .assert(_ == 365)

      test(t"Get days in leap-year"):
        calendars.gregorian.daysInYear(1996)
      .assert(_ == 366)

      test(t"Get Year from Date"):
        given calendar: Calendar = calendars.gregorian
        val date = 2016-Jul-11
        calendar.getYear(date)
      .assert(_ == 2016)
      
      test(t"Check Gregorian date"):
        given calendar: Calendar = calendars.gregorian
        2016-Apr-11
      .assert(_ == 2016-Apr-11)

      test(t"Get Gregorian date"):
        given calendar: Calendar = calendars.gregorian
        val date = 2016-Jul-11
        (date.year, date.month, date.day)
      .assert(_ == (2016, Jul, 11))

      test(t"Add two periods"):
        val period = 1.day + 2.months
        val period2 = 3.days + 1.year
        period + period2
      .assert(_ == 4.days + 2.months + 1.year)
      
      test(t"Simplify a period"):
        (8.months + 6.months).simplify
      .assert(_ == 1.year + 2.months)
      
      test(t"Hours do not simplify"):
        (1.day + 25.hours).simplify
      .assert(_ == 25.hours + 1.day)
      
      test(t"Minutes simplify"):
        123.minutes.simplify
      .assert(_ == 2.hours + 3.minutes)
      
      test(t"Seconds simplify"):
        123.seconds.simplify
      .assert(_ == 2.minutes + 3.seconds)
      
      test(t"Cascading simplification"):
        (1.hour + 59.minutes + 59.seconds + 2.seconds).simplify
      .assert(_ == 2.hours + 1.second)

      test(t"Simple multiplication"):
        (1.hour + 5.minutes)*100
      .assert(_ == 100.hours + 500.minutes)
      
      test(t"Simplified multiplication"):
        ((1.hour + 5.seconds)*100).simplify
      .assert(_ == 100.hours + 8.minutes + 20.seconds)

      test(t"Specify times"):
        2.01.am
      .assert(_ == Time(2, 1, 0))
      
      test(t"Specify times 2"):
        2.59.am
      .assert(_ == Time(2, 59, 0))
      
      test(t"Specify times 3"):
        11.40.am
      .assert(_ == Time(11, 40, 0))
      
      test(t"Specify times 4"):
        7.25.pm
      .assert(_ == Time(19, 25, 0))
      
      import calendars.gregorian
      test(t"Specify datetime"):
        2018-Aug-11 at 5.25.pm
      .assert(_ == Timestamp(Date(2018, Aug, 11), Time(17, 25, 0)))

      test(t"Add two months to a date"):
        2014-Nov-20 + 2.months
      .assert(_ == 2015-Jan-20)
      
      test(t"Add two days to a date"):
        2014-Nov-20 + 2.days
      .assert(_ == 2014-Nov-22)
      
      test(t"Add one year to a date"):
        2014-Nov-20 + 1.year
      .assert(_ == 2015-Nov-20)
      
      test(t"Add two years to a date"):
        2014-Nov-20 + 2.years
      .assert(_ == 2016-Nov-20)
      
      test(t"Add three years to a date"):
        2014-Nov-20 + 3.years
      .assert(_ == 2017-Nov-20)
      
      // test(t"Read TZDB file"):
      //   Tzdb.parseFile(t"europe")
      // .assert(_ == List())
