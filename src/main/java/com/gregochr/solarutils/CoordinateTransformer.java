package com.gregochr.solarutils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Package-private utility class providing shared astronomical coordinate transformations.
 *
 * <p>Implements:
 * <ul>
 *   <li>Julian Day conversion (from {@link LocalDate} or {@link ZonedDateTime})</li>
 *   <li>Julian century from J2000.0</li>
 *   <li>Mean obliquity of the ecliptic (Meeus Ch.22)</li>
 *   <li>Greenwich Mean Sidereal Time (Meeus Ch.12)</li>
 *   <li>Ecliptic → equatorial conversion</li>
 *   <li>Equatorial → horizontal conversion</li>
 *   <li>Angle normalisation and degree-based trig helpers</li>
 * </ul>
 *
 * <p>All angle inputs and outputs are in decimal degrees unless otherwise stated.
 * Both {@link SolarCalculator} and {@link LunarCalculator} delegate to this class.
 */
final class CoordinateTransformer {

    private CoordinateTransformer() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Nested result records
    // -------------------------------------------------------------------------

    /** Equatorial coordinates: right ascension and declination, both in degrees. */
    record EquatorialCoordinates(double rightAscension, double declination) { }

    /** Horizontal coordinates: altitude and azimuth, both in degrees. */
    record HorizontalCoordinates(double altitude, double azimuth) { }

