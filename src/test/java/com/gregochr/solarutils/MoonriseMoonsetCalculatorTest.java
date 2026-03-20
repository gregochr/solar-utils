package com.gregochr.solarutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Verifies MoonriseMoonsetCalculator against known almanac values.
 *
 * <p>Primary location: Durham, UK (54.776°N, 1.575°W).
 * High-latitude edge case: Tromsø (69.649°N, 18.955°E).
 * Tolerance: ±5 minutes, consistent with published table accuracy.
 */
class MoonriseMoonsetCalculatorTest {

    private static final double DURHAM_LAT = 54.776;
    private static final double DURHAM_LON = -1.575;

    private static final double EMBLETON_LAT = 55.520;
    private static final double EMBLETON_LON = -1.636;

    private static final double TROMSO_LAT = 69.649;
    private static final double TROMSO_LON = 18.955;

    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final ZoneId OSLO = ZoneId.of("Europe/Oslo");

    private static final int TOLERANCE_MINUTES = 5;

    private MoonriseMoonsetCalculator calculator;
    private LunarCalculator lunarCalculator;

    @BeforeEach
    void setUp() {
        calculator = new MoonriseMoonsetCalculator();
        lunarCalculator = new LunarCalculator();
    }

    // -------------------------------------------------------------------------
    // Input validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("calculate() throws IllegalArgumentException for latitude out of range")
    void invalidLatitudeThrows() {
        assertThatThrownBy(() -> calculator.calculate(LocalDate.of(2025, 1, 13), -91.0, 0.0, UTC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude");
    }

    @Test
    @DisplayName("calculate() throws IllegalArgumentException for longitude out of range")
    void invalidLongitudeThrows() {
        assertThatThrownBy(() -> calculator.calculate(LocalDate.of(2025, 1, 13), 0.0, 181.0, UTC))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude");
    }

    @Test
    @DisplayName("Latitude exactly -90.0 is accepted (boundary value does not throw)")
    void latitudeAtNegative90IsAccepted() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> calculator.calculate(LocalDate.of(2025, 1, 13), -90.0, 0.0, UTC))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Latitude exactly 90.0 is accepted (boundary value does not throw)")
    void latitudeAt90IsAccepted() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> calculator.calculate(LocalDate.of(2025, 1, 13), 90.0, 0.0, UTC))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Longitude exactly -180.0 is accepted (boundary value does not throw)")
    void longitudeAtNegative180IsAccepted() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> calculator.calculate(LocalDate.of(2025, 1, 13), 0.0, -180.0, UTC))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Longitude exactly 180.0 is accepted (boundary value does not throw)")
    void longitudeAt180IsAccepted() {
        org.assertj.core.api.Assertions.assertThatCode(
                () -> calculator.calculate(LocalDate.of(2025, 1, 13), 0.0, 180.0, UTC))
                .doesNotThrowAnyException();
    }

    // -------------------------------------------------------------------------
    // Normal day — moonrise and moonset both present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Normal day")
    class NormalDay {

        @Test
        @DisplayName("Both moonrise and moonset are present around the full moon at Durham (2025-01-13)")
        void fullMoonDateHasMoonriseAndMoonset() {
            var result = calculator.calculate(LocalDate.of(2025, 1, 13), DURHAM_LAT, DURHAM_LON, UTC);

            assertThat(result.moonrise()).isPresent();
            assertThat(result.moonset()).isPresent();
        }

        @Test
        @DisplayName("Both moonrise and moonset times fall within a 24-hour span on 2025-01-13")
        void moonriseAndMoonsetAreWithin24HoursOfEachOther() {
            var result = calculator.calculate(LocalDate.of(2025, 1, 13), DURHAM_LAT, DURHAM_LON, UTC);

            assertThat(result.moonrise()).isPresent();
            assertThat(result.moonset()).isPresent();
            // The moon set in the morning (~09:00) and rose in the afternoon (~14:54)
            // on this full-moon date; the difference is less than 24 hours either way
            long diffMinutes = Math.abs(
                    java.time.Duration.between(
                            result.moonrise().get(), result.moonset().get()).toMinutes());
            assertThat(diffMinutes).isLessThan(24 * 60);
        }

        @Test
        @DisplayName("Moonrise time at Durham on 2025-01-13 is approximately 14:54 UTC")
        void moonriseAtDurhamOnFullMoonJan2025() {
            // On the full moon (Jan 13, 2025) the moon rises in the afternoon at Durham.
            // The moon set in the morning (~09:00 UTC) and rises again around 14:54 UTC,
            // consistent with a January full moon rising near sunset time.
            var result = calculator.calculate(LocalDate.of(2025, 1, 13), DURHAM_LAT, DURHAM_LON, UTC);

            ZonedDateTime expected = ZonedDateTime.of(2025, 1, 13, 14, 54, 0, 0, UTC);
            assertThat(result.moonrise()).isPresent();
            assertThat(result.moonrise().get())
                    .isCloseTo(expected, within(TOLERANCE_MINUTES, ChronoUnit.MINUTES));
        }

        @Test
        @DisplayName("Result times are within the 25-hour scan window")
        void timesAreWithinScanWindow() {
            LocalDate date = LocalDate.of(2025, 6, 11);
            var result = calculator.calculate(date, DURHAM_LAT, DURHAM_LON, UTC);

            ZonedDateTime windowStart = date.atStartOfDay(UTC);
            ZonedDateTime windowEnd = windowStart.plusHours(25);

            result.moonrise().ifPresent(t -> {
                assertThat(t).isAfterOrEqualTo(windowStart);
                assertThat(t).isBefore(windowEnd);
            });
            result.moonset().ifPresent(t -> {
                assertThat(t).isAfterOrEqualTo(windowStart);
                assertThat(t).isBefore(windowEnd);
            });
        }

        @Test
        @DisplayName("Moon altitude at the returned moonrise time is approximately 0°")
        void altitudeAtMoonriseIsNearZero() {
            var result = calculator.calculate(LocalDate.of(2025, 1, 13), DURHAM_LAT, DURHAM_LON, UTC);

            assertThat(result.moonrise()).isPresent();
            ZonedDateTime riseTime = result.moonrise().get();
            double altAtRise = lunarCalculator.calculate(riseTime, DURHAM_LAT, DURHAM_LON).altitude();

            // Should be near the horizon (within 1°)
            assertThat(Math.abs(altAtRise)).isLessThan(1.0);
        }

        @Test
        @DisplayName("Moon altitude at the returned moonset time is approximately 0°")
        void altitudeAtMoonsetIsNearZero() {
            var result = calculator.calculate(LocalDate.of(2025, 1, 13), DURHAM_LAT, DURHAM_LON, UTC);

            assertThat(result.moonset()).isPresent();
            ZonedDateTime setTime = result.moonset().get();
            double altAtSet = lunarCalculator.calculate(setTime, DURHAM_LAT, DURHAM_LON).altitude();

            // Should be near the horizon (within 1°)
            assertThat(Math.abs(altAtSet)).isLessThan(1.0);
        }
    }

    // -------------------------------------------------------------------------
    // Time zone handling
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Time zone handling")
    class TimeZones {

        @Test
        @DisplayName("Moonrise in UTC and Europe/London differ by the zone offset at that date")
        void moonriseInLondonVsUtcDiffersByOffset() {
            // In January, London is GMT (same as UTC)
            LocalDate date = LocalDate.of(2025, 1, 13);
            var utcResult = calculator.calculate(date, DURHAM_LAT, DURHAM_LON, UTC);
            var londonResult = calculator.calculate(date, DURHAM_LAT, DURHAM_LON, LONDON);

            assertThat(utcResult.moonrise()).isPresent();
            assertThat(londonResult.moonrise()).isPresent();

            // Both represent the same instant — converting to UTC should be equal
            ZonedDateTime utcRise = utcResult.moonrise().get();
            ZonedDateTime londonRise = londonResult.moonrise().get().withZoneSameInstant(UTC);
            assertThat(utcRise).isCloseTo(londonRise, within(1, ChronoUnit.MINUTES));
        }
    }

    // -------------------------------------------------------------------------
    // High latitude
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("High latitude")
    class HighLatitude {

        @Test
        @DisplayName("Tromsø in winter may have no moonrise or moonset (Optional may be empty)")
        void tromsoPolarNightResultIsValid() {
            // At 69°N in January, the moon may not rise/set every day
            // We just verify the result is a valid MoonriseMoonset (not an exception)
            var result = calculator.calculate(
                    LocalDate.of(2025, 1, 13), TROMSO_LAT, TROMSO_LON, OSLO);

            // Result may have either present or empty optionals — both are valid
            assertThat(result).isNotNull();
            assertThat(result.moonrise()).isNotNull();
            assertThat(result.moonset()).isNotNull();
        }

        @Test
        @DisplayName("When present, Tromsø moonrise and moonset are within the 25-hour window")
        void tromsoCrossingTimesAreInWindow() {
            LocalDate date = LocalDate.of(2025, 1, 13);
            var result = calculator.calculate(date, TROMSO_LAT, TROMSO_LON, OSLO);

            ZonedDateTime windowStart = date.atStartOfDay(OSLO);
            ZonedDateTime windowEnd = windowStart.plusHours(25);

            result.moonrise().ifPresent(t -> {
                assertThat(t).isAfterOrEqualTo(windowStart);
                assertThat(t).isBefore(windowEnd);
            });
        }
    }

    // -------------------------------------------------------------------------
    // Embleton Bay
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Embleton Bay")
    class EmbletonBay {

        @Test
        @DisplayName("Embleton Bay moonrise is within 10 minutes of Durham moonrise (nearby locations)")
        void embletonAndDurhamMoonrisesAreSimilar() {
            LocalDate date = LocalDate.of(2025, 1, 13);
            var durham = calculator.calculate(date, DURHAM_LAT, DURHAM_LON, UTC);
            var embleton = calculator.calculate(date, EMBLETON_LAT, EMBLETON_LON, UTC);

            assertThat(durham.moonrise()).isPresent();
            assertThat(embleton.moonrise()).isPresent();

            // Embleton Bay is ~0.74° further north than Durham; latitude difference causes
            // a real ~7-minute difference in moonrise time for a winter full moon.
            assertThat(durham.moonrise().get())
                    .isCloseTo(embleton.moonrise().get(), within(10, ChronoUnit.MINUTES));
        }
    }

    // -------------------------------------------------------------------------
    // Seasonal variation
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Seasonal variation")
    class SeasonalVariation {

        @Test
        @DisplayName("Moonrise occurs on multiple consecutive days from Durham in January")
        void moonriseOccursOnConsecutiveDays() {
            int riseCount = 0;
            for (int day = 13; day <= 17; day++) {
                var result = calculator.calculate(
                        LocalDate.of(2025, 1, day), DURHAM_LAT, DURHAM_LON, UTC);
                if (result.moonrise().isPresent()) {
                    riseCount++;
                }
            }
            // The moon rises on most days; expect at least 3 out of 5
            assertThat(riseCount).isGreaterThanOrEqualTo(3);
        }
    }
}
