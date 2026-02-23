package com.gregochr.solarutils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Calculates precise sunrise and sunset times using the NOAA Solar Calculator algorithm.
 * Based on Jean Meeus, "Astronomical Algorithms", 2nd edition.
 *
 * <p>All calculations are performed locally — no network calls are made.
 * Accuracy is within ±1 minute for locations between 60°S and 60°N.
 */
public class SolarCalculator {

    /**
     * Solar zenith angle at sunrise/sunset in degrees.
     * 90.833° accounts for atmospheric refraction (~0.833°) and the solar disc radius.
     */
    private static final double ZENITH = 90.833;

    /**
     * Calculates the sunrise time for a given location and date.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate sunrise
     * @param zone      the time zone for the returned LocalDateTime
     * @return sunrise time as a LocalDateTime in the given time zone
     */
    public LocalDateTime sunrise(double latitude, double longitude, LocalDate date, ZoneId zone) {
        return calculate(latitude, longitude, date, zone, true);
    }

    /**
     * Calculates the sunset time for a given location and date.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate sunset
     * @param zone      the time zone for the returned LocalDateTime
     * @return sunset time as a LocalDateTime in the given time zone
     */
    public LocalDateTime sunset(double latitude, double longitude, LocalDate date, ZoneId zone) {
        return calculate(latitude, longitude, date, zone, false);
    }

    /**
     * Calculates the compass azimuth (degrees clockwise from North) at which the sun rises
     * for a given location and date.
     *
     * <p>At the equinoxes this is approximately 90° (due East). In summer it is less than 90°
     * (north of East); in winter it is greater than 90° (south of East).
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate the sunrise azimuth
     * @return sunrise azimuth in whole degrees (0–359, clockwise from North)
     */
    public int sunriseAzimuth(double latitude, double longitude, LocalDate date) {
        return calculateAzimuth(latitude, date, true);
    }

    /**
     * Calculates the compass azimuth (degrees clockwise from North) at which the sun sets
     * for a given location and date.
     *
     * <p>At the equinoxes this is approximately 270° (due West). In summer it is greater than
     * 270° (north of West); in winter it is less than 270° (south of West).
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate the sunset azimuth
     * @return sunset azimuth in whole degrees (0–359, clockwise from North)
     */
    public int sunsetAzimuth(double latitude, double longitude, LocalDate date) {
        return calculateAzimuth(latitude, date, false);
    }

    private int calculateAzimuth(double latitude, LocalDate date, boolean isSunrise) {
        double julianDay = toJulianDay(date);
        double t = julianCentury(julianDay);

        double meanLongitude = solarMeanLongitude(t);
        double meanAnomaly = solarMeanAnomaly(t);
        double eqCenter = equationOfCenter(t, meanAnomaly);
        double sunTrueLongitude = meanLongitude + eqCenter;

        double omega = 125.04 - 1934.136 * t;
        double apparentLongitude = sunTrueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double meanObliquity = meanObliquityOfEcliptic(t);
        double correctedObliquity = meanObliquity + 0.00256 * Math.cos(Math.toRadians(omega));

        double declinationDeg = Math.toDegrees(
                Math.asin(Math.sin(Math.toRadians(correctedObliquity)) * Math.sin(Math.toRadians(apparentLongitude)))
        );

        // At sunrise/sunset the solar altitude is -(ZENITH - 90°) = -0.833°,
        // accounting for atmospheric refraction and the solar disc radius.
        double altitudeDeg = -(ZENITH - 90.0);
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declinationDeg);
        double altRad = Math.toRadians(altitudeDeg);

        double cosAz = (Math.sin(decRad) - Math.sin(latRad) * Math.sin(altRad))
                / (Math.cos(latRad) * Math.cos(altRad));

        // Clamp to [-1, 1] to guard against floating-point rounding at extreme latitudes.
        cosAz = Math.max(-1.0, Math.min(1.0, cosAz));

        double azimuthDeg = Math.toDegrees(Math.acos(cosAz));

        // arccos gives the Eastern-half angle (0–180°); mirror it for sunset.
        if (!isSunrise) {
            azimuthDeg = 360.0 - azimuthDeg;
        }

