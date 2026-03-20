package com.gregochr.solarutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

/**
 * Verifies LunarCalculator against known almanac values for the Nephel reference locations.
 *
 * <p>Primary location: Durham, UK (54.776°N, 1.575°W).
 * Secondary location: Embleton Bay (55.520°N, 1.636°W).
 * High-latitude edge case: Tromsø (69.649°N, 18.955°E).
 */
class LunarCalculatorTest {

    // Durham Cricket Club area (primary site)
    private static final double DURHAM_LAT = 54.776;
    private static final double DURHAM_LON = -1.575;

    // Embleton Bay, Northumberland (coastal aurora site)
    private static final double EMBLETON_LAT = 55.520;
    private static final double EMBLETON_LON = -1.636;

    // Tromsø, Norway (high-latitude edge case)
    private static final double TROMSO_LAT = 69.649;
    private static final double TROMSO_LON = 18.955;

    // Equator
    private static final double EQUATOR_LAT = 0.0;
    private static final double EQUATOR_LON = 0.0;

    // Reference dates from the 2025 lunar calendar
    // Full moon: 2025-01-13 (verified against USNO)
    private static final ZonedDateTime FULL_MOON_JAN_2025 =
            ZonedDateTime.of(2025, 1, 13, 22, 27, 0, 0, ZoneOffset.UTC);

    // New moon: 2025-01-29
    private static final ZonedDateTime NEW_MOON_JAN_2025 =
            ZonedDateTime.of(2025, 1, 29, 12, 36, 0, 0, ZoneOffset.UTC);

    // First quarter: 2025-01-06
    private static final ZonedDateTime FIRST_QUARTER_JAN_2025 =
            ZonedDateTime.of(2025, 1, 6, 23, 56, 0, 0, ZoneOffset.UTC);

    // Full moon (summer): 2025-06-11
    private static final ZonedDateTime FULL_MOON_JUN_2025 =
            ZonedDateTime.of(2025, 6, 11, 7, 44, 0, 0, ZoneOffset.UTC);

    // Full moon (winter): 2025-12-04
    private static final ZonedDateTime FULL_MOON_DEC_2025 =
            ZonedDateTime.of(2025, 12, 4, 23, 14, 0, 0, ZoneOffset.UTC);

    private static final double ILLUMINATION_TOLERANCE = 0.05; // 5%
    private static final double ALTITUDE_TOLERANCE_DEG = 2.0;

