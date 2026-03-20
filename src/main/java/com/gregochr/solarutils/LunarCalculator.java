package com.gregochr.solarutils;

import java.time.ZonedDateTime;

/**
 * Calculates the Moon's position, illumination, phase, and distance for a given
 * observer location and instant.
 *
 * <p>Based on Jean Meeus, "Astronomical Algorithms", 2nd edition, Chapters 47 and 48.
 * Uses the principal terms of the lunar theory (simplified series), giving accuracy
 * within approximately 0.5° for position and 1% for illumination.
 *
 * <p>All calculations are performed locally — no network calls are made.
 *
 * <p>Usage:
 * <pre>{@code
 * var calc = new LunarCalculator();
 * var pos = calc.calculate(ZonedDateTime.now(), 54.776, -1.575);
 * System.out.println(pos.phase().displayName());  // e.g. "Waxing Crescent"
 * }</pre>
 */
public class LunarCalculator {

    // -------------------------------------------------------------------------
    // Meeus Table 47.A — principal terms for Σl (0.000001°) and Σr (0.001 km)
    // Columns: D, M, M', F, Σl coefficient, Σr coefficient
    // Terms involving M (column 1, solar mean anomaly) require E-correction.
    // -------------------------------------------------------------------------
    @SuppressWarnings("checkstyle:MagicNumber")
    private static final int[][] LONGITUDE_DISTANCE_TERMS = {
        { 0,  0,  1,  0,  6288774, -20905355},
        { 2,  0, -1,  0,  1274027,  -3699111},
        { 2,  0,  0,  0,   658314,  -2955968},
        { 0,  0,  2,  0,   213618,   -569925},
        { 0,  1,  0,  0,  -185116,     48888},
        { 0,  0,  0,  2,  -114332,     -3149},
        { 2,  0, -2,  0,    58793,    246158},
        { 2, -1, -1,  0,    57066,   -152138},
        { 2,  0,  1,  0,    53322,   -170733},
        { 2, -1,  0,  0,    45758,   -204586},
        { 0,  1, -1,  0,    41773,   -129620},
        { 1,  0,  0,  0,    17794,    108743},
        { 2,  1, -1,  0,    13115,     10575},
        { 2,  0, -3,  0,    11923,     10127},
        { 0,  1,  1,  0,    11643,    -36124},
        { 4,  0, -1,  0,    10932,          0},
        { 2, -1,  1,  0,    10321,          0},
        { 2, -2,  0,  0,     9060,          0},
        { 0,  0,  3,  0,     8520,          0},
        { 2,  2, -1,  0,     7888,          0},
        { 4,  0, -2,  0,     7222,          0},
        { 0,  2,  0,  0,     6679,          0},
        { 2,  2,  0,  0,     5478,          0},
        { 4, -1, -1,  0,     4407,          0},
        { 0,  0,  2, -2,    -4123,          0},
    };

    // -------------------------------------------------------------------------
    // Meeus Table 47.B — principal terms for Σb (0.000001°)
    // Columns: D, M, M', F, Σb coefficient
    // Terms involving M require E-correction.
    // -------------------------------------------------------------------------
    @SuppressWarnings("checkstyle:MagicNumber")
    private static final int[][] LATITUDE_TERMS = {
        { 0,  0,  0,  1,  5128122},
        { 0,  0,  1,  1,   280602},
        { 0,  0,  1, -1,   277693},
        { 2,  0,  0, -1,   173237},
        { 2,  0, -1,  1,    55413},
        { 2,  0, -1, -1,    46272},
        { 2,  0,  0,  1,    32573},
        { 0,  0,  2,  1,    17198},
        { 2,  0,  1, -1,     9266},
        { 0,  0,  2, -1,     8822},
        { 2, -1,  0, -1,     8216},
        { 2,  0, -2, -1,     4324},
        { 2,  0,  1,  1,     4200},
        {-2,  1,  0,  1,    -3359},
        { 2, -1, -1,  1,     2463},
    };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Calculates the Moon's position and state for a given observer and instant.
     *
     * <p>Accuracy (Meeus Ch.47):
     * <ul>
     *   <li>Longitude: within ~0.5°</li>
     *   <li>Latitude: within ~0.3°</li>
     *   <li>Distance: within ~0.5% of true value</li>
     *   <li>Illumination: within ~2% of true value</li>
     * </ul>
     *
     * @param dateTime  the date and time of observation (any time zone)
     * @param latitude  observer latitude in decimal degrees (positive = North, range [-90, 90])
     * @param longitude observer longitude in decimal degrees (positive = East, range [-180, 180])
     * @return the Moon's position and phase information
     * @throws IllegalArgumentException if latitude or longitude are out of range
     */
    public LunarPosition calculate(ZonedDateTime dateTime, double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be in [-90, 90], got: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be in [-180, 180], got: " + longitude);
        }

