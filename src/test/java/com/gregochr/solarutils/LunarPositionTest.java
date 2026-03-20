package com.gregochr.solarutils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * Verifies LunarPosition derived methods using directly constructed records.
 * Tests the scoring logic in isolation from the calculator.
 */
class LunarPositionTest {

    @Nested
    @DisplayName("isAboveHorizon()")
    class IsAboveHorizon {

        @Test
        @DisplayName("Positive altitude is above the horizon")
        void positiveAltitudeIsAboveHorizon() {
            var pos = new LunarPosition(15.0, 180.0, 0.8, LunarPhase.WAXING_GIBBOUS, 384400.0);

            assertThat(pos.isAboveHorizon()).isTrue();
        }

        @Test
        @DisplayName("Negative altitude is below the horizon")
        void negativeAltitudeIsBelowHorizon() {
            var pos = new LunarPosition(-10.0, 180.0, 0.8, LunarPhase.WAXING_GIBBOUS, 384400.0);

            assertThat(pos.isAboveHorizon()).isFalse();
        }

        @Test
        @DisplayName("Zero altitude is not above the horizon")
        void zeroAltitudeIsNotAboveHorizon() {
            var pos = new LunarPosition(0.0, 90.0, 0.5, LunarPhase.FIRST_QUARTER, 384400.0);

            assertThat(pos.isAboveHorizon()).isFalse();
        }
    }

    @Nested
    @DisplayName("isInNorthernSky()")
    class IsInNorthernSky {

