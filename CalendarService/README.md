# Calendar Service

A robust, scalable Spring Boot service designed to determine whether a given date is a holiday, work date, or weekend. It provides APIs to check date types and find the next/previous work dates, supporting multiple countries, time zones, and custom business calendars.

## Features

- **Date Type Classification**: Identify if a date is a work day, holiday, or weekend
- **Work Date Navigation**: Find next and previous work dates
- **Multi-Region Support**: Handle different countries and their specific holidays
- **Custom Business Rules**: Support for organization-specific calendars
- **High Performance**: Optimized for high-throughput scenarios with multi-level caching
- **Real-time Updates**: Support for dynamic holiday configuration changes

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **PostgreSQL** - Primary database
- **Redis** - Caching layer
- **Flyway** - Database migrations
- **WebClient** - External API integration
- **Maven** - Build tool

## API Endpoints

### 1. Check Date Type
```http
GET /api/v1/calendar/date-type?date=2024-07-04&country=US
```

**Response:**
```json
{
  "date": "2024-07-04",
  "dateType": "HOLIDAY",
  "isWorkDay": false,
  "holidayName": "Independence Day",
  "country": "US",
  "metadata": {
    "dayOfWeek": "THURSDAY",
    "weekNumber": 27,
    "isWeekend": false
  }
}
```

### 2. Get Next Work Date
```http
GET /api/v1/calendar/next-work-date?fromDate=2024-07-03&country=US
```

**Response:**
```json
{
  "fromDate": "2024-07-03",
  "nextWorkDate": "2024-07-05",
  "previousWorkDate": null,
  "daysSkipped": 1,
  "skippedDates": [
    {
      "date": "2024-07-04",
      "reason": "Independence Day Holiday",
      "dateType": "HOLIDAY"
    }
  ]
}
```

### 3. Get Previous Work Date
```http
GET /api/v1/calendar/previous-work-date?fromDate=2024-07-08&country=US
```
**Response:**
```json
{
	"fromDate": "2024-07-08",
	"nextWorkDate": null,
	"previousWorkDate": "2024-07-05",
	"daysSkipped": 2,
	"skippedDates": [
		{
			"date": "2024-07-07",
			"reason": "Weekend",
			"dateType": "WEEKEND"
		},
		{
			"date": "2024-07-06",
			"reason": "Weekend",
			"dateType": "WEEKEND"
		}
	]
}
```

### 4. Bulk Date Processing
```http
POST /api/v1/calendar/bulk-check
Content-Type: application/json

{
  "dates": ["2024-07-04", "2024-07-05", "2024-12-25"],
  "country": "US",
  "operations": ["DATE_TYPE", "NEXT_WORK_DATE"]
}
```
**Response:**
```json
{
	"dateTypeResults": [
		{
			"date": "2024-07-04",
			"dateType": "HOLIDAY",
			"isWorkDay": false,
			"holidayName": "Independence Day",
			"country": "US",
			"metadata": {
				"dayOfWeek": "Thursday",
				"weekNumber": 27,
				"isWeekend": false,
				"timezone": null
			}
		},
		{
			"date": "2024-07-05",
			"dateType": "WORK_DAY",
			"isWorkDay": true,
			"holidayName": null,
			"country": "US",
			"metadata": {
				"dayOfWeek": "Friday",
				"weekNumber": 27,
				"isWeekend": false,
				"timezone": null
			}
		},
		{
			"date": "2024-12-25",
			"dateType": "HOLIDAY",
			"isWorkDay": false,
			"holidayName": "Christmas Day",
			"country": "US",
			"metadata": {
				"dayOfWeek": "Wednesday",
				"weekNumber": 52,
				"isWeekend": false,
				"timezone": null
			}
		}
	],
	"workDateResults": [
		{
			"fromDate": "2024-07-04",
			"nextWorkDate": "2024-07-05",
			"previousWorkDate": null,
			"daysSkipped": 0,
			"skippedDates": []
		},
		{
			"fromDate": "2024-07-05",
			"nextWorkDate": "2024-07-08",
			"previousWorkDate": null,
			"daysSkipped": 2,
			"skippedDates": [
				{
					"date": "2024-07-06",
					"reason": "Weekend",
					"dateType": "WEEKEND"
				},
				{
					"date": "2024-07-07",
					"reason": "Weekend",
					"dateType": "WEEKEND"
				}
			]
		},
		{
			"fromDate": "2024-12-25",
			"nextWorkDate": "2024-12-26",
			"previousWorkDate": null,
			"daysSkipped": 0,
			"skippedDates": []
		}
	],
	"totalProcessed": 3,
	"processingTimeMs": 1208
}
```
### 5. Get Available Countries
```http
GET /api/v1/calendar/countries
```

