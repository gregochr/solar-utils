package com.gregochr.solarutils;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Immutable result record holding the moonrise and moonset times for a given date and location.
 *
 * <p>Either or both times may be absent: at high latitudes the Moon can remain above or below
 * the horizon for an entire calendar day.
 *
 * @param moonrise time the Moon rises above the horizon, or empty if it does not rise that day
 * @param moonset  time the Moon sets below the horizon, or empty if it does not set that day
 */
public record MoonriseMoonset(Optional<ZonedDateTime> moonrise, Optional<ZonedDateTime> moonset) { }
