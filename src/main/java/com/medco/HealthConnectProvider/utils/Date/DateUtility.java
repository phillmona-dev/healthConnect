package com.medco.HealthConnectProvider.utils.Date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;


public final class DateUtility {

    @Value("${spring.jpa.properties.hibernate.jdbc.time_zone}")
    private static  String timeZone;

    /**
     * Get current local date.
     *
     * @return
     */
    public static LocalDate getLocalDateFromClock() {
        return LocalDate.now();
    }

    /**
     * Get next day.
     *
     * @param localDate
     * @return
     */
    public static LocalDate getNextDay(LocalDate localDate) {
        return localDate.plusDays(1);
    }

    /**
     * Get Previous Day.
     *
     * @param localDate
     * @return
     */
    public static LocalDate getPreviousDay(LocalDate localDate) {
        return localDate.minus(1, ChronoUnit.DAYS);
    }

    /**
     * Get day of the week.
     *
     * @param localDate
     * @return
     */
    public static DayOfWeek getDayOfWeek(LocalDate localDate) {
        DayOfWeek day = localDate.getDayOfWeek();
        return day;
    }

    /**
     * Get first day of the Month.
     *
     * @return LocalDate
     */
    public static LocalDate getFirstDayOfMonth() {
        LocalDate firstDayOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        return firstDayOfMonth;
    }

    /**
     * Get start of the Day.
     *
     * @param localDate
     * @return
     */
    public static LocalDateTime getStartOfDay(LocalDate localDate) {
        LocalDateTime startofDay = localDate.atStartOfDay();
        return startofDay;
    }

    /**
     * Print current time in day,month and year format.
     */
    public static void printCurrentDayMonthAndYear() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        System.out.printf("Year : %d Month : %d day : %d \t %n", year, month, day);
    }

    /**
     * Check two dates are equals.
     *
     * @param date
     * @param today
     * @return
     */
    public static boolean checkDateEquals(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            System.out.printf("Today %s and date1 %s are same date %n", today, date);
            return true;
        }
        return false;
    }

    /**
     * Get current time.
     *
     * @return
     */
    public static LocalTime getCurrentTime() {
        LocalTime time = LocalTime.now();
        System.out.println("local time now : " + time);
        return time;
    }

    /**
     * Add hours to time.
     *
     * @param hours
     * @return
     */
    public static LocalTime addHoursToTime(int hours) {
        LocalTime time = LocalTime.now();
        LocalTime newTime = time.plusHours(hours); // adding two hours
        System.out.println("Time after 2 hours : " + newTime);
        return newTime;
    }

    /**
     * Get date and time by zone.
     *
     * @param timeZone
     * @return
     */
    public static ZonedDateTime timeZone(String timeZone) {
        ZoneId america = ZoneId.of(timeZone);
        LocalDateTime localtDateAndTime = LocalDateTime.now();
        ZonedDateTime dateAndTimeInNewYork = ZonedDateTime.of(localtDateAndTime, america);
        System.out.println("Current date and time in a particular timezone : " + dateAndTimeInNewYork);
        return dateAndTimeInNewYork;
    }

    /**
     * Check for leap year.
     */
    public static void checkLeapYear() {
        LocalDate today = LocalDate.now();
        if (today.isLeapYear()) {
            System.out.println("This year is Leap year");
        } else {
            System.out.println("2014 is not a Leap year");
        }
    }

    /**
     * get time stamp.
     *
     * @return
     */
    public static Instant getTimeStamp() {
        Instant timestamp = Instant.now();
        System.out.println("What is value of this instant " + timestamp);
        return timestamp;
    }

    /**
     * Get Date by hour,minute and seconds.
     *
     * @param hour
     * @param min
     * @param seconds
     * @return
     */
    public static LocalTime getLocalTimeUsingFactoryOfMethod(int hour, int min, int seconds) {
        return LocalTime.of(hour, min, seconds);
    }

    /**
     * Get zone date time.
     *
     * @param localDateTime
     * @param zoneId
     * @return
     */
    public static ZonedDateTime getZonedDateTime(LocalDateTime localDateTime, ZoneId zoneId) {
        return ZonedDateTime.of(localDateTime, zoneId);
    }

    /**
     * Returns a copy of this time with the specified amount added.
     *
     * @param localTime
     * @param duration
     * @return
     */
    public static LocalTime modifyDates(LocalTime localTime, Duration duration) {
        return localTime.plus(duration);
    }

    /**
     * Obtains a Duration representing the duration between two temporal objects.
     *
     * @param localTime1
     * @param localTime2
     * @return
     */
    public static Duration getDifferenceBetweenDates(LocalTime localTime1, LocalTime localTime2) {
        return Duration.between(localTime1, localTime2);
    }

    /**
     * Get date and time by passing format
     *
     * @param representation
     * @return
     */
    public static LocalDateTime getLocalDateTimeUsingParseMethod(String representation) {
        return LocalDateTime.parse(representation);
    }

    /**
     * Convert Date to Java 8 LocalDate
     *
     * @param date
     * @return
     */
    public static LocalDateTime convertDateToLocalDate(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * Convert Calender to LocalDate
     *
     * @param calendar
     * @return
     */
    public static LocalDateTime convertDateToLocalDate(Calendar calendar) {
        return LocalDateTime.ofInstant(calendar.toInstant(), ZoneId.systemDefault());
    }

    public static Date getCurrentDate() {

        LocalDateTime now = LocalDateTime.now();
        ZoneId zoneId = ZoneId.of("UTC+3");
        ZonedDateTime zonedDateTime = now.atZone(zoneId);
        Instant instant = zonedDateTime.toInstant();
        Date date = Date.from(instant);
        return date;
    }

    public static Date convertStringToDate(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC+3"));
        return dateFormat.parse(date);
    }

}