        return (int) Math.round(azimuthDeg);
    }

    private LocalDateTime calculate(double latitude, double longitude, LocalDate date, ZoneId zone, boolean isSunrise) {
        double julianDay = toJulianDay(date);
        double t = julianCentury(julianDay);

        double meanLongitude = solarMeanLongitude(t);
        double meanAnomaly = solarMeanAnomaly(t);
        double eqCenter = equationOfCenter(t, meanAnomaly);
        double sunTrueLongitude = meanLongitude + eqCenter;

        double omega = 125.04 - 1934.136 * t;
        double apparentLongitude = sunTrueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double meanObliquity = meanObliquityOfEcliptic(t);
        double correctedObliquity = meanObliquity + 0.00256 * Math.cos(Math.toRadians(omega));

        double declination = Math.toDegrees(
                Math.asin(Math.sin(Math.toRadians(correctedObliquity)) * Math.sin(Math.toRadians(apparentLongitude)))
        );

        double eqTime = equationOfTime(t, meanLongitude, meanAnomaly);
        double hourAngle = hourAngle(latitude, declination);

        // Solar noon in minutes from midnight UTC
        double solarNoonMinutes = 720.0 - 4.0 * longitude - eqTime;

        // Sunrise = solar noon minus hour angle; sunset = solar noon plus hour angle
        double eventMinutes = isSunrise
                ? solarNoonMinutes - hourAngle * 4.0
                : solarNoonMinutes + hourAngle * 4.0;

        long totalSeconds = Math.round(eventMinutes * 60.0);
        LocalDateTime utcDateTime = date.atStartOfDay(ZoneOffset.UTC).toLocalDateTime().plusSeconds(totalSeconds);

        return utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime();
    }

    private double toJulianDay(LocalDate date) {
        int y = date.getYear();
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();

        if (m <= 2) {
            y -= 1;
            m += 12;
        }

        int a = y / 100;
        int b = 2 - a + a / 4;

        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    private double julianCentury(double julianDay) {
        return (julianDay - 2451545.0) / 36525.0;
    }

    private double solarMeanLongitude(double t) {
        double l0 = 280.46646 + t * (36000.76983 + t * 0.0003032);
        return l0 % 360.0;
    }

    private double solarMeanAnomaly(double t) {
        return 357.52911 + t * (35999.05029 - t * 0.0001537);
    }

    private double equationOfCenter(double t, double meanAnomaly) {
        double mRad = Math.toRadians(meanAnomaly);
        return (1.914602 - t * (0.004817 + 0.000014 * t)) * Math.sin(mRad)
                + (0.019993 - 0.000101 * t) * Math.sin(2 * mRad)
                + 0.000289 * Math.sin(3 * mRad);
    }

    private double meanObliquityOfEcliptic(double t) {
        return 23.0 + (26.0 + (21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0;
    }

    private double equationOfTime(double t, double meanLongitude, double meanAnomaly) {
        double epsilon = meanObliquityOfEcliptic(t);
        double omega = 125.04 - 1934.136 * t;
        double correctedEpsilon = epsilon + 0.00256 * Math.cos(Math.toRadians(omega));

        double y = Math.pow(Math.tan(Math.toRadians(correctedEpsilon / 2.0)), 2);
        double e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);

        double mRad = Math.toRadians(meanAnomaly);
        double l0Rad = Math.toRadians(meanLongitude);

        return 4.0 * Math.toDegrees(
                y * Math.sin(2 * l0Rad)
                - 2 * e * Math.sin(mRad)
                + 4 * e * y * Math.sin(mRad) * Math.cos(2 * l0Rad)
                - 0.5 * y * y * Math.sin(4 * l0Rad)
                - 1.25 * e * e * Math.sin(2 * mRad)
        );
    }

    private double hourAngle(double latitude, double declination) {
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        double zenithRad = Math.toRadians(ZENITH);

        double cosHourAngle = (Math.cos(zenithRad) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));

        return Math.toDegrees(Math.acos(cosHourAngle));
    }
}