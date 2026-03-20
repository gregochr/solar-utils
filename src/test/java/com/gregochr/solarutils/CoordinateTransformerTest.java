package com.gregochr.solarutils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Verifies CoordinateTransformer calculations against known astronomical values.
 */
class CoordinateTransformerTest {

    private static final double ARC_SECOND_IN_DEGREES = 1.0 / 3600.0;

    @Nested
    @DisplayName("Julian Day conversion")
    class JulianDay {

        @Test
        @DisplayName("J2000.0 epoch (2000-01-01T12:00 UTC) corresponds to JD 2451545.0")
        void j2000Epoch() {
            ZonedDateTime j2000 = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
            double jd = CoordinateTransformer.toJulianDay(j2000);

            assertThat(jd).isCloseTo(2451545.0, offset(0.0001));
        }

        @Test
        @DisplayName("2000-01-01 at midnight UTC is JD 2451544.5")
        void j2000Midnight() {
            double jd = CoordinateTransformer.toJulianDay(LocalDate.of(2000, 1, 1));

            assertThat(jd).isCloseTo(2451544.5, offset(0.0001));
        }

        @Test
        @DisplayName("2025-01-13T00:00 UTC gives consistent JD from both LocalDate and ZonedDateTime")
        void consistencyBetweenOverloads() {
            LocalDate date = LocalDate.of(2025, 1, 13);
            ZonedDateTime dateTime = ZonedDateTime.of(2025, 1, 13, 0, 0, 0, 0, ZoneOffset.UTC);

            double jdFromDate = CoordinateTransformer.toJulianDay(date);
            double jdFromDateTime = CoordinateTransformer.toJulianDay(dateTime);

            assertThat(jdFromDateTime).isCloseTo(jdFromDate, offset(0.0001));
        }

        @Test
        @DisplayName("ZonedDateTime at 18:00 UTC gives JD fraction of 0.25 more than midnight")
        void fractionalDayAt18h() {
            ZonedDateTime noon = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime evening = ZonedDateTime.of(2025, 6, 1, 18, 0, 0, 0, ZoneOffset.UTC);

            double jdNoon = CoordinateTransformer.toJulianDay(noon);
            double jdEvening = CoordinateTransformer.toJulianDay(evening);

            assertThat(jdEvening - jdNoon).isCloseTo(0.75, offset(0.0001));
        }
    }

    @Nested
    @DisplayName("Julian century")
    class JulianCentury {

        @Test
        @DisplayName("Julian century at J2000.0 (JD 2451545.0) is 0.0")
        void j2000Century() {
            double t = CoordinateTransformer.julianCentury(2451545.0);

            assertThat(t).isCloseTo(0.0, offset(1e-10));
        }

        @Test
        @DisplayName("Julian century is 0.25 for JD 2451545.0 + 36525/4 days")
        void quarterCentury() {
            double t = CoordinateTransformer.julianCentury(2451545.0 + 36525.0 / 4.0);

            assertThat(t).isCloseTo(0.25, offset(1e-10));
        }
    }

    @Nested
    @DisplayName("Mean obliquity of the ecliptic")
    class Obliquity {

        @Test
        @DisplayName("Mean obliquity at J2000.0 is 23°26'21.448\" = 23.43929 degrees")
        void j2000Obliquity() {
            double epsilon = CoordinateTransformer.meanObliquity(0.0);

            // Exact value: 23 + 26/60 + 21.448/3600 = 23.439291... degrees
            assertThat(epsilon).isCloseTo(23.4393, offset(0.0001));
        }

        @Test
        @DisplayName("Obliquity at J2100 (T=1.0) is reduced by approximately 0.0131° from J2000")
        void j2100Obliquity() {
            double epsilon2000 = CoordinateTransformer.meanObliquity(0.0);
            double epsilon2100 = CoordinateTransformer.meanObliquity(1.0);

            // Principal term: -46.8150 arcsec per century = -0.013004° per century
            double expectedReduction = 46.8150 / 3600.0;
            assertThat(epsilon2000 - epsilon2100).isCloseTo(expectedReduction, offset(0.001));
        }

