# Tenant Info API Documentation

## Overview
The `/api/tenant/info` endpoint retrieves tenant information and associated quickation options based on an assistant ID. This API returns structured data including tenant details and available options with their corresponding questions.

## Endpoint Details

### POST `/api/tenant/info`

**Description**: Retrieves tenant information and options for a given assistant ID.

**Authentication**: Bearer Token required

**Content-Type**: `application/json`

## Request

### Headers
```
Authorization: Bearer <token> (optional)
Content-Type: application/json
```

### Request Body
```json
{
  "assistantId": 1126
}
```

### Request Parameters
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| assistantId | Integer | Yes | The unique identifier of the assistant |

## Response

### Success Response (200 OK)
```json
{
  "statusCode": 200,
  "message": "Tenant information retrieved successfully",
  "data": {
    "assistant_id": 1126,
    "name": "Ruby FB_Vanuatu",
    "tenant_id": "US_DIG_544c3577",
    "options": {
      "Special Offers": "What are the special offers?",
      "DTH offers": "What are the DTH offers?",
      "Digital apps": "What are the digital apps available?",
      "Contact us": "How can I contact you?",
      "Service plan and packages": "What service plans and packages are available?",
      "Bestselling mobile devices": "What are the bestselling mobile devices?",
      "Smart Solutions": "What smart solutions do you offer?",
      "Live Sports": "What live sports options are available?"
    }
  }
}
```

### Error Response (400 Bad Request)
```json
{
  "statusCode": 400,
  "message": "Assistant configuration not found for ID: 1126",
  "data": null
}
```

### Error Response (500 Internal Server Error)
```json
{
  "statusCode": 500,
  "message": "Error retrieving tenant information",
  "data": null
}
```

## Database Structure

### Table: `assistant_options`

**After Migration Schema:**
```sql
+--------------+--------------+------+-----+---------+----------------+
| Field        | Type         | Null | Key | Default | Extra          |
+--------------+--------------+------+-----+---------+----------------+
| id           | int(11)      | NO   | PRI | NULL    | auto_increment |
| assistant_id | int(11)      | NO   | MUL | NULL    |                |
| name         | varchar(255) | NO   |     | NULL    |                |
| options_json | longtext     | YES  |     | NULL    |                |
| tenant_id    | varchar(255) | NO   |     | NULL    |                |
+--------------+--------------+------+-----+---------+----------------+
```

**Sample Data After Migration:**
```sql
mysql> SELECT * FROM assistant_options LIMIT 1\G;
*************************** 1. row ***************************
          id: 1
assistant_id: 1126
        name: Ruby FB_Vanuatu
options_json: {"Special Offers": "What are the special offers?", "DTH offers": "What are the DTH offers?", "Digital apps": "What are the digital apps available?", "Contact us": "How can I contact you?", "Service plan and packages": "What service plans and packages are available?", "Bestselling mobile devices": "What are the bestselling mobile devices?", "Smart Solutions": "What smart solutions do you offer?", "Live Sports": "What live sports options are available?"}
   tenant_id: US_DIG_544c3577
```

### Table: `assistant_configuration`
Referenced table for assistant details:
```sql
| Field                      | Type         | Description                    |
|----------------------------|--------------|--------------------------------|
| id                         | int(11)      | Primary key (assistant_id)     |
| tenant_id                  | varchar(255) | Tenant identifier              |
| name                       | varchar(255) | Assistant name                 |
| status                     | boolean      | Assistant active status        |
| ... (other fields)         |              |                               |
```

## API Flow

### 1. Request Processing
```
POST /api/tenant/info
├── Authentication validation (optional)
├── Request body parsing
└── Extract assistantId from request
```

### 2. Data Retrieval Flow
```
TenantInfoService.getTenantInfoByAssistantId(assistantId)
├── Step 1: Validate assistant exists
│   ├── Query: assistant_configuration table
│   ├── Success: Get name, tenant_id
│   └── Failure: Throw IllegalArgumentException
├── Step 2: Get options with fallback
│   ├── Primary: Query assistant_options by assistant_id
│   │   ├── Found: Parse options_json as Map<String, String>
│   │   └── Not found/empty: Fallback to property file
│   └── Fallback: Use app.quickation.items property
│       ├── Parse CSV values
│       └── Convert to Map (key = value for property items)
└── Step 3: Build response
    └── Return TenantInfoResponse with Map<String, String> options
```

### 3. JSON Parsing Logic
```java
// Primary: Try to parse as JSON object
Map<String, String> options = parseAsMap(options_json);

// Fallback: Parse as JSON array (backward compatibility)
if (parseAsMap fails) {
    List<String> optionsList = parseAsArray(options_json);
    Map<String, String> options = convertToMap(optionsList);
}
```

## Response Format Changes

### Before (Array Format)
```json
{
  "options": [
    "Special Offers",
    "DTH offers",
    "Digital apps",
    "Contact us"
  ]
}
```

### After (Object Format)
```json
{
  "options": {
    "Special Offers": "What are the special offers?",
    "DTH offers": "What are the DTH offers?",
    "Digital apps": "What are the digital apps available?",
    "Contact us": "How can I contact you?"
  }
}
```

## Configuration

### Property File Fallback
```properties
app.quickation.items=Special Offers,DTH offers,Digital apps,Contact us,Service plan and packages,Bestselling mobile devices,Smart Solutions,Live Sports
```

## Data Migration

### Migration Script
The migration converts existing array-based JSON to object-based JSON:

```sql
-- Convert from array format
["Special Offers", "DTH offers", "Digital apps"]

-- To object format
{
  "Special Offers": "What are the special offers?",
  "DTH offers": "What are the DTH offers?",
  "Digital apps": "What are the digital apps available?"
}
```

## Error Handling

### Validation Errors
- **400 Bad Request**: Invalid assistant ID or assistant not found
- **500 Internal Server Error**: Database connection issues or unexpected errors

### Logging
```java
logger.info("Received request to get tenant info for assistant ID: {}", assistantId);
logger.debug("Found assistant: name={}, tenantId={} for assistantId={}", name, tenantId, assistantId);
logger.debug("Found options from DB by assistantId: {} options", optionsCount);
```

## Usage Examples

### cURL Example
```bash
curl -X POST "http://localhost:8080/api/tenant/info" \
  -H "Authorization: Bearer <your-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "assistantId": 1126
  }'
```

### JavaScript Example
```javascript
const response = await fetch('/api/tenant/info', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    assistantId: 1126
  })
});

const data = await response.json();
console.log(data.data.options); // Object with key-value pairs
```

## Backward Compatibility

The API maintains backward compatibility by:
1. **Database**: Supports both array and object formats in `options_json`
2. **Parsing**: Falls back to array parsing if object parsing fails
3. **Response**: Always returns object format, converting arrays when needed
4. **Properties**: Continues to support CSV format in property files

## Performance Considerations

- **Database Indexing**: Ensure `assistant_id` is indexed in both tables
- **Caching**: Consider caching frequently accessed tenant information
- **Connection Pooling**: Use appropriate database connection pool settings
- **JSON Parsing**: Efficient JSON parsing with Jackson ObjectMapper

## Security

- **Authentication**: Bearer token validation (optional)
- **Input Validation**: Assistant ID validation and sanitization
- **Error Handling**: Secure error messages without exposing internal details
- **SQL Injection**: Protected by JPA/Hibernate parameter binding 