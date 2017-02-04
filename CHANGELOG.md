# Change Log

## [Unreleased]
### Changed
- Component names if not configured, will now be derived from the key structure of the JSON payload (#16)

## 1.1.0
### Added
- JSON endpoint /all.json
- Status endpoint /status.json
- Prevent hover state getting lost across page refreshes
- Support JSON responses for status codes other than 200 (#15)

### Bug fixes
- Handle misconfigurations gracefully (#13)
- Show internal errors when parsing status in the UI rather than quietly swallowing them (#18)

## 1.0.0
### Added
- Ability to read overall status from components
- Feedback on misconfigured keys
- Ability to select unhealthy systems only

### Bug fixes
- Correctly read `false` value from color
