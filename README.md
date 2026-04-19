# Smart Campus API Coursework

## Project Overview
This project implements a RESTful Smart Campus backend using JAX-RS (Jersey) and an embedded Grizzly HTTP server.

Base API path:
http://localhost:8080/api/v1

The API models three core resources:
- Room: a physical campus space with capacity and linked sensors
- Sensor: a device assigned to a room
- SensorReading: time-series data for each sensor

Design highlights:
- Versioned entry point at /api/v1
- Nested resource handling for sensor readings via sub-resource locator
- In-memory storage using thread-safe maps and synchronized lists
- Structured JSON error responses through dedicated ExceptionMapper classes
- Global safety net mapper to avoid leaking internal stack traces
- Request and response logging through JAX-RS filters

## Build And Run (Step By Step)
Prerequisites:
- Java 17 or later
- Maven 3.8 or later

Steps:
1. Open a terminal in the project root.
2. Compile the project:
   mvn clean compile
3. Start the API server:
   mvn exec:java
4. Wait for this startup message:
   Smart Campus API started at http://localhost:8080/api/v1
5. Keep the server terminal running while testing endpoints.

Windows note if mvn is not on PATH:
- Use your Maven executable directly, for example:
  "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile
  "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" exec:java

## API Design Summary
Top-level collections:
- GET /api/v1
- GET /api/v1/rooms
- POST /api/v1/rooms
- GET /api/v1/rooms/{roomId}
- DELETE /api/v1/rooms/{roomId}
- GET /api/v1/sensors
- POST /api/v1/sensors
- GET /api/v1/sensors/{sensorId}/readings
- POST /api/v1/sensors/{sensorId}/readings

Business rules implemented:
- A room cannot be deleted while sensors are assigned to it
- A sensor cannot be created if its roomId does not exist
- A MAINTENANCE sensor cannot accept new readings
- Posting a new reading updates Sensor.currentValue

## Sample curl Commands
Use these in order so linked resources exist.

PowerShell (recommended on Windows): use curl.exe (not curl) and `--%` for JSON payload commands.

1) Discovery endpoint
curl.exe -X GET http://localhost:8080/api/v1

2) Create a room
curl.exe --% -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":120}"

3) List all rooms
curl.exe -X GET http://localhost:8080/api/v1/rooms

4) Create a sensor linked to the room
curl.exe --% -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"LIB-301\"}"

5) Filter sensors by type
curl.exe -X GET "http://localhost:8080/api/v1/sensors?type=CO2"

6) Add a reading for a sensor
curl.exe --% -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d "{\"value\":438.7}"

7) Get reading history for a sensor
curl.exe -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings

Git Bash / WSL alternative (same flow, no --% needed):

1) curl -X GET http://localhost:8080/api/v1
2) curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":120}'
3) curl -X GET http://localhost:8080/api/v1/rooms
4) curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
5) curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
6) curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d '{"value":438.7}'
7) curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings

## Expected Status Codes
- 200 OK for successful GET and delete confirmation
- 201 Created for successful POST creation
- 403 Forbidden when posting readings to a MAINTENANCE sensor
- 409 Conflict when deleting a room that still has sensors
- 422 Unprocessable Entity when sensor creation references a missing room
- 500 Internal Server Error for unexpected failures (generic JSON error body)