        @Test
        @DisplayName("Obliquity decreases slowly over centuries")
        void obliquityDecreasesOverTime() {
            double epsilon2000 = CoordinateTransformer.meanObliquity(0.0);
            double epsilon2100 = CoordinateTransformer.meanObliquity(1.0);

            assertThat(epsilon2000).isGreaterThan(epsilon2100);
        }
    }

    @Nested
    @DisplayName("Greenwich Mean Sidereal Time")
    class Gmst {

        @Test
        @DisplayName("GMST at J2000.0 noon (JD 2451545.0) is 280.46061837°")
        void gmstAtJ2000() {
            double gmst = CoordinateTransformer.greenwichMeanSiderealTime(2451545.0);

            // Exact value from Meeus Ch.12, eq.12.4
            assertThat(gmst).isCloseTo(280.46061837, offset(0.01));
        }

        @Test
        @DisplayName("GMST advances by 360.98564736629° per solar day")
        void gmstAdvancesAtCorrectRate() {
            double jd0 = 2451545.0;
            double gmst0 = CoordinateTransformer.greenwichMeanSiderealTime(jd0);
            double gmst1 = CoordinateTransformer.greenwichMeanSiderealTime(jd0 + 1.0);

            double delta = CoordinateTransformer.normalise360(gmst1 - gmst0);
            // Earth rotates 360.98564736629° per solar day
            assertThat(delta).isCloseTo(360.98564736629 % 360.0, offset(0.01));
        }

        @Test
        @DisplayName("GMST result is always in [0, 360)")
        void gmstIsNormalised() {
            double[] jds = {2451545.0, 2451545.5, 2400000.0, 2500000.0};
            for (double jd : jds) {
                double gmst = CoordinateTransformer.greenwichMeanSiderealTime(jd);
                assertThat(gmst).isBetween(0.0, 360.0);
            }
        }
    }

    @Nested
    @DisplayName("Ecliptic to equatorial conversion")
    class EclipticToEquatorial {

        private static final double OBLIQUITY = 23.4393;

        @Test
        @DisplayName("lambda=0°, beta=0°: RA=0° and dec=0° (vernal equinox)")
        void vernalEquinoxMapsToZero() {
            var eq = CoordinateTransformer.eclipticToEquatorial(0.0, 0.0, OBLIQUITY);

            assertThat(eq.rightAscension()).isCloseTo(0.0, offset(0.001));
            assertThat(eq.declination()).isCloseTo(0.0, offset(0.001));
        }

        @Test
        @DisplayName("lambda=90°, beta=0°: RA=90° and dec=obliquity")
        void eclipticAtZeroLatitude() {
            // At obliquity ε and lambda=90°, β=0°: RA=90°, dec=ε (summer solstice point)
            var eq = CoordinateTransformer.eclipticToEquatorial(90.0, 0.0, OBLIQUITY);

            assertThat(eq.rightAscension()).isCloseTo(90.0, offset(0.001));
            assertThat(eq.declination()).isCloseTo(OBLIQUITY, offset(0.001));
        }

        @Test
        @DisplayName("lambda=180°, beta=0°: RA=180° and dec=0° (autumnal equinox)")
        void autumnalEquinoxMapsToHalfCircle() {
            var eq = CoordinateTransformer.eclipticToEquatorial(180.0, 0.0, OBLIQUITY);

            assertThat(eq.rightAscension()).isCloseTo(180.0, offset(0.001));
            assertThat(eq.declination()).isCloseTo(0.0, offset(0.001));
        }

