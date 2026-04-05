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
    // Civil twilight (golden hour window)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Civil dawn is before sunrise on a typical winter day")
    void civilDawn_isBeforeSunrise_onWinterDay() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        LocalDateTime dawn = calculator.civilDawn(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime sunrise = calculator.sunrise(DURHAM_LAT, DURHAM_LON, date, UTC);

        assertThat(dawn).isBefore(sunrise);
    }

    @Test
    @DisplayName("Civil dusk is after sunset on a typical winter day")
    void civilDusk_isAfterSunset_onWinterDay() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        LocalDateTime sunset = calculator.sunset(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime dusk = calculator.civilDusk(DURHAM_LAT, DURHAM_LON, date, UTC);

        assertThat(dusk).isAfter(sunset);
    }

    @Test
    @DisplayName("Civil dawn to sunrise window is between 20 and 60 minutes in winter")
    void civilDawn_toSunrise_windowIsBetween20And60Minutes_inWinter() {
        LocalDate date = LocalDate.of(2026, 2, 20);
        LocalDateTime dawn = calculator.civilDawn(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime sunrise = calculator.sunrise(DURHAM_LAT, DURHAM_LON, date, UTC);

        long minutes = java.time.Duration.between(dawn, sunrise).toMinutes();
        assertThat(minutes).isBetween(20L, 60L);
    }

    @Test
    @DisplayName("Civil dusk window is longer in summer than in winter (shallower sun angle)")
    void civilTwilight_window_isLongerInSummerThanWinter() {
        LocalDate summer = LocalDate.of(2026, 6, 21);
        LocalDate winter = LocalDate.of(2026, 12, 21);

        long summerWindow = java.time.Duration.between(
                calculator.sunset(DURHAM_LAT, DURHAM_LON, summer, UTC),
                calculator.civilDusk(DURHAM_LAT, DURHAM_LON, summer, UTC)).toMinutes();

        long winterWindow = java.time.Duration.between(
                calculator.sunset(DURHAM_LAT, DURHAM_LON, winter, UTC),
                calculator.civilDusk(DURHAM_LAT, DURHAM_LON, winter, UTC)).toMinutes();

        assertThat(summerWindow).isGreaterThan(winterWindow);
    }

    // -------------------------------------------------------------------------
    // Solar noon
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Solar noon is between sunrise and sunset")
    void solarNoon_isBetweenSunriseAndSunset() {
        LocalDate date = LocalDate.of(2026, 6, 21);
        LocalDateTime sunrise = calculator.sunrise(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime noon = calculator.solarNoon(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime sunset = calculator.sunset(DURHAM_LAT, DURHAM_LON, date, UTC);

        assertThat(noon).isAfter(sunrise).isBefore(sunset);
    }

    @Test
    @DisplayName("Solar noon at Durham is close to 12:00 UTC (within 30 minutes)")
    void solarNoon_Durham_isCloseToNoonUTC() {
        LocalDate date = LocalDate.of(2026, 6, 21);
        LocalDateTime noon = calculator.solarNoon(DURHAM_LAT, DURHAM_LON, date, UTC);

        LocalDateTime expectedNoon = date.atTime(12, 0);
        assertThat(noon).isCloseTo(expectedNoon, within(30, MINUTES));
    }

    @Test
    @DisplayName("Solar noon is approximately equidistant between sunrise and sunset")
    void solarNoon_isApproximatelyMidpoint_betweenSunriseAndSunset() {
        LocalDate date = LocalDate.of(2026, 3, 20);
        LocalDateTime sunrise = calculator.sunrise(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime noon = calculator.solarNoon(DURHAM_LAT, DURHAM_LON, date, UTC);
        LocalDateTime sunset = calculator.sunset(DURHAM_LAT, DURHAM_LON, date, UTC);

        long minutesToNoon = MINUTES.between(sunrise, noon);
        long minutesFromNoon = MINUTES.between(noon, sunset);

        assertThat(minutesToNoon).isCloseTo(minutesFromNoon, org.assertj.core.data.Offset.offset(5L));
    }

    // -------------------------------------------------------------------------
    // Day length
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dayLengthMinutes() returns longer day in summer than winter at Durham")
    void dayLengthMinutes_isLongerInSummerThanWinter() {
        long summer = calculator.dayLengthMinutes(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 6, 21));
        long winter = calculator.dayLengthMinutes(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 12, 21));

        assertThat(summer).isGreaterThan(winter);
    }

    @Test
    @DisplayName("Day length on the equinox is approximately 12 hours")
    void dayLength_onEquinox_isApproximately12Hours() {
        long minutes = calculator.dayLengthMinutes(DURHAM_LAT, DURHAM_LON, LocalDate.of(2026, 3, 20));

        assertThat(minutes).isCloseTo(720L, org.assertj.core.data.Offset.offset(30L));
    }

    @Test
    @DisplayName("Day length is positive for all seasons")
    void dayLength_isPositive_forAllSeasons() {
        LocalDate[] dates = {
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 20),
            LocalDate.of(2026, 6, 21),
            LocalDate.of(2026, 9, 22),
            LocalDate.of(2026, 12, 21)
        };

        for (LocalDate date : dates) {
            assertThat(calculator.dayLengthMinutes(DURHAM_LAT, DURHAM_LON, date)).isPositive();
        }
    }

    // -------------------------------------------------------------------------
    // Golden hour
    // -------------------------------------------------------------------------

    // Angel of the North: 54.9144°N, 1.5803°W
    private static final double ANGEL_LAT = 54.9144;
    private static final double ANGEL_LON = -1.5803;

    @Test
    @DisplayName("Golden hour start is before sunset on 5 April 2026")
    void goldenHourStart_isBeforeSunset() {
        var date = LocalDate.of(2026, 4, 5);
        LocalDateTime start = calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime sunset = calculator.sunset(ANGEL_LAT, ANGEL_LON, date, UTC);

        assertThat(start).isBefore(sunset);
    }

    @Test
    @DisplayName("Golden hour end is after sunrise on 5 April 2026")
    void goldenHourEnd_isAfterSunrise() {
        var date = LocalDate.of(2026, 4, 5);
        LocalDateTime end = calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime sunrise = calculator.sunrise(ANGEL_LAT, ANGEL_LON, date, UTC);

        assertThat(end).isAfter(sunrise);
    }

    @Test
    @DisplayName("Evening golden hour at 55°N in April is 25–55 minutes")
    void goldenHourWindow_eveningApril_plausibleDuration() {
        var date = LocalDate.of(2026, 4, 5);
        LocalDateTime start = calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime sunset = calculator.sunset(ANGEL_LAT, ANGEL_LON, date, UTC);

        long minutes = MINUTES.between(start, sunset);
        assertThat(minutes).isBetween(25L, 55L);
    }

    @Test
    @DisplayName("Morning golden hour at 55°N on summer solstice exceeds 60 minutes (shallow sun angle)")
    void goldenHourWindow_morningMidsummer_exceedsSixtyMinutes() {
        var date = LocalDate.of(2026, 6, 21);
        LocalDateTime sunrise = calculator.sunrise(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime end = calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, date, UTC);

        long minutes = MINUTES.between(sunrise, end);
        assertThat(minutes).isGreaterThan(60);
    }

    @Test
    @DisplayName("Morning and evening golden hours are roughly equal on the same day")
    void goldenHour_morningAndEvening_roughlySymmetric() {
        var date = LocalDate.of(2026, 4, 5);
        long morningMinutes = MINUTES.between(
                calculator.sunrise(ANGEL_LAT, ANGEL_LON, date, UTC),
                calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, date, UTC));
        long eveningMinutes = MINUTES.between(
                calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, UTC),
                calculator.sunset(ANGEL_LAT, ANGEL_LON, date, UTC));

        assertThat(morningMinutes).isCloseTo(eveningMinutes, org.assertj.core.data.Offset.offset(10L));
    }

    @Test
    @DisplayName("Golden hour end is before solar noon; golden hour start is after solar noon")
    void goldenHour_bracketsSolarNoon() {
        var date = LocalDate.of(2026, 4, 5);
        LocalDateTime end = calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime noon = calculator.solarNoon(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime start = calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, UTC);

        assertThat(end).isBefore(noon);
        assertThat(start).isAfter(noon);
    }

    @Test
    @DisplayName("Winter golden hour is longer than spring golden hour at 55°N (shallower ascent)")
    void goldenHour_longerInWinterThanSpring() {
        long winterMinutes = MINUTES.between(
                calculator.sunrise(ANGEL_LAT, ANGEL_LON, LocalDate.of(2026, 12, 21), UTC),
                calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, LocalDate.of(2026, 12, 21), UTC));
        long springMinutes = MINUTES.between(
                calculator.sunrise(ANGEL_LAT, ANGEL_LON, LocalDate.of(2026, 3, 20), UTC),
                calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, LocalDate.of(2026, 3, 20), UTC));

        assertThat(winterMinutes).isGreaterThan(springMinutes);
    }

    @Test
    @DisplayName("Equatorial golden hour is shorter than 55°N golden hour (steeper sun path)")
    void goldenHour_shorterAtEquatorThanHighLatitude() {
        var date = LocalDate.of(2026, 4, 5);
        long equatorMinutes = MINUTES.between(
                calculator.sunrise(0.0, 0.0, date, UTC),
                calculator.goldenHourEnd(0.0, 0.0, date, UTC));
        long highLatMinutes = MINUTES.between(
                calculator.sunrise(ANGEL_LAT, ANGEL_LON, date, UTC),
                calculator.goldenHourEnd(ANGEL_LAT, ANGEL_LON, date, UTC));

        assertThat(equatorMinutes).isLessThan(highLatMinutes);
    }

    @Test
    @DisplayName("Golden hour start respects BST — 1 hour ahead of UTC in summer")
    void goldenHourStart_respectsTimeZone() {
        var date = LocalDate.of(2026, 6, 21);
        LocalDateTime utcStart = calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, UTC);
        LocalDateTime londonStart = calculator.goldenHourStart(ANGEL_LAT, ANGEL_LON, date, LONDON);

        assertThat(londonStart).isEqualTo(utcStart.plusHours(1));
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