    // -------------------------------------------------------------------------
    // Julian Day
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link LocalDate} to Julian Day Number at 0h UT (midnight UTC).
     *
     * @param date the date to convert
     * @return Julian Day at midnight UTC of the given date
     */
    static double toJulianDay(LocalDate date) {
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

    /**
     * Converts a {@link ZonedDateTime} to Julian Day Number including fractional day.
     *
     * @param dateTime the date-time to convert (any zone; converted to UTC internally)
     * @return Julian Day including the time-of-day fraction
     */
    static double toJulianDay(ZonedDateTime dateTime) {
        ZonedDateTime utc = dateTime.withZoneSameInstant(ZoneOffset.UTC);
        LocalDate date = utc.toLocalDate();
        double fractionalDay = (utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0) / 24.0;
        return toJulianDay(date) + fractionalDay;
    }

    // -------------------------------------------------------------------------
    // Julian century
    // -------------------------------------------------------------------------

    /**
     * Converts a Julian Day to Julian centuries since J2000.0 (JD 2451545.0).
     *
     * @param julianDay the Julian Day
     * @return T, Julian centuries from J2000.0
     */
    static double julianCentury(double julianDay) {
        return (julianDay - 2451545.0) / 36525.0;
    }

    // -------------------------------------------------------------------------
    // Obliquity of the ecliptic
    // -------------------------------------------------------------------------

    /**
     * Computes the mean obliquity of the ecliptic in degrees (Meeus Ch.22).
     *
     * <p>Accuracy: better than 1 arcsecond over ±2000 years from J2000.0.
     *
     * @param t Julian centuries from J2000.0
     * @return mean obliquity in degrees
     */
    static double meanObliquity(double t) {
        return 23.0 + (26.0 + (21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0;
    }

    // -------------------------------------------------------------------------
    // Greenwich Mean Sidereal Time
    // -------------------------------------------------------------------------

    /**
     * Computes the Greenwich Mean Sidereal Time (GMST) in degrees for the given Julian Day.
     *
     * <p>Formula from Meeus Ch.12, eq.12.4. Accurate to ~0.1 second over centuries.
     *
     * @param julianDay the Julian Day (including fractional day for time of day)
     * @return GMST in degrees [0, 360)
     */
    static double greenwichMeanSiderealTime(double julianDay) {
        double t = julianCentury(julianDay);
        double theta = 280.46061837
                + 360.98564736629 * (julianDay - 2451545.0)
                + 0.000387933 * t * t
                - t * t * t / 38710000.0;
        return normalise360(theta);
    }

    // -------------------------------------------------------------------------
    // Coordinate transformations
    // -------------------------------------------------------------------------

    /**
     * Converts ecliptic coordinates to equatorial (Meeus Ch.13).
     *
     * @param lambda    ecliptic longitude in degrees
     * @param beta      ecliptic latitude in degrees
     * @param obliquity obliquity of the ecliptic in degrees
     * @return equatorial coordinates (right ascension [0,360°), declination [-90°,90°])
     */
    static EquatorialCoordinates eclipticToEquatorial(double lambda, double beta, double obliquity) {
        double lambdaRad = Math.toRadians(lambda);
        double betaRad = Math.toRadians(beta);
        double epsRad = Math.toRadians(obliquity);

        double ra = Math.toDegrees(Math.atan2(
                Math.sin(lambdaRad) * Math.cos(epsRad) - Math.tan(betaRad) * Math.sin(epsRad),
                Math.cos(lambdaRad)));
        double dec = Math.toDegrees(Math.asin(
                Math.sin(betaRad) * Math.cos(epsRad)
                        + Math.cos(betaRad) * Math.sin(epsRad) * Math.sin(lambdaRad)));

        return new EquatorialCoordinates(normalise360(ra), dec);
    }

    /**
     * Converts equatorial coordinates to horizontal (altitude/azimuth) for a given observer.
     *
     * <p>Azimuth is measured from North, increasing clockwise (0=N, 90=E, 180=S, 270=W).
     *
     * @param ra        right ascension in degrees [0, 360)
     * @param dec       declination in degrees [-90, 90]
     * @param gmst      Greenwich Mean Sidereal Time in degrees
     * @param longitude observer longitude in degrees (positive = East)
     * @param latitude  observer latitude in degrees (positive = North)
     * @return horizontal coordinates
     */
    static HorizontalCoordinates equatorialToHorizontal(
            double ra, double dec, double gmst, double longitude, double latitude) {
        // Local hour angle: positive for objects past the meridian (moving westward)
        double h = Math.toRadians(normalise360(gmst + longitude - ra));
        double decRad = Math.toRadians(dec);
        double latRad = Math.toRadians(latitude);

        // Altitude
        double sinAlt = Math.sin(latRad) * Math.sin(decRad) + Math.cos(latRad) * Math.cos(decRad) * Math.cos(h);
        sinAlt = Math.max(-1.0, Math.min(1.0, sinAlt));
        double alt = Math.toDegrees(Math.asin(sinAlt));

        // Azimuth (from North, clockwise) via components
        double cosAlt = Math.cos(Math.toRadians(alt));
        double sinAz;
        double cosAz;
        if (cosAlt < 1e-10) {
            // Object is at or very near zenith — azimuth is undefined, default to North
            sinAz = 0.0;
            cosAz = 1.0;
        } else {
            sinAz = -Math.cos(decRad) * Math.sin(h) / cosAlt;
            cosAz = (Math.sin(decRad) - Math.sin(latRad) * sinAlt) / (Math.cos(latRad) * cosAlt);
        }
        double az = normalise360(Math.toDegrees(Math.atan2(sinAz, cosAz)));

        return new HorizontalCoordinates(alt, az);
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Normalises an angle in degrees to the range [0, 360).
     *
     * @param deg angle in degrees
     * @return equivalent angle in [0, 360)
     */
    static double normalise360(double deg) {
        double result = deg % 360.0;
        return result < 0 ? result + 360.0 : result;
    }

    /**
     * Returns the sine of an angle given in degrees.
     *
     * @param deg angle in degrees
     * @return sine value
     */
    static double sinDeg(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    /**
     * Returns the cosine of an angle given in degrees.
     *
     * @param deg angle in degrees
     * @return cosine value
     */
    static double cosDeg(double deg) {
        return Math.cos(Math.toRadians(deg));
    }
}
