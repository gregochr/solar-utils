package com.gregochr.solarutils;

/**
 * The eight principal phases of the Moon, derived from the elongation angle
 * between the Moon and the Sun.
 *
 * <p>Phases are determined by the Moon's ecliptic elongation from the Sun in 45° sectors:
 * <ul>
 *   <li>New Moon: 0° ± 22.5°</li>
 *   <li>Waxing Crescent: 22.5° – 67.5°</li>
 *   <li>First Quarter: 67.5° – 112.5°</li>
 *   <li>Waxing Gibbous: 112.5° – 157.5°</li>
 *   <li>Full Moon: 157.5° – 202.5°</li>
 *   <li>Waning Gibbous: 202.5° – 247.5°</li>
 *   <li>Last Quarter: 247.5° – 292.5°</li>
 *   <li>Waning Crescent: 292.5° – 337.5°</li>
 * </ul>
 */
public enum LunarPhase {

    /** Moon is between Earth and Sun — illumination near 0%. */
    NEW_MOON("New Moon"),

    /** Moon moving away from Sun, less than half illuminated. */
    WAXING_CRESCENT("Waxing Crescent"),

    /** Moon at 90° elongation, right half illuminated. */
    FIRST_QUARTER("First Quarter"),

    /** Moon more than half illuminated and growing. */
    WAXING_GIBBOUS("Waxing Gibbous"),

    /** Earth is between Moon and Sun — illumination near 100%. */
    FULL_MOON("Full Moon"),

    /** Moon more than half illuminated and shrinking. */
    WANING_GIBBOUS("Waning Gibbous"),

    /** Moon at 270° elongation, left half illuminated. */
    LAST_QUARTER("Last Quarter"),

    /** Moon approaching the Sun again, less than half illuminated. */
    WANING_CRESCENT("Waning Crescent");

    private final String displayName;

    LunarPhase(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable display name for this lunar phase.
     *
     * @return display name, e.g. {@code "Waxing Crescent"}
     */
    public String displayName() {
        return displayName;
    }
}