        @Test
        @DisplayName("Azimuth 0° (due North) is in the northern sky")
        void dueNorthIsNorthernSky() {
            var pos = new LunarPosition(10.0, 0.0, 0.5, LunarPhase.FIRST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isTrue();
        }

        @Test
        @DisplayName("Azimuth 45° (NE) is in the northern sky")
        void northEastIsNorthernSky() {
            var pos = new LunarPosition(10.0, 45.0, 0.5, LunarPhase.FIRST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isTrue();
        }

        @Test
        @DisplayName("Azimuth 90° (due East) is in the northern sky boundary")
        void dueEastIsNorthernSkyBoundary() {
            var pos = new LunarPosition(10.0, 90.0, 0.5, LunarPhase.FIRST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isTrue();
        }

        @Test
        @DisplayName("Azimuth 180° (due South) is not in the northern sky")
        void dueSouthIsNotNorthernSky() {
            var pos = new LunarPosition(40.0, 180.0, 1.0, LunarPhase.FULL_MOON, 384400.0);

            assertThat(pos.isInNorthernSky()).isFalse();
        }

        @Test
        @DisplayName("Azimuth 270° (due West) is in the northern sky")
        void dueWestIsNorthernSky() {
            var pos = new LunarPosition(10.0, 270.0, 0.5, LunarPhase.LAST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isTrue();
        }

        @Test
        @DisplayName("Azimuth 315° (NW) is in the northern sky")
        void northWestIsNorthernSky() {
            var pos = new LunarPosition(10.0, 315.0, 0.5, LunarPhase.LAST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isTrue();
        }

        @Test
        @DisplayName("Azimuth 135° (SE) is not in the northern sky")
        void southEastIsNotNorthernSky() {
            var pos = new LunarPosition(10.0, 135.0, 0.5, LunarPhase.FIRST_QUARTER, 384400.0);

            assertThat(pos.isInNorthernSky()).isFalse();
        }
    }

    @Nested
    @DisplayName("illuminationPercent()")
    class IlluminationPercent {

        @Test
        @DisplayName("illuminationPercent is illumination × 100")
        void percentIsScaledFraction() {
            var pos = new LunarPosition(10.0, 180.0, 0.75, LunarPhase.WAXING_GIBBOUS, 384400.0);

            assertThat(pos.illuminationPercent()).isCloseTo(75.0, offset(0.001));
        }

        @Test
        @DisplayName("Full moon illuminationPercent is 100")
        void fullMoonIs100Percent() {
            var pos = new LunarPosition(40.0, 180.0, 1.0, LunarPhase.FULL_MOON, 384400.0);

            assertThat(pos.illuminationPercent()).isCloseTo(100.0, offset(0.001));
        }

        @Test
        @DisplayName("New moon illuminationPercent is 0")
        void newMoonIsZeroPercent() {
            var pos = new LunarPosition(-10.0, 180.0, 0.0, LunarPhase.NEW_MOON, 384400.0);

            assertThat(pos.illuminationPercent()).isCloseTo(0.0, offset(0.001));
        }
    }

    @Nested
    @DisplayName("auroraPenalty()")
    class AuroraPenalty {

        @Test
        @DisplayName("Moon below horizon — auroraPenalty() returns 0.0")
        void belowHorizonPenaltyIsZero() {
            var pos = new LunarPosition(-5.0, 180.0, 1.0, LunarPhase.FULL_MOON, 384400.0);

            assertThat(pos.auroraPenalty()).isCloseTo(0.0, offset(1e-10));
        }

        @Test
        @DisplayName("New moon above horizon — auroraPenalty() is less than 0.1")
        void newMoonAboveHorizonHasNegligiblePenalty() {
            // New moon: very low illumination even when above horizon
            var pos = new LunarPosition(20.0, 30.0, 0.02, LunarPhase.NEW_MOON, 384400.0);

            assertThat(pos.auroraPenalty()).isLessThan(0.1);
        }

        @Test
        @DisplayName("Full moon high in the northern sky — auroraPenalty() is greater than 0.5")
        void fullMoonHighInNorthHasHighPenalty() {
            // altitude=60°, azimuth=30° (NNE), illumination=1.0
            // Expected: 1.0 * sin(60°) * 1.0 = 0.866
            var pos = new LunarPosition(60.0, 30.0, 1.0, LunarPhase.FULL_MOON, 384400.0);

            assertThat(pos.auroraPenalty()).isGreaterThan(0.5);
        }

        @Test
        @DisplayName("Full moon in southern sky — auroraPenalty() applies 0.4 direction factor")
        void fullMoonInSouthHasReducedPenaltyByFourTenths() {
            double altitude = 60.0;
            var northPos = new LunarPosition(altitude, 30.0, 1.0, LunarPhase.FULL_MOON, 384400.0);
            var southPos = new LunarPosition(altitude, 180.0, 1.0, LunarPhase.FULL_MOON, 384400.0);

            double northPenalty = northPos.auroraPenalty();
            double southPenalty = southPos.auroraPenalty();

            // Southern penalty should be 40% of northern penalty
            assertThat(southPenalty).isCloseTo(northPenalty * 0.4, offset(0.001));
        }

        @Test
        @DisplayName("Crescent moon — auroraPenalty() is negligible (< 0.1)")
        void crescentMoonHasNegligiblePenalty() {
            // Waxing crescent: about 20% illuminated, moderate altitude
            var pos = new LunarPosition(25.0, 250.0, 0.20, LunarPhase.WAXING_CRESCENT, 384400.0);

            assertThat(pos.auroraPenalty()).isLessThan(0.1);
        }

        @Test
        @DisplayName("auroraPenalty() is in [0.0, 1.0] for physically plausible inputs")
        void penaltyIsAlwaysInUnitRange() {
            double[] altitudes = {-10.0, 0.0, 30.0, 60.0, 89.9};
            double[] azimuths = {0.0, 90.0, 180.0, 270.0};
            double[] illuminations = {0.0, 0.25, 0.5, 0.75, 1.0};

            for (double alt : altitudes) {
                for (double az : azimuths) {
                    for (double illum : illuminations) {
                        var pos = new LunarPosition(alt, az, illum, LunarPhase.FULL_MOON, 384400.0);
                        assertThat(pos.auroraPenalty())
                                .as("altitude=%.1f, azimuth=%.1f, illumination=%.2f", alt, az, illum)
                                .isBetween(0.0, 1.0);
                    }
                }
            }
        }

        @Test
        @DisplayName("Waxing gibbous in northern sky at moderate altitude has moderate penalty")
        void gibbousMoonInNorthHasModeratePenalty() {
            // 75% illumination, altitude=35°, northern sky
            var pos = new LunarPosition(35.0, 20.0, 0.75, LunarPhase.WAXING_GIBBOUS, 384400.0);

            // Expected: 0.75 * sin(35°) * 1.0 = 0.75 * 0.574 = 0.43
            assertThat(pos.auroraPenalty()).isGreaterThan(0.2).isLessThan(0.8);
        }
    }
}