        @Test
        @DisplayName("lambda=270°, beta=0°: RA=270° and dec=-obliquity (winter solstice)")
        void winterSolsticeMapsToNegativeDeclination() {
            var eq = CoordinateTransformer.eclipticToEquatorial(270.0, 0.0, OBLIQUITY);

            assertThat(eq.rightAscension()).isCloseTo(270.0, offset(0.001));
            assertThat(eq.declination()).isCloseTo(-OBLIQUITY, offset(0.001));
        }

        @Test
        @DisplayName("North ecliptic pole (beta=90°): dec = 90 - obliquity")
        void northEclipticPoleDeclination() {
            var eq = CoordinateTransformer.eclipticToEquatorial(0.0, 90.0, OBLIQUITY);

            // North ecliptic pole dec = 90° - ε
            assertThat(eq.declination()).isCloseTo(90.0 - OBLIQUITY, offset(0.01));
        }

        @Test
        @DisplayName("Right ascension is always in [0, 360)")
        void rightAscensionIsNormalised() {
            for (double lambda = 0; lambda < 360; lambda += 45) {
                var eq = CoordinateTransformer.eclipticToEquatorial(lambda, 0.0, OBLIQUITY);
                assertThat(eq.rightAscension()).isBetween(0.0, 360.0);
            }
        }

        @Test
        @DisplayName("Declination is always in [-90, 90]")
        void declinationInRange() {
            for (double lambda = 0; lambda < 360; lambda += 30) {
                for (double beta = -90; beta <= 90; beta += 30) {
                    var eq = CoordinateTransformer.eclipticToEquatorial(lambda, beta, OBLIQUITY);
                    assertThat(eq.declination()).isBetween(-90.0, 90.0);
                }
            }
        }
    }

    @Nested
    @DisplayName("Equatorial to horizontal conversion")
    class EquatorialToHorizontal {

        // Durham, UK
        private static final double LAT = 54.776;
        private static final double LON = -1.575;

        @Test
        @DisplayName("Object on meridian (H=0) due south of observer has azimuth ~180°")
        void onMeridianDueSouth() {
            // For dec < lat: object transits due South
            // GMST chosen so that H = GMST + longitude - RA = 0
            double dec = 20.0;
            double ra = 100.0;
            double gmst = CoordinateTransformer.normalise360(ra - LON); // makes H = 0

            var horiz = CoordinateTransformer.equatorialToHorizontal(ra, dec, gmst, LON, LAT);

            assertThat(horiz.azimuth()).isCloseTo(180.0, offset(1.0));
        }

        @Test
        @DisplayName("Object rising in East has azimuth near 90° for equatorial declination")
        void risingInEast() {
            // H = -90° (object 6h before meridian, rising in East)
            double ra = 100.0;
            double dec = 0.0;
            double gmst = CoordinateTransformer.normalise360(ra - LON - 90.0);

            var horiz = CoordinateTransformer.equatorialToHorizontal(ra, dec, gmst, LON, LAT);

            assertThat(horiz.azimuth()).isCloseTo(90.0, offset(2.0));
        }

        @Test
        @DisplayName("Object setting in West has azimuth near 270° for equatorial declination")
        void settingInWest() {
            // H = +90° (object 6h past meridian, setting in West)
            double ra = 100.0;
            double dec = 0.0;
            double gmst = CoordinateTransformer.normalise360(ra - LON + 90.0);

            var horiz = CoordinateTransformer.equatorialToHorizontal(ra, dec, gmst, LON, LAT);

            assertThat(horiz.azimuth()).isCloseTo(270.0, offset(2.0));
        }

        @Test
        @DisplayName("Altitude is in [-90, 90] for all inputs")
        void altitudeIsInRange() {
            for (double dec = -90; dec <= 90; dec += 30) {
                for (double ha = 0; ha < 360; ha += 45) {
                    double ra = 180.0;
                    double gmst = CoordinateTransformer.normalise360(ra + LON + ha);
                    var horiz = CoordinateTransformer.equatorialToHorizontal(ra, dec, gmst, LON, LAT);
                    assertThat(horiz.altitude()).isBetween(-90.0, 90.0);
                }
            }
        }

