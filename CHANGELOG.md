# Changelog - fmi-avi-messageconverter-tac

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- ...

### Changed

- ...

### Deprecated

- ...

### Removed

- ...

### Fixed

- ...

### Security

- ...

## [v6.0.0-beta3] - 2023-11-14

### Changed

- Added partial support for minimal test SIGMET messages [#180], [#188]

### Fixed

- Fixed SigmetWithinRadius coordinate serialization [#186]

## [v6.0.0-beta2] - 2023-10-23

### Changed

- Adapted to SIGMET and AIRMET interface changes in fmi-avi-messageconverter [#136]

### Fixed

- Fixed FRQ_TCU comparison in AirmetPhenomenon [#138]
- Set PermissibleUsage correctly for AIRMET TAC parsing [#181]

## [v6.0.0-beta1] - 2023-09-27

### Added

- Experimental support for SIGMET/AIRMET parsing and serialization [#117]

## [v5.3.0] - 2023-02-15

### Added

- ConversionHint for disabling line wrapping in TAC serialization [#120]

### Changed

- Depend on fmi-avi-messageconverter:6.3.0

## [v5.2.0] - 2022-08-24

### Changed

- Remove ICAO code to country mapping in ICAOCode lexeme [#118]
- Depend on fmi-avi-messageconverter:6.2.0

## [v5.1.0] - 2022-06-06

### Changed

- Depend on fmi-avi-messageconverter:6.1.0

## [v5.0.0] - 2022-02-22

### Changed

- Removed preceding CR/LF characters in front of serialized/parsed bulletin. [#102]
- Adapted to location indicator model changes in GenericAviationMessage. [#106]
- Separated generic message parsing from generic bulletin parsing. [#107]
- Depend on fmi-avi-messageconverter:6.0.0

### Fixed

- Fixed NullPointerException in Lexing. [#114]

## [v4.0.0] - 2021-04-13

### Added

- Created overview documentation for developers. [#95]

### Changed

- Adapted TAF TAC support to model changes for IWXXM 3. [#97]
- Adapted to `AviationWeatherMessage.getReportStatus()` being non-`Optional`. [#90]
- Code quality enhancements. [#101]

## Past Changelog

Previous changelog entries are available
on [GitHub releases page](https://github.com/fmidev/fmi-avi-messageconverter-tac/releases) in a more freeform format.

[Unreleased]: https://github.com/fmidev/fmi-avi-messageconverter-tac/compare/fmi-avi-messageconverter-tac-5.3.0...HEAD

[v6.0.0-beta3]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-6.0.0-beta3

[v6.0.0-beta2]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-6.0.0-beta2

[v6.0.0-beta1]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-6.0.0-beta1

[v5.3.0]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-5.3.0

[v5.2.0]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-5.2.0

[v5.1.0]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-5.1.0

[v5.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-5.0.0

[v4.0.0]: https://github.com/fmidev/fmi-avi-messageconverter-tac/releases/tag/fmi-avi-messageconverter-tac-4.0.0

[#90]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/90

[#95]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/95

[#97]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/97

[#101]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/101

[#102]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/102

[#106]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/106

[#107]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/107

[#114]: https://github.com/fmidev/fmi-avi-messageconverter-tac/pull/114

[#117]: https://github.com/fmidev/fmi-avi-messageconverter-tac/pull/117

[#118]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/118

[#120]: https://github.com/fmidev/fmi-avi-messageconverter-tac/pull/120

[#136]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/136

[#138]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/138

[#180]: https://github.com/fmidev/fmi-avi-messageconverter-tac/pull/180

[#181]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/181

[#186]: https://github.com/fmidev/fmi-avi-messageconverter-tac/issues/186

[#188]: https://github.com/fmidev/fmi-avi-messageconverter-tac/pull/188