        double jd = CoordinateTransformer.toJulianDay(dateTime);
        double t = CoordinateTransformer.julianCentury(jd);

        // Moon ecliptic coordinates (Meeus Ch.47)
        double[] ecliptic = calculateMoonEcliptic(t);
        double lambda = ecliptic[0]; // ecliptic longitude, degrees
        double beta = ecliptic[1];   // ecliptic latitude, degrees
        double delta = ecliptic[2];  // distance, km

        // Convert to equatorial coordinates (Meeus Ch.13)
        double epsilon = CoordinateTransformer.meanObliquity(t);
        // Nutation correction to obliquity
        double omega = CoordinateTransformer.normalise360(125.04452 - 1934.136261 * t);
        double correctedEpsilon = epsilon + 0.00256 * CoordinateTransformer.cosDeg(omega);

        var equatorial = CoordinateTransformer.eclipticToEquatorial(lambda, beta, correctedEpsilon);

        // Convert to horizontal coordinates
        double gmst = CoordinateTransformer.greenwichMeanSiderealTime(jd);
        var horizontal = CoordinateTransformer.equatorialToHorizontal(
                equatorial.rightAscension(), equatorial.declination(), gmst, longitude, latitude);

        // Illumination and phase
        double sunLon = calculateSunLongitude(t);
        double elongation = CoordinateTransformer.normalise360(lambda - sunLon);
        double illumination = calculateIllumination(elongation);
        LunarPhase lunarPhase = determineLunarPhase(elongation);

