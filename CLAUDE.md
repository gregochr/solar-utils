# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`solar-utils` is a zero-dependency Java library published to JitPack that provides astronomical calculations for the **Nephel platform**. It is consumed by **PhotoCast** (landscape photography forecasting) and potentially other Nephel apps. The library must remain lightweight with no runtime dependencies beyond the JDK.

- **Package:** `com.gregochr.solarutils`
- **Publishing:** JitPack (triggered by GitHub release tags)

## Commands

```bash
./mvnw clean verify           # Build, test, and run all checks (coverage, Checkstyle, SpotBugs)
./mvnw test                   # Run all tests
./mvnw checkstyle:check       # Run Checkstyle
./mvnw spotbugs:check         # Run SpotBugs
./mvnw org.pitest:pitest-maven:mutationCoverage  # Run PIT mutation testing
./mvnw --batch-mode deploy    # Publish to JitPack (CI only)
```

To run a single test class or method:
```bash
./mvnw test -Dtest=SolarCalculatorTest
./mvnw test -Dtest=SolarCalculatorTest#testSunriseWinterSolstice
```

## Architecture

### Module Structure

```
com.gregochr.solarutils
├── SolarCalculator.java           # sunrise/sunset, civil twilight, solar noon, azimuth
├── CoordinateTransformer.java     # package-private shared coordinate maths (ecliptic→equatorial→horizontal, GMST, Julian Day, obliquity)
├── LunarCalculator.java           # moon position, illumination, phase (Meeus Ch.47/48)
├── LunarPosition.java             # result record — altitude, azimuth, illumination, phase, distance, auroraPenalty()
├── LunarPhase.java                # enum — 8 lunar phases
└── MoonriseMoonsetCalculator.java # moonrise/moonset via iterative horizon search + bisection
```

### Design Principles

- **Zero runtime dependencies.** No Spring, no logging frameworks, no external libs. Test dependencies (JUnit, AssertJ, PIT) are fine.
- **Immutable results.** All return types are Java records. No setters, no mutation.
- **Degree-based public API.** All angles in the public API are in degrees. Radian conversion is internal only.
- **Meeus algorithms.** Based on Jean Meeus, *Astronomical Algorithms* (2nd ed.). Simplified series (principal terms) — accuracy within ~0.5° for position and ~1% for illumination is sufficient.
- **Observer-centric.** All calculations take `(ZonedDateTime, latitude, longitude)` and return results from the observer's perspective.
- **Shared coordinate code lives in `CoordinateTransformer`.** Both `SolarCalculator` and `LunarCalculator` delegate to it. Do not duplicate trigonometric helpers.

### SolarCalculator Internals

All time-based solar calculations route through the private `calculateWithZenith()` method with the appropriate zenith angle:
- `ZENITH = 90.833°` — standard sunrise/sunset (atmospheric refraction + solar disc radius)
- `CIVIL_TWILIGHT_ZENITH = 96.0°` — sun 6° below horizon (civil dawn/dusk)

`calculateAzimuth()` handles both sunrise and sunset azimuths via a direction flag. Azimuth methods return `int` (0–359°) and omit `ZoneId`.

## Coding Standards

- **Google Java Style**, modified: 4-space indentation, 120-character line limit. Config: `config/checkstyle/checkstyle.xml`
- No wildcard imports, no unused imports
- Use `var` for local variables where the type is obvious from the RHS
- Prefer `Math.toRadians()` / `Math.toDegrees()` over manual `* Math.PI / 180.0` in new code (refactor existing uses opportunistically)
- Use Java 21 features (records, sealed interfaces, pattern matching) where appropriate

### Javadoc

All public classes and methods require Javadoc with `@param`, `@return`, and `@throws`. Reference Meeus chapter numbers where applicable (e.g., `Based on Meeus Ch.47`) and document accuracy expectations (e.g., `Accuracy: ~0.5° for position`).

### Input Validation and Error Handling