        @Test
        @DisplayName("Azimuth is in [0, 360) for all inputs")
        void azimuthIsInRange() {
            for (double dec = -90; dec <= 90; dec += 30) {
                for (double ha = 0; ha < 360; ha += 45) {
                    double ra = 180.0;
                    double gmst = CoordinateTransformer.normalise360(ra + LON + ha);
                    var horiz = CoordinateTransformer.equatorialToHorizontal(ra, dec, gmst, LON, LAT);
                    assertThat(horiz.azimuth()).isBetween(0.0, 360.0);
                }
            }
        }
    }

    @Nested
    @DisplayName("Angle normalisation")
    class Normalise {

        @Test
        @DisplayName("normalise360 maps 360.0 to 0.0")
        void fullCircle() {
            assertThat(CoordinateTransformer.normalise360(360.0)).isCloseTo(0.0, offset(1e-10));
        }

        @Test
        @DisplayName("normalise360 maps 0.0 to 0.0 (boundary: < 0 is false)")
        void zeroMapsToZero() {
            assertThat(CoordinateTransformer.normalise360(0.0)).isCloseTo(0.0, offset(1e-10));
        }

        @Test
        @DisplayName("normalise360 maps -0.001 to 359.999 (boundary: < 0 is true)")
        void slightlyNegativeMapsToNearThreeSixty() {
            assertThat(CoordinateTransformer.normalise360(-0.001)).isCloseTo(359.999, offset(1e-6));
        }

        @Test
        @DisplayName("normalise360 maps negative angles to positive equivalents")
        void negativeAngle() {
            assertThat(CoordinateTransformer.normalise360(-90.0)).isCloseTo(270.0, offset(1e-10));
        }

        @Test
        @DisplayName("normalise360 maps large angles correctly")
        void largeAngle() {
            assertThat(CoordinateTransformer.normalise360(540.0)).isCloseTo(180.0, offset(1e-10));
        }

        @Test
        @DisplayName("normalise360 preserves angles already in [0, 360)")
        void alreadyNormalisedAngle() {
            assertThat(CoordinateTransformer.normalise360(45.0)).isCloseTo(45.0, offset(1e-10));
            assertThat(CoordinateTransformer.normalise360(180.0)).isCloseTo(180.0, offset(1e-10));
            assertThat(CoordinateTransformer.normalise360(359.9)).isCloseTo(359.9, offset(1e-10));
        }
    }

    @Nested
    @DisplayName("Degree-based trig helpers")
    class TrigHelpers {

        @Test
        @DisplayName("sinDeg(90) is 1.0")
        void sin90() {
            assertThat(CoordinateTransformer.sinDeg(90.0)).isCloseTo(1.0, offset(1e-10));
        }

        @Test
        @DisplayName("cosDeg(0) is 1.0")
        void cos0() {
            assertThat(CoordinateTransformer.cosDeg(0.0)).isCloseTo(1.0, offset(1e-10));
        }

        @Test
        @DisplayName("sinDeg(30) is 0.5")
        void sin30() {
            assertThat(CoordinateTransformer.sinDeg(30.0)).isCloseTo(0.5, offset(1e-10));
        }

        @Test
        @DisplayName("cosDeg(60) is 0.5")
        void cos60() {
            assertThat(CoordinateTransformer.cosDeg(60.0)).isCloseTo(0.5, offset(1e-10));
        }

        @Test
        @DisplayName("sinDeg(ARC_SECOND) is proportional to one arcsecond in radians")
        void sinSmallAngle() {
            // For small angles, sin(x) ≈ x in radians
            double sinVal = CoordinateTransformer.sinDeg(ARC_SECOND_IN_DEGREES);
            assertThat(sinVal).isCloseTo(Math.toRadians(ARC_SECOND_IN_DEGREES), offset(1e-15));
        }
    }
}