        return new LunarPosition(horizontal.altitude(), horizontal.azimuth(), illumination, lunarPhase, delta);
    }

    // -------------------------------------------------------------------------
    // Private — lunar ecliptic coordinates (Meeus Ch.47)
    // -------------------------------------------------------------------------

    /**
     * Computes the Moon's ecliptic longitude, latitude, and distance.
     * Based on Meeus Ch.47, principal terms from Tables 47.A and 47.B.
     *
     * @param t Julian centuries from J2000.0
     * @return [lambda (degrees), beta (degrees), delta (km)]
     */
    /**
     * Computes the five fundamental arguments for the Meeus Ch.47 lunar position theory.
     * Returns [L', M, M', D, F] in degrees, normalised to [0, 360).
     *
     * @param t Julian centuries from J2000.0
     * @return [L'°, M°, M'°, D°, F°]
     */
    private double[] computeFundamentalArguments(double t) {
        double lPrime = CoordinateTransformer.normalise360(
                218.3164477 + 481267.88123421 * t - 0.0015786 * t * t
                        + t * t * t / 538841.0 - t * t * t * t / 65194000.0);
        double m = CoordinateTransformer.normalise360(
                357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0);
        double mPrime = CoordinateTransformer.normalise360(
                134.9634114 + 477198.8676313 * t + 0.0089970 * t * t
                        + t * t * t / 69699.0 - t * t * t * t / 14712000.0);
        double d = CoordinateTransformer.normalise360(
                297.8501921 + 445267.1114034 * t - 0.0018819 * t * t
                        + t * t * t / 545868.0 - t * t * t * t / 113065000.0);
        double f = CoordinateTransformer.normalise360(
                93.2720950 + 483202.0175233 * t - 0.0036539 * t * t
                        - t * t * t / 3526000.0 + t * t * t * t / 863310000.0);
        return new double[]{lPrime, m, mPrime, d, f};
    }

    private double[] calculateMoonEcliptic(double t) {
        // Fundamental arguments (Meeus Ch.47, eq.47.1)
        double[] args = computeFundamentalArguments(t);
        double lPrime = args[0];
        double m = args[1];
        double mPrime = args[2];
        double d = args[3];
        double f = args[4];

        // Eccentricity of Earth's orbit — corrects terms involving M (Meeus Ch.47, p.338)
        double e = 1.0 - 0.002516 * t - 0.0000074 * t * t;
        double e2 = e * e;

        // A1, A2, A3 — Venus, Jupiter, and flattening corrections (Meeus p.338)
        double a1 = CoordinateTransformer.normalise360(119.75 + 131.849 * t);
        double a2 = CoordinateTransformer.normalise360(53.09 + 479264.290 * t);
        double a3 = CoordinateTransformer.normalise360(313.45 + 481266.484 * t);

        // Sum longitude and distance terms (Table 47.A)
        double sumL = 0.0;
        double sumR = 0.0;
        for (int[] term : LONGITUDE_DISTANCE_TERMS) {
            double dCoeff = term[0];
            double mCoeff = term[1];
            double mPrimeCoeff = term[2];
            double fCoeff = term[3];
            double slCoeff = term[4];
            double srCoeff = term[5];

            double arg = dCoeff * d + mCoeff * m + mPrimeCoeff * mPrime + fCoeff * f;
            double eFactor = eccentricityFactor(mCoeff, e, e2);
            sumL += eFactor * slCoeff * CoordinateTransformer.sinDeg(arg);
            sumR += eFactor * srCoeff * CoordinateTransformer.cosDeg(arg);
        }

        // Sum latitude terms (Table 47.B)
        double sumB = 0.0;
        for (int[] term : LATITUDE_TERMS) {
            double dCoeff = term[0];
            double mCoeff = term[1];
            double mPrimeCoeff = term[2];
            double fCoeff = term[3];
            double sbCoeff = term[4];

            double arg = dCoeff * d + mCoeff * m + mPrimeCoeff * mPrime + fCoeff * f;
            double eFactor = eccentricityFactor(mCoeff, e, e2);
            sumB += eFactor * sbCoeff * CoordinateTransformer.sinDeg(arg);
        }

        // Additional corrections for Venus (A1), Jupiter (A2), and obliquity (A3)
        sumL += 3958.0 * CoordinateTransformer.sinDeg(a1)
                + 1962.0 * CoordinateTransformer.sinDeg(lPrime - f)
                + 318.0 * CoordinateTransformer.sinDeg(a2);

        sumB += -2235.0 * CoordinateTransformer.sinDeg(lPrime)
                + 382.0 * CoordinateTransformer.sinDeg(a3)
                + 175.0 * CoordinateTransformer.sinDeg(a1 - f)
                + 175.0 * CoordinateTransformer.sinDeg(a1 + f)
                + 127.0 * CoordinateTransformer.sinDeg(lPrime - mPrime)
                - 115.0 * CoordinateTransformer.sinDeg(lPrime + mPrime);

        // Compute final values (Meeus p.342)
        double lambda = CoordinateTransformer.normalise360(lPrime + sumL / 1_000_000.0);
        double beta = sumB / 1_000_000.0;
        double delta = 385000.56 + sumR / 1000.0;

        return new double[]{lambda, beta, delta};
    }

    /**
     * Returns the eccentricity correction factor for terms involving the solar mean anomaly M.
     * For |M|=1, multiply by E; for |M|=2, multiply by E².
     *
     * @param mCoeff the M coefficient (−2, −1, 0, 1, or 2)
     * @param e      eccentricity factor E
     * @param e2     e squared
     * @return correction factor
     */
    private double eccentricityFactor(double mCoeff, double e, double e2) {
        double absMCoeff = Math.abs(mCoeff);
        if (absMCoeff == 2.0) {
            return e2;
        } else if (absMCoeff == 1.0) {
            return e;
        }
        return 1.0;
    }

    // -------------------------------------------------------------------------
    // Private — solar longitude for phase angle (Meeus Ch.25, low-precision)
    // -------------------------------------------------------------------------

    /**
     * Computes the Sun's apparent ecliptic longitude in degrees.
     * Low-precision version (Meeus Ch.25) sufficient for phase angle calculation.
     * Accuracy: better than 0.01° for dates near J2000.0.
     *
     * @param t Julian centuries from J2000.0
     * @return apparent solar longitude in degrees [0, 360)
     */
    private double calculateSunLongitude(double t) {
        double l0 = CoordinateTransformer.normalise360(280.46646 + t * (36000.76983 + t * 0.0003032));
        double mSun = 357.52911 + t * (35999.05029 - t * 0.0001537);
        double center = (1.914602 - t * (0.004817 + 0.000014 * t)) * CoordinateTransformer.sinDeg(mSun)
                + (0.019993 - 0.000101 * t) * CoordinateTransformer.sinDeg(2.0 * mSun)
                + 0.000289 * CoordinateTransformer.sinDeg(3.0 * mSun);
        double sunTrue = l0 + center;
        // Apparent longitude (aberration + nutation)
        double omega = 125.04 - 1934.136 * t;
        return CoordinateTransformer.normalise360(sunTrue - 0.00569 - 0.00478 * CoordinateTransformer.sinDeg(omega));
    }

    // -------------------------------------------------------------------------
    // Private — illumination and phase
    // -------------------------------------------------------------------------

    /**
     * Computes the fraction of the lunar disc that is illuminated (Meeus Ch.48).
     *
     * <p>The phase angle i is approximated as (180° − elongation), which is accurate
     * to better than 0.5° for the simplified lunar theory used here.
     *
     * @param elongation Moon–Sun elongation in degrees [0, 360)
     * @return illuminated fraction [0.0, 1.0]
     */
    private double calculateIllumination(double elongation) {
        // For elongation in [0, 360): phase angle i ≈ 180° - min(elongation, 360-elongation)
        // More precisely, use the actual angular separation folded to [0, 180°]
        double phaseAngle = elongation <= 180.0 ? 180.0 - elongation : elongation - 180.0;
        return (1.0 + CoordinateTransformer.cosDeg(phaseAngle)) / 2.0;
    }

    /**
     * Package-private: exposes phase determination logic for boundary-condition mutation testing.
     *
     * @param elongation Moon–Sun elongation in degrees [0, 360)
     * @return the corresponding lunar phase
     */
    LunarPhase phaseFromElongation(double elongation) {
        return determineLunarPhase(elongation);
    }

    /**
     * Package-private: exposes illumination calculation for mutation testing.
     *
     * @param elongation Moon–Sun elongation in degrees [0, 360)
     * @return illuminated fraction [0.0, 1.0]
     */
    double illuminationFromElongation(double elongation) {
        return calculateIllumination(elongation);
    }

    /**
     * Package-private: exposes eccentricity factor for mutation testing.
     *
     * @param mCoeff coefficient of M in the Meeus table term
     * @param e      Earth's orbital eccentricity
     * @param e2     e squared
     * @return eccentricity correction factor
     */
    double eccentricityFactorForTest(double mCoeff, double e, double e2) {
        return eccentricityFactor(mCoeff, e, e2);
    }

    /**
     * Package-private: exposes raw Moon ecliptic coordinates for mutation testing.
     * Returns [lambda (°), beta (°), delta (km)] for the given Julian centuries t.
     * See Meeus Ch.47.
     *
     * @param t Julian centuries from J2000.0
     * @return [ecliptic longitude °, ecliptic latitude °, distance km]
     */
    double[] moonEclipticForTest(double t) {
        return calculateMoonEcliptic(t);
    }

    /**
     * Package-private: exposes the five fundamental arguments for mutation testing.
     * Returns [L', M, M', D, F] in degrees, after normalisation to [0, 360).
     * See Meeus Ch.47, eq.47.1.
     *
     * @param t Julian centuries from J2000.0
     * @return [L'°, M°, M'°, D°, F°]
     */
    double[] fundamentalArgumentsForTest(double t) {
        return computeFundamentalArguments(t);
    }

    /**
     * Determines the lunar phase from the Moon–Sun elongation angle.
     *
     * <p>The elongation is divided into eight 45° sectors starting from 0° (New Moon).
     *
     * @param elongation Moon–Sun elongation in degrees [0, 360)
     * @return the corresponding {@link LunarPhase}
     */
    private LunarPhase determineLunarPhase(double elongation) {
        if (elongation < 22.5 || elongation >= 337.5) {
            return LunarPhase.NEW_MOON;
        } else if (elongation < 67.5) {
            return LunarPhase.WAXING_CRESCENT;
        } else if (elongation < 112.5) {
            return LunarPhase.FIRST_QUARTER;
        } else if (elongation < 157.5) {
            return LunarPhase.WAXING_GIBBOUS;
        } else if (elongation < 202.5) {
            return LunarPhase.FULL_MOON;
        } else if (elongation < 247.5) {
            return LunarPhase.WANING_GIBBOUS;
        } else if (elongation < 292.5) {
            return LunarPhase.LAST_QUARTER;
        } else {
            return LunarPhase.WANING_CRESCENT;
        }
    }
}