    private LunarCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LunarCalculator();
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("calculate() throws IllegalArgumentException for latitude out of range")
    void invalidLatitudeThrows() {
        assertThatThrownBy(() -> calculator.calculate(FULL_MOON_JAN_2025, 91.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }

    @Test
    @DisplayName("calculate() throws IllegalArgumentException for longitude out of range")
    void invalidLongitudeThrows() {
        assertThatThrownBy(() -> calculator.calculate(FULL_MOON_JAN_2025, 0.0, 181.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");
    }

    // -------------------------------------------------------------------------
    // Illumination
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Illumination")
    class Illumination {

        @Test
        @DisplayName("Full moon (2025-01-13) illumination is approximately 100% from Durham")
        void fullMoonNearMaxIllumination() {
            var pos = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.illumination()).isGreaterThan(1.0 - ILLUMINATION_TOLERANCE);
        }

        @Test
        @DisplayName("New moon (2025-01-29) illumination is approximately 0% from Durham")
        void newMoonNearZeroIllumination() {
            var pos = calculator.calculate(NEW_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.illumination()).isLessThan(ILLUMINATION_TOLERANCE);
        }

        @Test
        @DisplayName("First quarter (2025-01-06) illumination is approximately 50% from Durham")
        void firstQuarterNearHalfIllumination() {
            var pos = calculator.calculate(FIRST_QUARTER_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.illumination()).isCloseTo(0.5, offset(0.10));
        }

        @Test
        @DisplayName("Illumination is always in [0.0, 1.0] across multiple dates")
        void illuminationAlwaysInRange() {
            ZonedDateTime base = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
            for (int day = 0; day < 30; day++) {
                var pos = calculator.calculate(base.plusDays(day), DURHAM_LAT, DURHAM_LON);
                assertThat(pos.illumination())
                        .as("Day %d of lunation", day)
                        .isBetween(0.0, 1.0);
            }
        }

        @Test
        @DisplayName("illuminationPercent() is illumination × 100")
        void illuminationPercentIsScaledCorrectly() {
            var pos = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.illuminationPercent()).isCloseTo(pos.illumination() * 100.0, offset(0.001));
        }
    }

    // -------------------------------------------------------------------------
    // Phase
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Phase")
    class Phase {

        @Test
        @DisplayName("Full moon date (2025-01-13) returns FULL_MOON phase")
        void knownFullMoonReturnsFullMoonPhase() {
            var pos = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.phase()).isEqualTo(LunarPhase.FULL_MOON);
        }

        @Test
        @DisplayName("New moon date (2025-01-29) returns NEW_MOON phase")
        void knownNewMoonReturnsNewMoonPhase() {
            var pos = calculator.calculate(NEW_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.phase()).isEqualTo(LunarPhase.NEW_MOON);
        }

        @Test
        @DisplayName("First quarter date (2025-01-06) returns FIRST_QUARTER phase")
        void knownFirstQuarterReturnsFirstQuarterPhase() {
            var pos = calculator.calculate(FIRST_QUARTER_JAN_2025, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.phase()).isEqualTo(LunarPhase.FIRST_QUARTER);
        }

        @Test
        @DisplayName("All 8 lunar phases are reachable across a 30-day lunation")
        void allEightPhasesReachableAcrossLunation() {
            ZonedDateTime start = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
            var phases = new java.util.HashSet<LunarPhase>();

            for (int day = 0; day < 30; day++) {
                var pos = calculator.calculate(start.plusDays(day), DURHAM_LAT, DURHAM_LON);
                phases.add(pos.phase());
            }

            assertThat(phases).containsExactlyInAnyOrder(LunarPhase.values());
        }

        @Test
        @DisplayName("Lunar phases advance in correct order across a lunation")
        void phaseProgressionIsMonotonic() {
            // Track ordinal of each unique phase in sequence; it should never decrease
            // (wrapping from WANING_CRESCENT back to NEW_MOON is expected at cycle end)
            ZonedDateTime start = NEW_MOON_JAN_2025;
            LunarPhase prev = null;

            for (int day = 0; day < 29; day++) {
                LunarPhase current = calculator.calculate(start.plusDays(day), EQUATOR_LAT, EQUATOR_LON).phase();
                if (prev != null && prev != current) {
                    // Phase should only advance forward, or wrap from WANING_CRESCENT to NEW_MOON
                    int prevOrdinal = prev.ordinal();
                    int currOrdinal = current.ordinal();
                    boolean advanced = currOrdinal > prevOrdinal;
                    boolean wrapped = prev == LunarPhase.WANING_CRESCENT && current == LunarPhase.NEW_MOON;
                    assertThat(advanced || wrapped)
                            .as("Phase went from %s (ordinal %d) to %s (ordinal %d) on day %d",
                                    prev, prevOrdinal, current, currOrdinal, day)
                            .isTrue();
                }
                prev = current;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Position
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Position")
    class Position {

        @Test
        @DisplayName("Winter full moon (2025-12-04) is at high altitude from Durham at local midnight")
        void winterFullMoonHighAltitudeAtMidnight() {
            // December full moon has high declination (opposite to low winter sun)
            // At midnight from Durham it should be well above the horizon
            ZonedDateTime midnight = ZonedDateTime.of(2025, 12, 5, 0, 0, 0, 0, ZoneOffset.UTC);
            var pos = calculator.calculate(midnight, DURHAM_LAT, DURHAM_LON);

            assertThat(pos.altitude()).isGreaterThan(20.0);
        }

        @Test
        @DisplayName("Summer full moon (2025-06-11) is at lower altitude at local midnight than winter full moon")
        void summerFullMoonLowerThanWinterFullMoon() {
            ZonedDateTime summerMidnight = ZonedDateTime.of(2025, 6, 11, 23, 0, 0, 0, ZoneOffset.UTC);
            ZonedDateTime winterMidnight = ZonedDateTime.of(2025, 12, 5, 0, 0, 0, 0, ZoneOffset.UTC);

            double summerAlt = calculator.calculate(summerMidnight, DURHAM_LAT, DURHAM_LON).altitude();
            double winterAlt = calculator.calculate(winterMidnight, DURHAM_LAT, DURHAM_LON).altitude();

            assertThat(summerAlt).isLessThan(winterAlt);
        }

        @Test
        @DisplayName("Azimuth is always in [0, 360) for all test dates")
        void azimuthAlwaysInRange() {
            ZonedDateTime base = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
            for (int day = 0; day < 30; day++) {
                var pos = calculator.calculate(base.plusDays(day), DURHAM_LAT, DURHAM_LON);
                assertThat(pos.azimuth())
                        .as("Day %d azimuth", day)
                        .isBetween(0.0, 360.0);
            }
        }

        @Test
        @DisplayName("Position at Embleton Bay gives physically plausible altitude and azimuth")
        void embletonBayPositionIsPlausible() {
            var pos = calculator.calculate(FULL_MOON_JAN_2025, EMBLETON_LAT, EMBLETON_LON);

            assertThat(pos.altitude()).isBetween(-90.0, 90.0);
            assertThat(pos.azimuth()).isBetween(0.0, 360.0);
            assertThat(pos.illumination()).isGreaterThan(0.9); // Full moon
        }

        @Test
        @DisplayName("New moon at night is below or near the horizon at Durham")
        void newMoonBelowHorizonAtNight() {
            // New moon is near the sun, so it sets shortly after the sun
            // At midnight local time around new moon it should be below the horizon
            ZonedDateTime newMoonMidnight = ZonedDateTime.of(2025, 1, 29, 23, 0, 0, 0, ZoneOffset.UTC);
            var pos = calculator.calculate(newMoonMidnight, DURHAM_LAT, DURHAM_LON);

            // New moon at midnight should be well below the horizon
            assertThat(pos.altitude()).isLessThan(10.0);
        }
    }

    // -------------------------------------------------------------------------
    // Distance
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Distance")
    class Distance {

        @Test
        @DisplayName("Lunar distance is in the physically plausible range (356,000–407,000 km)")
        void distanceInPhysicallyPlausibleRange() {
            ZonedDateTime base = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
            for (int day = 0; day < 29; day++) {
                var pos = calculator.calculate(base.plusDays(day), DURHAM_LAT, DURHAM_LON);
                assertThat(pos.distanceKm())
                        .as("Distance on day %d", day)
                        .isBetween(356000.0, 407000.0);
            }
        }

        @Test
        @DisplayName("Perigee distance is less than apogee distance across a lunation")
        void perigeeIsCloserThanApogee() {
            ZonedDateTime base = ZonedDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
            double minDist = Double.MAX_VALUE;
            double maxDist = Double.MIN_VALUE;

            for (int day = 0; day < 29; day++) {
                double dist = calculator.calculate(base.plusDays(day), DURHAM_LAT, DURHAM_LON).distanceKm();
                minDist = Math.min(minDist, dist);
                maxDist = Math.max(maxDist, dist);
            }

            // Perigee is typically ~50,000 km closer than apogee
            assertThat(maxDist - minDist).isGreaterThan(20000.0);
        }
    }

    // -------------------------------------------------------------------------
    // High-latitude edge cases
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("High-latitude edge cases")
    class HighLatitude {

        @Test
        @DisplayName("Calculations for Tromsø (69.6°N) return physically plausible values")
        void tromsoPolarLatitudeReturnsPlausibleValues() {
            var pos = calculator.calculate(FULL_MOON_JAN_2025, TROMSO_LAT, TROMSO_LON);

            assertThat(pos.altitude()).isBetween(-90.0, 90.0);
            assertThat(pos.azimuth()).isBetween(0.0, 360.0);
            assertThat(pos.illumination()).isBetween(0.0, 1.0);
        }
    }

    // -------------------------------------------------------------------------
    // Cross-location consistency
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Cross-location consistency")
    class CrossLocation {

        @Test
        @DisplayName("Durham and Embleton Bay give similar illumination (same instant)")
        void nearbyLocationsHaveSameIllumination() {
            var durham = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);
            var embleton = calculator.calculate(FULL_MOON_JAN_2025, EMBLETON_LAT, EMBLETON_LON);

            // Illumination is geocentric — should be nearly identical from nearby locations
            assertThat(durham.illumination()).isCloseTo(embleton.illumination(), offset(0.01));
        }

        @Test
        @DisplayName("Durham and Embleton Bay give the same phase (same instant)")
        void nearbyLocationsHaveSamePhase() {
            var durham = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);
            var embleton = calculator.calculate(FULL_MOON_JAN_2025, EMBLETON_LAT, EMBLETON_LON);

            assertThat(durham.phase()).isEqualTo(embleton.phase());
        }

        @Test
        @DisplayName("Locations 90° of longitude apart give different altitudes at the same instant")
        void locationsNinetyDegreesApartHaveDifferentAltitudes() {
            // 90° longitude difference = 6 hours of hour angle — guarantees a large altitude difference
            var west = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON);
            var east = calculator.calculate(FULL_MOON_JAN_2025, DURHAM_LAT, DURHAM_LON + 90.0);

            // 90° longitude = 6h hour angle, so altitudes must differ by many degrees
            assertThat(Math.abs(west.altitude() - east.altitude())).isGreaterThan(10.0);
        }
    }

    // -------------------------------------------------------------------------
    // Lunar phase display names
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("All LunarPhase enum values have non-blank display names")
    void allPhasesHaveDisplayNames() {
        for (LunarPhase phase : LunarPhase.values()) {
            assertThat(phase.displayName())
                    .as("Display name for %s", phase)
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("LunarPhase.FULL_MOON display name is 'Full Moon'")
    void fullMoonDisplayName() {
        assertThat(LunarPhase.FULL_MOON.displayName()).isEqualTo("Full Moon");
    }

    @Test
    @DisplayName("LunarPhase.WAXING_CRESCENT display name is 'Waxing Crescent'")
    void waxingCrescentDisplayName() {
        assertThat(LunarPhase.WAXING_CRESCENT.displayName()).isEqualTo("Waxing Crescent");
    }

    // -------------------------------------------------------------------------
    // Time zone handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Results are identical regardless of input time zone when instant is the same")
    void resultIsTimeZoneIndependent() {
        ZonedDateTime utcTime = ZonedDateTime.of(2025, 1, 13, 22, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime londonTime = utcTime.withZoneSameInstant(ZoneId.of("Europe/London"));

        var utcPos = calculator.calculate(utcTime, DURHAM_LAT, DURHAM_LON);
        var londonPos = calculator.calculate(londonTime, DURHAM_LAT, DURHAM_LON);

        assertThat(utcPos.altitude()).isCloseTo(londonPos.altitude(), offset(0.001));
        assertThat(utcPos.azimuth()).isCloseTo(londonPos.azimuth(), offset(0.001));
        assertThat(utcPos.illumination()).isCloseTo(londonPos.illumination(), offset(0.001));
        assertThat(utcPos.phase()).isEqualTo(londonPos.phase());
    }
}
