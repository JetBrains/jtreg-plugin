# JTReg plugin

## [1.1.4]
### Fixed
- Fix escaping paths from Intellij project classpath

## [1.1.3]
### Fixed
- Use JTreg built-in libraries in project index for JUnit and TestNG tests

## [1.1.2]
### Fixed
- Fix the issue with resetting test java options after test run

## [1.1.1]
### Added
- Set Intellij v261.x as supported version

## [1.1.0]
### Added
- Upgrade Intellij Platform Plugin to 2.9.0

### Fixed
- Fix persistence of run/debug configurations

## [1.0.0]
### Added
- Run and Debug Configurations
  - Added support for executing individual test files or all tests within a selected directory.
  - Implemented automatic detection of local JDK builds and associated native libraries from the project’s build directory.
- Automatic Test File Registration
  - Introduced a file listener that automatically incorporates previously unindexed test files into the project structure.
- Global JTReg Options
  - Added the ability to configure general JTReg options at a global level, with per-configuration overrides available in run/debug settings.
- Java Options and Environment Variables
  - Provided controls for specifying Java options and environment variables for the test JDK within run/debug configurations.
- Test Filtering
  - Implemented filtering capabilities based on category (automatic, manual), keywords, and exclusion lists.
- Test Mode Selection
  - Enabled selection of the JTReg execution mode (agentvm, othervm) within run/debug configurations.
- Concurrency Management
  - Added concurrency settings to manage parallel test execution.