### 6. Check Country Support
```http
GET /api/v1/calendar/countries/US/supported
```

## Database Schema

The service uses PostgreSQL with the following main entities:

- **countries**: Country definitions with timezone information
- **holidays**: Holiday definitions with recurrence rules
- **business_calendars**: Custom business calendar definitions
- **business_calendar_rules**: Rules within business calendars
- **weekend_definitions**: Weekend day definitions per country

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
calendar-service:
  cache:
    ttl: PT24H
    max-entries: 100000
  
  external-apis:
    holiday-api:
      url: "https://api.holidayapi.com"
      timeout: 5s
      retry-attempts: 3
    
  performance:
    bulk-request-limit: 1000
    max-search-days: 30
```

### Environment Variables

- `DB_PASSWORD`: Database password
- `REDIS_HOST`: Redis host (default: localhost)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (optional)

## Running the Service

### Prerequisites

- Java 17+
- PostgreSQL 12+
- Redis 6+
- Maven 3.8+

### Local Development

1. **Start dependencies:**
   ```bash
   # Start PostgreSQL
   docker run --name calendar-postgres -e POSTGRES_PASSWORD=password -e POSTGRES_DB=calendar_service -p 5432:5432 -d postgres:15

   # Start Redis
   docker run --name calendar-redis -p 6379:6379 -d redis:7
   ```

2. **Set environment variables:**
   ```bash
   export DB_PASSWORD=password
   export REDIS_HOST=localhost
   export REDIS_PORT=6379
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

### Production Deployment

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Run with production profile:**
   ```bash
   java -jar target/CalendarService-1.0-SNAPSHOT.jar --spring.profiles.active=prod
   ```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Manual Testing

Test the API endpoints using curl:

```bash
# Check if July 4th is a holiday in US
curl "http://localhost:8080/calendar-service/api/v1/calendar/date-type?date=2024-07-04&country=US"

# Find next work date after July 4th
curl "http://localhost:8080/calendar-service/api/v1/calendar/next-work-date?fromDate=2024-07-04&country=US"

# Bulk check multiple dates
curl -X POST "http://localhost:8080/calendar-service/api/v1/calendar/bulk-check" \
  -H "Content-Type: application/json" \
  -d '{
    "dates": ["2024-07-04", "2024-07-05", "2024-12-25"],
    "country": "US",
    "operations": ["DATE_TYPE"]
  }'
```

## Architecture

### High-Level Architecture

```
Client Applications
       ↓
   API Gateway
       ↓
  Load Balancer
       ↓
Calendar Service Instances
       ↓
   Redis Cache
       ↓
  PostgreSQL DB
       ↓
External Holiday APIs
```

### Component Architecture

- **CalendarController**: REST API endpoints
- **CalendarService**: Main business logic orchestration
- **DateTypeChecker**: Determines date types
- **WorkDateFinder**: Finds next/previous work dates
- **HolidayResolver**: Resolves holidays with caching
- **BusinessCalendarService**: Manages custom business rules
- **ExternalHolidayClient**: Fetches real-time holiday data

## Performance Considerations

### Caching Strategy

The service implements a multi-level caching strategy:

1. **L1 Cache (In-Memory)**: Application-level caching for frequently accessed data
2. **L2 Cache (Redis)**: Distributed caching for shared data across instances
3. **Database**: Persistent storage with optimized indexes

### Performance Optimizations

- **Batch Processing**: Bulk operations for multiple dates
- **Connection Pooling**: Optimized database connections
- **Index Optimization**: Strategic database indexes for common queries
- **Async Processing**: Non-blocking external API calls

## Monitoring and Observability

The service includes:

- **Health Checks**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Logging**: Structured logging with different levels

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.
