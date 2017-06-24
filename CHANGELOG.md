# Change Log

## 2.0.0
### Added
- Components are now also understood as a map (not just a list)

### Changed
- Component names if not configured, will now be derived from the key structure of the JSON payload, instead of assuming that a `name` field exists (#16)
- Interpretation of json-path queries closer to the standard (see https://github.com/gga/json-path/blob/master/CHANGELOG.md)

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
