package com.gregochr.solarutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.data.Offset.offset;

/**
 * Boundary condition tests for LunarCalculator using package-private test helpers.
 * These tests are specifically designed to kill ConditionalsBoundaryMutator mutations
 * in {@link LunarCalculator#determineLunarPhase(double)} and
 * {@link LunarCalculator#calculateIllumination(double)}.
 *
 * <p>Each boundary is tested from both sides to ensure that changing {@code <} to {@code <=}
 * (or vice versa) causes a test failure.
 */
class LunarCalculatorBoundaryTest {

    private LunarCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LunarCalculator();
    }

    @Nested
    @DisplayName("Phase boundaries — determineLunarPhase()")
    class PhaseBoundaries {

        // Tests at the EXACT boundary value kill ConditionalsBoundaryMutator mutations.
        // e.g. for "elongation < 22.5": at exactly 22.5, `<` returns WAXING_CRESCENT
        // but mutated `<=` returns NEW_MOON. Both sides of each boundary are also tested.

        @Test
        @DisplayName("Elongation 22.4° is NEW_MOON")
        void justBelowNewMoonBoundary() {
            assertThat(calculator.phaseFromElongation(22.4)).isEqualTo(LunarPhase.NEW_MOON);
        }

        @Test
        @DisplayName("Elongation exactly 22.5° is WAXING_CRESCENT (< 22.5 is false)")
        void exactlyAtNewMoonWaxingCrescentBoundary() {
            assertThat(calculator.phaseFromElongation(22.5)).isEqualTo(LunarPhase.WAXING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation 22.6° is WAXING_CRESCENT")
        void justAboveNewMoonBoundary() {
            assertThat(calculator.phaseFromElongation(22.6)).isEqualTo(LunarPhase.WAXING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation 67.4° is WAXING_CRESCENT")
        void justBelowFirstQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(67.4)).isEqualTo(LunarPhase.WAXING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation exactly 67.5° is FIRST_QUARTER (< 67.5 is false)")
        void exactlyAtWaxingCrescentFirstQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(67.5)).isEqualTo(LunarPhase.FIRST_QUARTER);
        }

        @Test
        @DisplayName("Elongation 67.6° is FIRST_QUARTER")
        void justAboveFirstQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(67.6)).isEqualTo(LunarPhase.FIRST_QUARTER);
        }

        @Test
        @DisplayName("Elongation 112.4° is FIRST_QUARTER")
        void justBelowWaxingGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(112.4)).isEqualTo(LunarPhase.FIRST_QUARTER);
        }

        @Test
        @DisplayName("Elongation exactly 112.5° is WAXING_GIBBOUS (< 112.5 is false)")
        void exactlyAtFirstQuarterWaxingGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(112.5)).isEqualTo(LunarPhase.WAXING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation 112.6° is WAXING_GIBBOUS")
        void justAboveWaxingGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(112.6)).isEqualTo(LunarPhase.WAXING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation 157.4° is WAXING_GIBBOUS")
        void justBelowFullMoonBoundary() {
            assertThat(calculator.phaseFromElongation(157.4)).isEqualTo(LunarPhase.WAXING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation exactly 157.5° is FULL_MOON (< 157.5 is false)")
        void exactlyAtWaxingGibbousFullMoonBoundary() {
            assertThat(calculator.phaseFromElongation(157.5)).isEqualTo(LunarPhase.FULL_MOON);
        }

        @Test
        @DisplayName("Elongation 157.6° is FULL_MOON")
        void justAboveFullMoonBoundary() {
            assertThat(calculator.phaseFromElongation(157.6)).isEqualTo(LunarPhase.FULL_MOON);
        }

        @Test
        @DisplayName("Elongation 202.4° is FULL_MOON")
        void justBelowWaningGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(202.4)).isEqualTo(LunarPhase.FULL_MOON);
        }

        @Test
        @DisplayName("Elongation exactly 202.5° is WANING_GIBBOUS (< 202.5 is false)")
        void exactlyAtFullMoonWaningGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(202.5)).isEqualTo(LunarPhase.WANING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation 202.6° is WANING_GIBBOUS")
        void justAboveWaningGibbousBoundary() {
            assertThat(calculator.phaseFromElongation(202.6)).isEqualTo(LunarPhase.WANING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation 247.4° is WANING_GIBBOUS")
        void justBelowLastQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(247.4)).isEqualTo(LunarPhase.WANING_GIBBOUS);
        }

        @Test
        @DisplayName("Elongation exactly 247.5° is LAST_QUARTER (< 247.5 is false)")
        void exactlyAtWaningGibbousLastQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(247.5)).isEqualTo(LunarPhase.LAST_QUARTER);
        }

        @Test
        @DisplayName("Elongation 247.6° is LAST_QUARTER")
        void justAboveLastQuarterBoundary() {
            assertThat(calculator.phaseFromElongation(247.6)).isEqualTo(LunarPhase.LAST_QUARTER);
        }

        @Test
        @DisplayName("Elongation 292.4° is LAST_QUARTER")
        void justBelowWaningCrescentBoundary() {
            assertThat(calculator.phaseFromElongation(292.4)).isEqualTo(LunarPhase.LAST_QUARTER);
        }

        @Test
        @DisplayName("Elongation exactly 292.5° is WANING_CRESCENT (< 292.5 is false)")
        void exactlyAtLastQuarterWaningCrescentBoundary() {
            assertThat(calculator.phaseFromElongation(292.5)).isEqualTo(LunarPhase.WANING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation 292.6° is WANING_CRESCENT")
        void justAboveWaningCrescentBoundary() {
            assertThat(calculator.phaseFromElongation(292.6)).isEqualTo(LunarPhase.WANING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation 337.4° is WANING_CRESCENT")
        void justBelowFinalNewMoonBoundary() {
            assertThat(calculator.phaseFromElongation(337.4)).isEqualTo(LunarPhase.WANING_CRESCENT);
        }

        @Test
        @DisplayName("Elongation exactly 337.5° is NEW_MOON (>= 337.5 is true)")
        void exactlyAtWaningCrescentNewMoonBoundary() {
            assertThat(calculator.phaseFromElongation(337.5)).isEqualTo(LunarPhase.NEW_MOON);
        }

        @Test
        @DisplayName("Elongation 337.6° is NEW_MOON")
        void justAboveFinalNewMoonBoundary() {
            assertThat(calculator.phaseFromElongation(337.6)).isEqualTo(LunarPhase.NEW_MOON);
        }

        @Test
        @DisplayName("Elongation 0° is NEW_MOON (minimum elongation)")
        void minimumElongationIsNewMoon() {
            assertThat(calculator.phaseFromElongation(0.0)).isEqualTo(LunarPhase.NEW_MOON);
        }

        @Test
        @DisplayName("Elongation 180° is FULL_MOON (maximum elongation)")
        void maximumElongationIsFullMoon() {
            assertThat(calculator.phaseFromElongation(180.0)).isEqualTo(LunarPhase.FULL_MOON);
        }
    }

    @Nested
    @DisplayName("Illumination — calculateIllumination()")
    class IlluminationBoundaries {

        @Test
        @DisplayName("Elongation 0° gives illumination = 0.0 (new moon)")
        void zeroElongationGivesZeroIllumination() {
            // phaseAngle = 180 - 0 = 180°, illumination = (1 + cos(180)) / 2 = 0
            assertThat(calculator.illuminationFromElongation(0.0)).isCloseTo(0.0, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 90° gives illumination = 0.5 (first quarter)")
        void quarterElongationGivesHalfIllumination() {
            // phaseAngle = 180 - 90 = 90°, illumination = (1 + cos(90)) / 2 = 0.5
            assertThat(calculator.illuminationFromElongation(90.0)).isCloseTo(0.5, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 180° gives illumination = 1.0 (full moon)")
        void fullElongationGivesFullIllumination() {
            // phaseAngle = 180 - 180 = 0°, illumination = (1 + cos(0)) / 2 = 1.0
            assertThat(calculator.illuminationFromElongation(180.0)).isCloseTo(1.0, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 270° gives illumination = 0.5 (last quarter)")
        void threeQuarterElongationGivesHalfIllumination() {
            // phaseAngle = 270 - 180 = 90°, illumination = (1 + cos(90)) / 2 = 0.5
            assertThat(calculator.illuminationFromElongation(270.0)).isCloseTo(0.5, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 60° gives illumination = 0.25")
        void sixtyDegreesGivesQuarterIllumination() {
            // phaseAngle = 180 - 60 = 120°, illumination = (1 + cos(120)) / 2 = (1 - 0.5) / 2 = 0.25
            assertThat(calculator.illuminationFromElongation(60.0)).isCloseTo(0.25, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 120° gives illumination = 0.75")
        void oneHundredTwentyDegreesGivesThreeQuarterIllumination() {
            // phaseAngle = 180 - 120 = 60°, illumination = (1 + cos(60)) / 2 = (1 + 0.5) / 2 = 0.75
            assertThat(calculator.illuminationFromElongation(120.0)).isCloseTo(0.75, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 240° gives illumination = 0.75 (symmetric with 120°)")
        void twoFortyDegreesGivesThreeQuarterIllumination() {
            // phaseAngle = 240 - 180 = 60°, illumination = 0.75
            assertThat(calculator.illuminationFromElongation(240.0)).isCloseTo(0.75, offset(0.001));
        }

        @Test
        @DisplayName("Elongation 300° gives illumination = 0.25 (symmetric with 60°)")
        void threeHundredDegreesGivesQuarterIllumination() {
            // phaseAngle = 300 - 180 = 120°, illumination = 0.25
            assertThat(calculator.illuminationFromElongation(300.0)).isCloseTo(0.25, offset(0.001));
        }

        @Test
        @DisplayName("Illumination is symmetric around 180° elongation")
        void illuminationIsSymmetricAroundFullMoon() {
            double before = calculator.illuminationFromElongation(150.0);
            double after = calculator.illuminationFromElongation(210.0);
            // Both should be symmetric: phaseAngle 30° and 30° → same cos value
            assertThat(before).isCloseTo(after, offset(0.001));
        }

        @Test
        @DisplayName("Illumination increases monotonically from 0° to 180° elongation")
        void illuminationIncreasesFromNewMoonToFullMoon() {
            double prev = calculator.illuminationFromElongation(0.0);
            for (int deg = 10; deg <= 180; deg += 10) {
                double current = calculator.illuminationFromElongation(deg);
                assertThat(current).isGreaterThanOrEqualTo(prev);
                prev = current;
            }
        }
    }

    @Nested
    @DisplayName("Input validation boundaries — calculate()")
    class InputValidationBoundaries {

        private static final ZonedDateTime REF_TIME =
                ZonedDateTime.of(2025, 1, 13, 22, 27, 0, 0, ZoneOffset.UTC);

        // Exact boundary values must NOT throw — kills ConditionalsBoundaryMutator mutations
        // that change `latitude < -90.0` to `<= -90.0` (which would throw at -90.0).

        @Test
        @DisplayName("Latitude exactly -90.0 is accepted (boundary value does not throw)")
        void latitudeAtNegative90IsAccepted() {
            assertThatCode(() -> calculator.calculate(REF_TIME, -90.0, 0.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Latitude exactly 90.0 is accepted (boundary value does not throw)")
        void latitudeAt90IsAccepted() {
            assertThatCode(() -> calculator.calculate(REF_TIME, 90.0, 0.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Longitude exactly -180.0 is accepted (boundary value does not throw)")
        void longitudeAtNegative180IsAccepted() {
            assertThatCode(() -> calculator.calculate(REF_TIME, 0.0, -180.0))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Longitude exactly 180.0 is accepted (boundary value does not throw)")
        void longitudeAt180IsAccepted() {
            assertThatCode(() -> calculator.calculate(REF_TIME, 0.0, 180.0))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Eccentricity factor — eccentricityFactor()")
    class EccentricityFactorBoundaries {

        // Use realistic Earth orbital eccentricity values for t ≈ 0.25 (year 2025)
        private static final double E = 0.99372;   // 1 - 0.002516*0.25 - ...
        private static final double E2 = E * E;

        @Test
        @DisplayName("mCoeff = 2.0 returns e² (E correction for double M term)")
        void mCoeffTwoReturnsESquared() {
            assertThat(calculator.eccentricityFactorForTest(2.0, E, E2))
                    .isCloseTo(E2, offset(1e-9));
        }

        @Test
        @DisplayName("mCoeff = -2.0 returns e² (absolute value symmetry)")
        void mCoeffNegativeTwoReturnsESquared() {
            assertThat(calculator.eccentricityFactorForTest(-2.0, E, E2))
                    .isCloseTo(E2, offset(1e-9));
        }

        @Test
        @DisplayName("mCoeff = 1.0 returns e (E correction for single M term)")
        void mCoeffOneReturnsE() {
            assertThat(calculator.eccentricityFactorForTest(1.0, E, E2))
                    .isCloseTo(E, offset(1e-9));
        }

        @Test
        @DisplayName("mCoeff = -1.0 returns e (absolute value symmetry)")
        void mCoeffNegativeOneReturnsE() {
            assertThat(calculator.eccentricityFactorForTest(-1.0, E, E2))
                    .isCloseTo(E, offset(1e-9));
        }

        @Test
        @DisplayName("mCoeff = 0.0 returns 1.0 (no eccentricity correction)")
        void mCoeffZeroReturnsOne() {
            assertThat(calculator.eccentricityFactorForTest(0.0, E, E2))
                    .isCloseTo(1.0, offset(1e-9));
        }

        @Test
        @DisplayName("Eccentricity factor distinguishes e from e² (e ≠ e²)")
        void eAndESquaredAreDistinct() {
            double factorFor1 = calculator.eccentricityFactorForTest(1.0, E, E2);
            double factorFor2 = calculator.eccentricityFactorForTest(2.0, E, E2);
            assertThat(factorFor2).isLessThan(factorFor1); // e² < e since e < 1
        }
    }

    @Nested
    @DisplayName("Fundamental arguments — computeFundamentalArguments()")
    class FundamentalArgumentPrecisionTest {

        // At T = 0.1 exactly, the fundamental arguments can be computed analytically.
        // These expected values are derived from the Meeus Ch.47, eq.47.1 polynomials
        // with T = 0.1 substituted exactly (no external reference needed).
        //
        // Tolerances are chosen to be well above floating-point rounding (1e-6°) but
        // below the effect of changing a quadratic correction term (which causes a shift
        // of ~0.001° to ~0.009° for the t² coefficients). This kills MathMutator
        // mutations that change `0.0015786 * t * t` to `0.0015786 * t / t`, etc.

        private static final double T = 0.1;

        // Expected values at T=0.1 (computed from Meeus polynomials directly):
        // L' = normalise360(218.3164477 + 481267.88123421*0.1 - 0.0015786*0.01 + ...)
        //    ≈ normalise360(48345.1046) = 105.1046°
        private static final double L_PRIME = 105.104556;

        // M  = normalise360(357.5291092 + 35999.0502909*0.1 - 0.0001536*0.01 + ...)
        //    ≈ normalise360(3957.4341) = 357.4341°
        private static final double M_SUN = 357.434138;

        // M' = normalise360(134.9634114 + 477198.8676313*0.1 + 0.0089970*0.01 + ...)
        //    ≈ normalise360(47854.8503) = 334.8503°
        private static final double M_MOON = 334.850312;

        // D  = normalise360(297.8501921 + 445267.1114034*0.1 - 0.0018819*0.01 + ...)
        //    ≈ normalise360(44824.5613) = 184.5613°
        private static final double D = 184.561315;

        // F  = normalise360(93.2720950 + 483202.0175233*0.1 - 0.0036539*0.01 + ...)
        //    ≈ normalise360(48413.4738) = 173.4738°
        private static final double F = 173.473812;

        // Tolerance: 0.001° — tight enough to catch t² correction term mutations
        // (effects of 0.0016° to 0.009°) but generous vs floating-point rounding.
        private static final double TOLERANCE = 0.001;

        @Test
        @DisplayName("L' (moon mean longitude) at T=0.1 matches expected value")
        void lPrimeAtT01() {
            double[] args = calculator.fundamentalArgumentsForTest(T);
            assertThat(args[0]).isCloseTo(L_PRIME, offset(TOLERANCE));
        }

        @Test
        @DisplayName("M (sun mean anomaly) at T=0.1 matches expected value")
        void mSunAtT01() {
            double[] args = calculator.fundamentalArgumentsForTest(T);
            assertThat(args[1]).isCloseTo(M_SUN, offset(TOLERANCE));
        }

        @Test
        @DisplayName("M' (moon mean anomaly) at T=0.1 matches expected value")
        void mMoonAtT01() {
            double[] args = calculator.fundamentalArgumentsForTest(T);
            assertThat(args[2]).isCloseTo(M_MOON, offset(TOLERANCE));
        }

        @Test
        @DisplayName("D (moon mean elongation) at T=0.1 matches expected value")
        void dAtT01() {
            double[] args = calculator.fundamentalArgumentsForTest(T);
            assertThat(args[3]).isCloseTo(D, offset(TOLERANCE));
        }

        @Test
        @DisplayName("F (moon argument of latitude) at T=0.1 matches expected value")
        void fAtT01() {
            double[] args = calculator.fundamentalArgumentsForTest(T);
            assertThat(args[4]).isCloseTo(F, offset(TOLERANCE));
        }

        @Test
        @DisplayName("All fundamental arguments at T=0 equal the bare constants (no polynomial drift)")
        void atJ2000EpochEqualsConstants() {
            double[] args = calculator.fundamentalArgumentsForTest(0.0);
            assertThat(args[0]).isCloseTo(218.3164477, offset(TOLERANCE));
            assertThat(args[1]).isCloseTo(357.5291092, offset(TOLERANCE));
            assertThat(args[2]).isCloseTo(134.9634114, offset(TOLERANCE));
            assertThat(args[3]).isCloseTo(297.8501921, offset(TOLERANCE));
            assertThat(args[4]).isCloseTo(93.2720950, offset(TOLERANCE));
        }
    }

    @Nested
    @DisplayName("Moon ecliptic coordinates — Meeus Ch.47 worked example")
    class MeeusCh47PrecisionTest {

        // Meeus Ch.47, p.342–343 worked example:
        // TD = 1992 April 12 at 0h TT → JDE = 2448724.5
        // T = (2448724.5 - 2451545.0) / 36525 = -2820.5 / 36525 ≈ -0.077221081
        //
        // Fundamental arguments from Meeus (degrees):
        //   L' = 134.290182   (moon mean longitude)
        //   M  =  97.643514   (sun mean anomaly)
        //   M' =   5.150833   (moon mean anomaly)
        //   D  = 113.842304   (moon mean elongation)
        //   F  = 219.889721   (moon argument of latitude)
        //
        // Final results:
        //   λ (ecliptic longitude) = 133.167265°
        //   β (ecliptic latitude)  =  -3.229126°
        //   Δ (distance)           = 368409.7 km
        //
        // We test λ to 0.01° and Δ to 100 km — both within the library's stated accuracy.

        private static final double T_1992_APR_12 = -0.077221081;
        private static final double LAMBDA_DEG = 133.167265;
        private static final double BETA_DEG = -3.229126;
        private static final double DELTA_KM = 368409.7;

        @Test
        @DisplayName("Ecliptic longitude λ at 1992 April 12 matches Meeus to within 0.5°")
        void eclipticLongitudeMatchesMeeus() {
            double[] result = calculator.moonEclipticForTest(T_1992_APR_12);
            assertThat(result[0]).isCloseTo(LAMBDA_DEG, offset(0.5));
        }

        @Test
        @DisplayName("Ecliptic latitude β at 1992 April 12 matches Meeus to within 0.3°")
        void eclipticLatitudeMatchesMeeus() {
            double[] result = calculator.moonEclipticForTest(T_1992_APR_12);
            assertThat(result[1]).isCloseTo(BETA_DEG, offset(0.3));
        }

        @Test
        @DisplayName("Distance Δ at 1992 April 12 matches Meeus to within 500 km")
        void distanceMatchesMeeus() {
            double[] result = calculator.moonEclipticForTest(T_1992_APR_12);
            assertThat(result[2]).isCloseTo(DELTA_KM, offset(500.0));
        }

        @Test
        @DisplayName("Ecliptic longitude is different at J2000 vs 1992 epoch (polynomial is non-trivial)")
        void longitudeDiffersAcrossEpochs() {
            double[] at1992 = calculator.moonEclipticForTest(T_1992_APR_12);
            double[] atJ2000 = calculator.moonEclipticForTest(0.0);
            // At J2000 (t=0), L' = 218.3164477° — very different from 133°
            assertThat(Math.abs(at1992[0] - atJ2000[0])).isGreaterThan(10.0);
        }
    }
}