- Validate: latitude ∈ [-90, 90], longitude ∈ [-180, 180] — throw `IllegalArgumentException` with a descriptive message
- No checked exceptions in the public API
- No nulls in the public API — use `Optional` where absence is meaningful (e.g., moonrise may not occur at high latitudes)

## Testing

**Framework:** JUnit 5 + AssertJ. Use `@Nested` classes to group related tests and `@DisplayName` for descriptive names:

```java
@Test
@DisplayName("Full moon at midnight in winter should be high in the south from Durham")
void fullMoonMidnightWinter() { ... }
```

Use `isCloseTo(expected, within(tolerance))` for floating point and time assertions. No mocking — this is a pure calculation library.

### Reference Locations

- **Durham, UK — 54.776°N, -1.575°W** — primary test location (Chris's observing site)
- **Embleton Bay — 55.520°N, -1.636°W** — secondary, for coastal aurora scenarios

Solar time assertions use ±3 minutes tolerance.

### Test Strategy

1. **Known astronomical events.** Validate against published almanac data (full moons, new moons, equinoxes). Use 2024–2026 dates.
2. **Boundary conditions.** High latitudes (moon doesn't set), equator, date boundaries, midnight UTC.
3. **Physical plausibility.** Illumination ∈ [0,1], altitude ∈ [-90,90], azimuth ∈ [0,360).
4. **Cross-validation.** Compare `LunarCalculator` output against USNO or timeanddate.com for specific dates.
5. **Regression tests.** Any bug fix must include a test that reproduces the bug first.

### Coverage Requirements

- **JaCoCo:** ≥ 80% line AND branch coverage — enforced, build fails if unmet
- **PIT mutation testing:** ≥ 70% mutation kill score — exclude trivial methods (enum `values()`, record accessors)

## CI Pipeline

Workflow: `.github/workflows/ci.yml`. Triggers: push to `main`, PRs targeting `main`, weekly scheduled run.

| Stage | Command |
|---|---|
| Build & Test | `mvn clean verify` (includes JaCoCo check) |
| Mutation Testing | `mvn org.pitest:pitest-maven:mutationCoverage` |
| Checkstyle | `mvn checkstyle:check` |
| SpotBugs | `mvn spotbugs:check` |
| OWASP Dependency-Check | `mvn org.owasp:dependency-check-maven:check` (fails on CVSS ≥ 7.0; uses `NVD_API_KEY` secret) |

JaCoCo HTML, PIT HTML, and Dependency-Check HTML reports are uploaded as workflow artifacts.

## Release Process

1. Update version in `pom.xml`
2. Merge PR to `main`
3. Create a GitHub release with a matching tag (e.g., `v1.3.0`)
4. JitPack picks up the tag and builds automatically
5. Consumers (PhotoCast) update their JitPack dependency version

## Current Task: Add Lunar Calculations

Integrate moon position, phase, illumination, and moonrise/moonset into the library.

**Integration steps:**
1. Extract shared coordinate transformations into `CoordinateTransformer` (package-private)
2. Refactor `SolarCalculator` to delegate to `CoordinateTransformer`
3. Add `LunarCalculator`, `LunarPosition`, `LunarPhase`, `MoonriseMoonsetCalculator`
4. Wire `LunarCalculator` to use `CoordinateTransformer`
5. Write comprehensive tests (see Testing section)
6. Ensure JaCoCo ≥ 80% and PIT ≥ 70% thresholds are met
7. Update `README.md` with lunar API examples
8. Bump version to next minor

**Acceptance criteria:**
- [ ] All existing solar tests still pass
- [ ] Lunar illumination within 5% of almanac values for test dates
- [ ] Lunar altitude/azimuth within 1° of USNO reference values
- [ ] Moonrise/moonset within 5 minutes of published times
- [ ] `auroraPenalty()` returns `0.0` when moon is below horizon
- [ ] `auroraPenalty()` returns `> 0.5` for full moon high in northern sky
- [ ] All eight lunar phases reachable across a 30-day lunation
- [ ] Checkstyle and SpotBugs: zero violations
- [ ] OWASP Dependency-Check: no HIGH/CRITICAL CVEs
- [ ] Javadoc on all public API
