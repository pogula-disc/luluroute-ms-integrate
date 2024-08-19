package com.luluroute.ms.integrate.util;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;

import static com.luluroute.ms.integrate.util.Constants.FROM_DATE_FORMAT;

@Slf4j
public class DateUtil {
    public static long getCurrentTime() {
        return Instant.now().toEpochMilli();
    }


    public static LocalDateTime getLocalDateTime() {
        return LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
    }

    public static long currentDateTimeInLong() {
        return new Date().getTime();
    }

    public static Date convertToDate(String value) { //throws MappingFormatException {
        Date date = null;
        SimpleDateFormat df = new SimpleDateFormat(FROM_DATE_FORMAT);
        try {
            date = df.parse(value);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            //throw new MappingFormatException(String.format(Constants.PARSER_ERROR_FORMAT, field, ExceptionUtils.getStackTrace(e)));
        }
        return date;
    }

    public static Date getCurrentUtcTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat ldf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date d1 = null;
        try {
            d1 = ldf.parse(sdf.format(new Date()));
        } catch (java.text.ParseException e) {
            e.printStackTrace();
            log.info(e.getMessage());
        }
        return d1;
    }


    public static long offsetBetweenTimezone(String entityTimezone){
      LocalDateTime dateTime=  getLocalDateTime();
      return   ChronoUnit.MINUTES.between(dateTime.atZone(ZoneId.of("UTC")),
              dateTime.atZone(ZoneId.of(entityTimezone)));
    }
}
