# Email Configuration

This document describes how to configure email sending in OpenDroneDiary.

## SMTP Configuration

OpenDroneDiary uses generic SMTP connection for sending emails. Configure the following environment variables:

### Required Environment Variables

- `SMTP_HOST` - SMTP server hostname (e.g., smtp.gmail.com)
- `SMTP_USERNAME` - SMTP authentication username
- `SMTP_PASSWORD` - SMTP authentication password

### Optional Environment Variables

- `SMTP_PORT` - SMTP server port (default: 587)
- `SMTP_FROM_EMAIL` - From email address (default: noreply@opendronediary.com)
- `SMTP_FROM_NAME` - From display name (default: OpenDroneDiary)
- `SMTP_USE_TLS` - Enable STARTTLS (default: true)
- `SMTP_USE_SSL` - Enable SSL/TLS (default: false)
- `BASE_URL` - Base URL for email links (default: https://opendronediary.herokuapp.com)

## Example Configuration

### Gmail SMTP
```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_FROM_EMAIL=your-email@gmail.com
SMTP_FROM_NAME=Your App Name
SMTP_USE_TLS=true
SMTP_USE_SSL=false
BASE_URL=https://yourdomain.com
```

### Other SMTP Providers
```bash
# For Office 365/Outlook.com
SMTP_HOST=smtp-mail.outlook.com
SMTP_PORT=587

# For Yahoo Mail
SMTP_HOST=smtp.mail.yahoo.com
SMTP_PORT=587

# For custom SMTP server
SMTP_HOST=mail.yourdomain.com
SMTP_PORT=587
```

## Development Mode

If SMTP configuration is not complete (missing host, username, or password), the email service will:
- Skip sending actual emails
- Log a message: "SMTP configuration not complete, skipping email send"
- Return `true` for graceful handling in development environments

## Email Types

The application sends the following types of emails:

1. **Welcome Email** - Sent when a user registers
2. **Password Reset Email** - Sent when a user requests password reset

Both emails are sent as HTML content with UTF-8 encoding.