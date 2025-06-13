# Sage Intacct Environment Migration Guide

## 🚨 Urgent: Partner Environment Deprecation

**Effective Date:** June 13, 2025  
**Impact:** All applications using `partner.intacct.com` must migrate to `api-partner-main.intacct.com`

## What Changed

Sage Intacct has deprecated the legacy partner environment and introduced new partner environments for better security and performance.

### Old Environment (Deprecated)
```
https://partner.intacct.com/ia/api/v1-beta2
```

### New Environments
You will be assigned a specific new partner environment URL, such as:
```
https://p301.intacct.com/ia/api/v1-beta2
https://p302.intacct.com/ia/api/v1-beta2
https://p303.intacct.com/ia/api/v1-beta2
... (and others)
```

## How to Get Your New Environment URL

1. **Contact your Sage Intacct Administrator**
2. **Request SSO or OTP Access** to the new partner environments
3. **Refer to Sage Intacct documentation**: "External Access to Main, Release, and OCR (p301–p306)"

## Migration Steps for MCP Server

### 1. Update System Properties
```bash
# OLD
-Dintacct.base.url=https://partner.intacct.com/ia/api/v1-beta2

# NEW (replace with your assigned environment)
-Dintacct.base.url=https://api-partner-main.intacct.com/ia/api/v1-beta2
```

### 2. Update Claude Desktop Configuration
```json
{
  "Model and Query": {
    "command": "java",
    "args": [
      "-Dspring.ai.mcp.server.stdio=true",
      "-Dspring.main.web-application-type=none",
      "-Dlogging.pattern.console=",
      "-Dintacct.base.url=https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2",
      "-DOAUTH2_CLIENT_ID=YOUR_CLIENT_ID",
      "-DOAUTH2_CLIENT_SECRET=YOUR_CLIENT_SECRET",
      "-DOAUTH2_USERNAME=YOUR_USERNAME",
      "-DOAUTH2_PASSWORD=YOUR_PASSWORD",
      "-jar",
      "path/to/mcp-query-stdio-server-0.1.0.jar"
    ]
  }
}
```

### 3. Update Environment Variables
```bash
# Update all environment variables
export INTACCT_BASE_URL=https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2
export OAUTH2_AUTH_URI=https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2/oauth2/authorize
export OAUTH2_TOKEN_URI=https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2/oauth2/token
```

### 4. Update application.yml
```yaml
mcp:
  server:
    auth:
      base-url: https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2
      oauth2:
        authorization-uri: https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2/oauth2/authorize
        token-uri: https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2/oauth2/token
```

## Testing Your Migration

### 1. Test with curl
```bash
# Test authentication with your new environment
curl -X POST https://YOUR-NEW-ENV.intacct.com/ia/api/v1-beta2/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "password",
    "client_id": "YOUR_CLIENT_ID",
    "client_secret": "YOUR_CLIENT_SECRET",
    "username": "YOUR_USERNAME",
    "password": "YOUR_PASSWORD"
  }'
```

### 2. Test MCP Server
```bash
# Rebuild and test
mvn clean package -DskipTests
java -jar target/mcp-query-stdio-server-0.1.0.jar
```

## Common Issues and Solutions

### Issue: "Connection refused" or "Unknown host"
**Solution:** Verify your new environment URL is correct and accessible

### Issue: "Invalid credentials"
**Solution:** Your OAuth2 credentials may need to be updated for the new environment

### Issue: "Token not valid"
**Solution:** Clear any cached tokens and obtain a fresh token from the new environment

## Verification Checklist

- [ ] Obtained new partner environment URL from Sage Intacct
- [ ] Updated all configuration files and environment variables
- [ ] Tested authentication with curl
- [ ] Rebuilt MCP server with new configuration
- [ ] Verified MCP server can obtain access tokens
- [ ] Tested query functionality in Claude Desktop
- [ ] Updated any CI/CD pipelines or deployment scripts

## Support

If you encounter issues during migration:

1. **Check Sage Intacct Documentation**: "External Access to Main, Release, and OCR"
2. **Contact Sage Intacct Support**: For environment-specific issues
3. **Verify Network Access**: Ensure your network can reach the new environment
4. **Check Credentials**: Verify OAuth2 credentials work with the new environment

## Timeline

- **June 13, 2025**: Legacy `partner.intacct.com` deprecated, migrate to `api-partner-main.intacct.com`
- **Immediate Action Required**: Migrate to new environment URLs
- **No Grace Period**: Applications must be updated immediately

---

**Remember:** Replace `YOUR-NEW-ENV` with your actual assigned partner environment URL (e.g., `p301`, `