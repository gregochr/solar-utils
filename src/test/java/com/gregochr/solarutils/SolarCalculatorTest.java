package com.gregochr.solarutils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Verifies solar calculations against known values for Durham, UK (54.7753°N, 1.5849°W).
 * All expected times are UTC. Tolerance is ±3 minutes to account for atmospheric variation.
 */
class SolarCalculatorTest {

    // Durham Cricket Club, Brandon, County Durham
    private static final double DURHAM_LAT = 54.7753;
    private static final double DURHAM_LON = -1.5849;
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final ZoneId LONDON = ZoneId.of("Europe/London");
    private static final int TOLERANCE_MINUTES = 3;

    private SolarCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new SolarCalculator();
    }

    // -------------------------------------------------------------------------
    // Sunrise — UTC
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunrise at Durham on a typical winter day (20 Feb 2026) is around 07:19 UTC")
    void sunrise_Durham_winterDay() {
        LocalDateTime result = calculator.sunrise(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 2, 20), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 2, 20, 7, 19);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunrise at Durham on summer solstice (21 Jun 2026) is around 03:26 UTC")
    void sunrise_Durham_summerSolstice() {
        LocalDateTime result = calculator.sunrise(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 6, 21), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 6, 21, 3, 26);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunrise at Durham on winter solstice (21 Dec 2026) is around 08:28 UTC")
    void sunrise_Durham_winterSolstice() {
        LocalDateTime result = calculator.sunrise(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 12, 21), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 12, 21, 8, 28);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunrise at Durham on spring equinox (20 Mar 2026) is around 06:09 UTC")
    void sunrise_Durham_springEquinox() {
        LocalDateTime result = calculator.sunrise(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 3, 20), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 3, 20, 6, 9);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    // -------------------------------------------------------------------------
    // Sunset — UTC
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunset at Durham on a typical winter day (20 Feb 2026) is around 17:22 UTC")
    void sunset_Durham_winterDay() {
        LocalDateTime result = calculator.sunset(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 2, 20), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 2, 20, 17, 22);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunset at Durham on summer solstice (21 Jun 2026) is around 20:47 UTC (21:47 BST)")
    void sunset_Durham_summerSolstice() {
        LocalDateTime result = calculator.sunset(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 6, 21), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 6, 21, 20, 47);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunset at Durham on winter solstice (21 Dec 2026) is around 15:40 UTC")
    void sunset_Durham_winterSolstice() {
        LocalDateTime result = calculator.sunset(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 12, 21), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 12, 21, 15, 40);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    @Test
    @DisplayName("Sunset at Durham on autumn equinox (22 Sep 2026) is around 18:07 UTC")
    void sunset_Durham_autumnEquinox() {
        LocalDateTime result = calculator.sunset(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 9, 22), UTC);

        LocalDateTime expected = LocalDateTime.of(2026, 9, 22, 18, 7);
        assertThat(result).isCloseTo(expected, within(TOLERANCE_MINUTES, MINUTES));
    }

    // -------------------------------------------------------------------------
    // Time zone conversion
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunrise returned in Europe/London BST is 1 hour ahead of UTC in summer")
    void sunrise_returnedInLondonBST_isOneHourAheadOfUTC() {
        LocalDate summerDate = LocalDate.of(2026, 6, 21);

        LocalDateTime utcResult = calculator.sunrise(DURHAM_LAT, DURHAM_LON, summerDate, UTC);
        LocalDateTime londonResult = calculator.sunrise(DURHAM_LAT, DURHAM_LON, summerDate, LONDON);

        assertThat(londonResult).isEqualTo(utcResult.plusHours(1));
    }

    @Test
    @DisplayName("Sunset returned in Europe/London GMT equals UTC in winter")
    void sunset_returnedInLondonGMT_equalsUTC_inWinter() {
        LocalDate winterDate = LocalDate.of(2026, 12, 21);

        LocalDateTime utcResult = calculator.sunset(DURHAM_LAT, DURHAM_LON, winterDate, UTC);
        LocalDateTime londonResult = calculator.sunset(DURHAM_LAT, DURHAM_LON, winterDate, LONDON);

        assertThat(londonResult).isEqualTo(utcResult); // GMT == UTC in winter
    }

    // -------------------------------------------------------------------------
    // Sunrise azimuth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunrise azimuth at Durham on spring equinox is approximately due East (90°)")
    void sunriseAzimuth_Durham_springEquinox_isDueEast() {
        int azimuth = calculator.sunriseAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 3, 20));

        assertThat(azimuth).isCloseTo(90, org.assertj.core.data.Offset.offset(5));
    }

    @Test
    @DisplayName("Sunrise azimuth at Durham on summer solstice is north of East (< 90°)")
    void sunriseAzimuth_Durham_summerSolstice_isNorthOfEast() {
        int azimuth = calculator.sunriseAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 6, 21));

        assertThat(azimuth).isLessThan(90);
    }

    @Test
    @DisplayName("Sunrise azimuth at Durham on winter solstice is south of East (> 90°)")
    void sunriseAzimuth_Durham_winterSolstice_isSouthOfEast() {
        int azimuth = calculator.sunriseAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 12, 21));

        assertThat(azimuth).isGreaterThan(90);
    }

    // -------------------------------------------------------------------------
    // Sunset azimuth
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunset azimuth at Durham on autumn equinox is approximately due West (270°)")
    void sunsetAzimuth_Durham_autumnEquinox_isDueWest() {
        int azimuth = calculator.sunsetAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 9, 22));

        assertThat(azimuth).isCloseTo(270, org.assertj.core.data.Offset.offset(5));
    }

    @Test
    @DisplayName("Sunset azimuth at Durham on summer solstice is north of West (> 270°)")
    void sunsetAzimuth_Durham_summerSolstice_isNorthOfWest() {
        int azimuth = calculator.sunsetAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 6, 21));

        assertThat(azimuth).isGreaterThan(270);
    }

    @Test
    @DisplayName("Sunset azimuth at Durham on winter solstice is south of West (< 270°)")
    void sunsetAzimuth_Durham_winterSolstice_isSouthOfWest() {
        int azimuth = calculator.sunsetAzimuth(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 12, 21));

        assertThat(azimuth).isLessThan(270);
    }

    @Test
    @DisplayName("Sunrise and sunset azimuths are symmetric about 180° on the equinox")
    void azimuths_areSymmetricAbout180_onEquinox() {
        LocalDate equinox = LocalDate.of(2026, 3, 20);
        int rise = calculator.sunriseAzimuth(DURHAM_LAT, DURHAM_LON, equinox);
        int set = calculator.sunsetAzimuth(DURHAM_LAT, DURHAM_LON, equinox);

        assertThat(rise + set).isCloseTo(360, org.assertj.core.data.Offset.offset(2));
    }

    // -------------------------------------------------------------------------
    // Sanity checks
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Sunrise is always before sunset on any given day")
    void sunrise_isAlwaysBeforeSunset() {
        LocalDate[] dates = {
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 20),
            LocalDate.of(2026, 6, 21),
            LocalDate.of(2026, 9, 22),
            LocalDate.of(2026, 12, 21)
        };

        for (LocalDate date : dates) {
            LocalDateTime sunrise = calculator.sunrise(DURHAM_LAT, DURHAM_LON, date, UTC);
            LocalDateTime sunset = calculator.sunset(DURHAM_LAT, DURHAM_LON, date, UTC);
            assertThat(sunrise).isBefore(sunset);
        }
    }

    @Test
    @DisplayName("Day length in Durham is longer in June than December")
    void dayLength_isLongerInSummerThanWinter() {
        LocalDate summer = LocalDate.of(2026, 6, 21);
        LocalDate winter = LocalDate.of(2026, 12, 21);

        long summerDayMinutes = MINUTES.between(
                calculator.sunrise(DURHAM_LAT, DURHAM_LON, summer, UTC),
                calculator.sunset(DURHAM_LAT, DURHAM_LON, summer, UTC)
        );
        long winterDayMinutes = MINUTES.between(
                calculator.sunrise(DURHAM_LAT, DURHAM_LON, winter, UTC),
                calculator.sunset(DURHAM_LAT, DURHAM_LON, winter, UTC)
        );

        assertThat(summerDayMinutes).isGreaterThan(winterDayMinutes);
    }
}
