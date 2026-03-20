package com.gregochr.solarutils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Calculates solar event times and related quantities using the NOAA Solar Calculator algorithm.
 * Based on Jean Meeus, "Astronomical Algorithms", 2nd edition.
 *
 * <p>Provides:
 * <ul>
 *   <li>Sunrise and sunset times</li>
 *   <li>Civil dawn and dusk (sun at −6° — the golden/blue hour window)</li>
 *   <li>Solar noon</li>
 *   <li>Day length</li>
 *   <li>Sunrise and sunset compass azimuths</li>
 * </ul>
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
     * Solar zenith angle at civil twilight in degrees.
     * Civil twilight begins/ends when the sun is 6° below the horizon (zenith = 96°).
     * This defines the golden hour and blue hour window for photographers.
     */
    private static final double CIVIL_TWILIGHT_ZENITH = 96.0;

    // -------------------------------------------------------------------------
    // Sunrise / Sunset
    // -------------------------------------------------------------------------

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
        return calculateWithZenith(latitude, longitude, date, zone, true, ZENITH);
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
        return calculateWithZenith(latitude, longitude, date, zone, false, ZENITH);
    }

    // -------------------------------------------------------------------------
    // Civil twilight (golden hour / blue hour window)
    // -------------------------------------------------------------------------

    /**
     * Calculates the start of civil dawn — the moment the sun reaches −6° in the morning.
     *
     * <p>Civil dawn marks the beginning of the blue hour and golden hour window before sunrise.
     * It is typically 25–50 minutes before sunrise depending on latitude and time of year.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate civil dawn
     * @param zone      the time zone for the returned LocalDateTime
     * @return civil dawn time as a LocalDateTime in the given time zone
     */
    public LocalDateTime civilDawn(double latitude, double longitude, LocalDate date, ZoneId zone) {
        return calculateWithZenith(latitude, longitude, date, zone, true, CIVIL_TWILIGHT_ZENITH);
    }

    /**
     * Calculates the end of civil dusk — the moment the sun reaches −6° in the evening.
     *
     * <p>Civil dusk marks the end of the golden hour and blue hour window after sunset.
     * It is typically 25–50 minutes after sunset depending on latitude and time of year.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate civil dusk
     * @param zone      the time zone for the returned LocalDateTime
     * @return civil dusk time as a LocalDateTime in the given time zone
     */
    public LocalDateTime civilDusk(double latitude, double longitude, LocalDate date, ZoneId zone) {
        return calculateWithZenith(latitude, longitude, date, zone, false, CIVIL_TWILIGHT_ZENITH);
    }

    // -------------------------------------------------------------------------
    // Solar noon
    // -------------------------------------------------------------------------

    /**
     * Calculates the time of solar noon — when the sun is at its highest point in the sky.
     *
     * <p>Solar noon is not necessarily 12:00 local time; it varies by longitude and the
     * equation of time. At solar noon, shadows are shortest and light is most directly overhead.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate solar noon
     * @param zone      the time zone for the returned LocalDateTime
     * @return solar noon time as a LocalDateTime in the given time zone
     */
    public LocalDateTime solarNoon(double latitude, double longitude, LocalDate date, ZoneId zone) {
        double julianDay = CoordinateTransformer.toJulianDay(date);
        double t = CoordinateTransformer.julianCentury(julianDay);

        double meanLongitude = solarMeanLongitude(t);
        double meanAnomaly = solarMeanAnomaly(t);
        double eqTime = equationOfTime(t, meanLongitude, meanAnomaly);

        double solarNoonMinutes = 720.0 - 4.0 * longitude - eqTime;

        long totalSeconds = Math.round(solarNoonMinutes * 60.0);
        LocalDateTime utcDateTime = date.atStartOfDay(ZoneOffset.UTC).toLocalDateTime().plusSeconds(totalSeconds);
        return utcDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDateTime();
    }

    // -------------------------------------------------------------------------
    // Day length
    // -------------------------------------------------------------------------

    /**
     * Calculates the length of the day in minutes — the duration from sunrise to sunset.
     *
     * @param latitude  latitude in decimal degrees (positive = North)
     * @param longitude longitude in decimal degrees (positive = East)
     * @param date      the date for which to calculate day length
     * @return day length in minutes
     */
    public long dayLengthMinutes(double latitude, double longitude, LocalDate date) {
        LocalDateTime rise = sunrise(latitude, longitude, date, ZoneOffset.UTC);
        LocalDateTime set = sunset(latitude, longitude, date, ZoneOffset.UTC);
        return Duration.between(rise, set).toMinutes();
    }

    // -------------------------------------------------------------------------
    // Azimuth
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Private implementation
    // -------------------------------------------------------------------------

    private int calculateAzimuth(double latitude, LocalDate date, boolean isSunrise) {
        double julianDay = CoordinateTransformer.toJulianDay(date);
        double t = CoordinateTransformer.julianCentury(julianDay);

        double meanLongitude = solarMeanLongitude(t);
        double meanAnomaly = solarMeanAnomaly(t);
        double eqCenter = equationOfCenter(t, meanAnomaly);
        double sunTrueLongitude = meanLongitude + eqCenter;

        double omega = 125.04 - 1934.136 * t;
        double apparentLongitude = sunTrueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double meanObliquity = CoordinateTransformer.meanObliquity(t);
        double correctedObliquity = meanObliquity + 0.00256 * Math.cos(Math.toRadians(omega));

        double declinationDeg = Math.toDegrees(
                Math.asin(Math.sin(Math.toRadians(correctedObliquity))
                        * Math.sin(Math.toRadians(apparentLongitude)))
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

    private LocalDateTime calculateWithZenith(double latitude, double longitude, LocalDate date,
            ZoneId zone, boolean isSunrise, double zenith) {
        double julianDay = CoordinateTransformer.toJulianDay(date);
        double t = CoordinateTransformer.julianCentury(julianDay);

        double meanLongitude = solarMeanLongitude(t);
        double meanAnomaly = solarMeanAnomaly(t);
        double eqCenter = equationOfCenter(t, meanAnomaly);
        double sunTrueLongitude = meanLongitude + eqCenter;

        double omega = 125.04 - 1934.136 * t;
        double apparentLongitude = sunTrueLongitude - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        double meanObliquity = CoordinateTransformer.meanObliquity(t);
        double correctedObliquity = meanObliquity + 0.00256 * Math.cos(Math.toRadians(omega));

        double declination = Math.toDegrees(
                Math.asin(Math.sin(Math.toRadians(correctedObliquity))
                        * Math.sin(Math.toRadians(apparentLongitude)))
        );

        double eqTime = equationOfTime(t, meanLongitude, meanAnomaly);
        double hourAngle = hourAngle(latitude, declination, zenith);

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

    private double equationOfTime(double t, double meanLongitude, double meanAnomaly) {
        double epsilon = CoordinateTransformer.meanObliquity(t);
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

    private double hourAngle(double latitude, double declination, double zenith) {
        double latRad = Math.toRadians(latitude);
        double decRad = Math.toRadians(declination);
        double zenithRad = Math.toRadians(zenith);

        double cosHourAngle = (Math.cos(zenithRad) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad));

        return Math.toDegrees(Math.acos(cosHourAngle));
    }
}
