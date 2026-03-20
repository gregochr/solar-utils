package com.gregochr.solarutils;

/**
 * Immutable result record representing the Moon's position and state as observed from
 * a specific location at a specific instant.
 *
 * <p>Produced by {@link LunarCalculator#calculate(java.time.ZonedDateTime, double, double)}.
 *
 * @param altitude        altitude above the horizon in degrees; negative = below horizon
 * @param azimuth         azimuth in degrees [0, 360), measured clockwise from North
 * @param illumination    fraction of the Moon's disc that is illuminated [0.0, 1.0]
 * @param phase           the current lunar phase
 * @param distanceKm      distance from the centre of Earth to the Moon in kilometres
 */
public record LunarPosition(
        double altitude,
        double azimuth,
        double illumination,
        LunarPhase phase,
        double distanceKm) {

    /**
     * Returns the illuminated fraction of the Moon's disc as a percentage.
     *
     * @return illumination percentage [0.0, 100.0]
     */
    public double illuminationPercent() {
        return illumination * 100.0;
    }

    /**
     * Returns {@code true} if the Moon is currently above the observer's horizon.
     *
     * @return {@code true} when altitude &gt; 0°
     */
    public boolean isAboveHorizon() {
        return altitude > 0.0;
    }

    /**
     * Returns {@code true} if the Moon is in the northern half of the sky.
     *
     * <p>The northern sky is defined as azimuths in [270°, 360°) or [0°, 90°] — i.e.,
     * the Moon is generally toward the North rather than the South.
     * This is relevant for aurora photography, where the Moon in the northern sky
     * competes directly with the aurora display.
     *
     * @return {@code true} when azimuth ≤ 90° or azimuth ≥ 270°
     */
    public boolean isInNorthernSky() {
        return azimuth <= 90.0 || azimuth >= 270.0;
    }

    /**
     * Computes a penalty score [0.0, 1.0] representing how much the Moon degrades
     * aurora photography conditions.
     *
     * <p>The score factors in:
     * <ul>
     *   <li>Whether the Moon is above the horizon (0.0 if below)</li>
     *   <li>Moon illumination — a full moon causes far more light pollution than a crescent</li>
     *   <li>Moon altitude — a Moon near the horizon has less impact than one high overhead</li>
     *   <li>Moon direction — a Moon in the southern sky (away from the aurora) reduces
     *       impact by 60% compared to one directly in the northern viewing direction</li>
     * </ul>
     *
     * <p>Score interpretation:
     * <ul>
     *   <li>0.0 — no penalty (Moon below horizon)</li>
     *   <li>0.0–0.1 — negligible impact</li>
     *   <li>0.1–0.4 — moderate impact, aurora still likely visible</li>
     *   <li>0.4–0.7 — significant impact, only bright auroras visible</li>
     *   <li>0.7–1.0 — severe impact, aurora photography very difficult</li>
     * </ul>
     *
     * @return aurora penalty score in [0.0, 1.0]
     */
    public double auroraPenalty() {
        if (!isAboveHorizon()) {
            return 0.0;
        }
        double altitudeFactor = Math.sin(Math.toRadians(Math.max(0.0, altitude)));
        double directionFactor = isInNorthernSky() ? 1.0 : 0.4;
        return illumination * altitudeFactor * directionFactor;
    }
}
