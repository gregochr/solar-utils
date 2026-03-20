package com.gregochr.solarutils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Calculates moonrise and moonset times for a given date and observer location.
 *
 * <p>Algorithm: scans 25 hours starting from local midnight in 15-minute steps, detecting
 * sign changes in the Moon's altitude. Each crossing is then refined to approximately
 * 4-second accuracy via bisection search.
 *
 * <p>The horizon threshold is −0.5° to account for lunar horizontal parallax and
 * atmospheric refraction, consistent with published moonrise/moonset times.
 *
 * <p>At high latitudes (roughly above 60°N or below 60°S) the Moon may not rise or set
 * on a given calendar day; in that case the corresponding {@link Optional} will be empty.
 *
 * <p>Usage:
 * <pre>{@code
 * var calc = new MoonriseMoonsetCalculator();
 * var times = calc.calculate(LocalDate.now(), 54.776, -1.575, ZoneId.of("Europe/London"));
 * times.moonrise().ifPresent(t -> System.out.println("Moonrise: " + t));
 * }</pre>
 */
public class MoonriseMoonsetCalculator {

    /** Altitude threshold in degrees; below this the Moon is considered set. */
    private static final double HORIZON = -0.5;

    /** Step size in minutes for the initial scan. */
    private static final int STEP_MINUTES = 15;

    /** Total scan window in hours (slightly more than a full day). */
    private static final int SCAN_HOURS = 25;

    /** Bisection terminates when the interval is smaller than this many seconds. */
    private static final double BISECTION_ACCURACY_SECONDS = 4.0;

    private final LunarCalculator lunarCalculator = new LunarCalculator();

    /**
     * Calculates moonrise and moonset times for the given date and location.
     *
     * @param date      the calendar date to search
     * @param latitude  observer latitude in decimal degrees (positive = North, range [-90, 90])
     * @param longitude observer longitude in decimal degrees (positive = East, range [-180, 180])
     * @param zone      the time zone for the returned times
     * @return moonrise and moonset times (either may be empty)
     * @throws IllegalArgumentException if latitude or longitude are out of range
     */
    public MoonriseMoonset calculate(LocalDate date, double latitude, double longitude, ZoneId zone) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be in [-90, 90], got: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be in [-180, 180], got: " + longitude);
        }

        // Start from local midnight
        ZonedDateTime start = date.atStartOfDay(zone);

        ZonedDateTime riseTime = null;
        ZonedDateTime setTime = null;

        int stepsPerHour = 60 / STEP_MINUTES;
        int totalSteps = SCAN_HOURS * stepsPerHour;

        ZonedDateTime t0 = start;
        double alt0 = altitude(t0, latitude, longitude);

        for (int i = 1; i <= totalSteps && (riseTime == null || setTime == null); i++) {
            ZonedDateTime t1 = start.plusMinutes((long) i * STEP_MINUTES);
            double alt1 = altitude(t1, latitude, longitude);

            if (riseTime == null && alt0 <= HORIZON && alt1 > HORIZON) {
                riseTime = bisect(t0, t1, latitude, longitude, true);
            }
            if (setTime == null && alt0 > HORIZON && alt1 <= HORIZON) {
                setTime = bisect(t0, t1, latitude, longitude, false);
            }

            t0 = t1;
            alt0 = alt1;
        }

        return new MoonriseMoonset(Optional.ofNullable(riseTime), Optional.ofNullable(setTime));
    }

    /**
     * Refines a horizon crossing time using bisection.
     *
     * @param t0        time before the crossing (altitude on the "entry" side of the horizon)
     * @param t1        time after the crossing
     * @param latitude  observer latitude
     * @param longitude observer longitude
     * @param rising    true if searching for a rising crossing, false for setting
     * @return the refined crossing time
     */
    private ZonedDateTime bisect(
            ZonedDateTime t0, ZonedDateTime t1,
            double latitude, double longitude, boolean rising) {
        ZonedDateTime lo = t0;
        ZonedDateTime hi = t1;

        while (secondsBetween(lo, hi) > BISECTION_ACCURACY_SECONDS) {
            ZonedDateTime mid = midpoint(lo, hi);
            double altMid = altitude(mid, latitude, longitude);

            boolean midBelowHorizon = altMid <= HORIZON;
            if (rising ? midBelowHorizon : !midBelowHorizon) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        return midpoint(lo, hi);
    }

    private double altitude(ZonedDateTime dateTime, double latitude, double longitude) {
        return lunarCalculator.calculate(dateTime, latitude, longitude).altitude();
    }

    private ZonedDateTime midpoint(ZonedDateTime a, ZonedDateTime b) {
        long millis = (a.toInstant().toEpochMilli() + b.toInstant().toEpochMilli()) / 2;
        return ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis), a.getZone());
    }

    private double secondsBetween(ZonedDateTime a, ZonedDateTime b) {
        return Math.abs(java.time.Duration.between(a, b).toMillis()) / 1000.0;
    }
}
