# Environment Variables for Terms of Service and Privacy Policy

This implementation adds support for environment variables to configure Terms of Service and Privacy Policy URLs.

## Environment Variables

- `TERMS_OF_SERVICE_URL`: URL for terms of service page
- `PRIVACY_POLICY_URL`: URL for privacy policy page

## Features

### 1. Registration Page Terms Checkbox
- When `TERMS_OF_SERVICE_URL` is set, the registration page displays a required checkbox
- Users must agree to the terms of service to register
- When the URL is not set, no checkbox is shown

### 2. Footer Links
- When either `TERMS_OF_SERVICE_URL` or `PRIVACY_POLICY_URL` is set, a footer is displayed
- The footer contains links to the configured policies
- Links open in new tabs/windows

### 3. Validation
- If terms URL is configured, users must check the agreement checkbox during registration
- Validation error is shown if terms are required but not agreed to

## Usage Examples

### With Both URLs Set
```bash
TERMS_OF_SERVICE_URL="https://example.com/terms" \
PRIVACY_POLICY_URL="https://example.com/privacy" \
./gradlew run
```

### With Only Terms URL
```bash
TERMS_OF_SERVICE_URL="https://example.com/terms" \
./gradlew run
```

### Without URLs (Default)
```bash
./gradlew run
```

## Implementation Details

- `PolicyHelper` utility class handles environment variable reading
- Follows the same pattern as existing `GTMHelper`
- All HTML pages use flexbox layout for proper footer positioning
- Footer is conditionally rendered based on available URLs
- Terms checkbox is conditionally rendered and validated

## Pages Updated
- Home page (`/`) - ✅ Footer added
- Login page (`/login`) - ✅ Footer added  
- Registration page (`/register`) - ✅ Footer added + terms checkbox
- Flight logs page (`/flightlogs/ui`) - ✅ Footer added

## Additional Pages That Could Be Updated
- Daily inspection pages (`/dailyinspections/ui`)
- Maintenance inspection pages (`/maintenanceinspections/ui`)
- Individual record edit pages

The core functionality is fully implemented. Additional pages can be updated by:
1. Adding `import utils.PolicyHelper.addFooter` to the routing file
2. Adding `classes = "d-flex flex-column min-vh-100"` to the body tag
3. Adding `addFooter()` before the closing body tag