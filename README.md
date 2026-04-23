# Smart Campus API Coursework

## Project Overview
This project implements a RESTful Smart Campus backend using JAX-RS (Jersey) deployed as a WAR on Apache Tomcat.

Base API path:
http://localhost:8080/campus_api/api/v1

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
- Apache Tomcat 9.x

Steps:
1. Open a terminal in the project root.
2. Build the WAR:
   mvn package
3. Copy the WAR to Tomcat webapps:
   copy target\campus_api.war <TOMCAT_HOME>\webapps\campus_api.war
4. Start Tomcat:
   cmd /c ""<TOMCAT_HOME>\bin\startup.bat""
5. Wait until Tomcat deploys campus_api.war, then test:
   http://localhost:8080/campus_api/api/v1
6. Stop Tomcat when done:
   cmd /c ""<TOMCAT_HOME>\bin\shutdown.bat""

Important (Windows): set both variables before startup if you see
"CATALINA_HOME environment variable is not defined correctly".

```powershell
$env:CATALINA_HOME = "<TOMCAT_HOME>"
$env:CATALINA_BASE = "<TOMCAT_HOME>"
```

## Download Tomcat 9 On Windows (PowerShell)
Run these commands once:

```powershell
$tomcatVersion = "9.0.89"
$zipUrl = "https://archive.apache.org/dist/tomcat/tomcat-9/v$tomcatVersion/bin/apache-tomcat-$tomcatVersion-windows-x64.zip"
$destRoot = "$PWD\tools"
New-Item -ItemType Directory -Force -Path $destRoot | Out-Null
$zipPath = "$destRoot\apache-tomcat-$tomcatVersion-windows-x64.zip"
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath
Expand-Archive -Path $zipPath -DestinationPath $destRoot -Force
```

After extraction, set:

```powershell
$env:TOMCAT_HOME = "$PWD\tools\apache-tomcat-9.0.89"
$env:CATALINA_HOME = $env:TOMCAT_HOME
$env:CATALINA_BASE = $env:TOMCAT_HOME
```

## One-Command Deploy On Windows
This repository includes helper scripts:

```powershell
# Build (if needed), deploy WAR, start Tomcat, and wait for API health
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-tomcat.ps1

# Stop Tomcat and free port 8080
powershell -ExecutionPolicy Bypass -File .\scripts\stop-tomcat.ps1
```

## API Design Summary
Top-level collections:
- GET /campus_api/api/v1
- GET /campus_api/api/v1/rooms
- POST /campus_api/api/v1/rooms
- GET /campus_api/api/v1/rooms/{roomId}
- DELETE /campus_api/api/v1/rooms/{roomId}
- GET /campus_api/api/v1/sensors
- POST /campus_api/api/v1/sensors
- GET /campus_api/api/v1/sensors/{sensorId}/readings
- POST /campus_api/api/v1/sensors/{sensorId}/readings

Business rules implemented:
- A room cannot be deleted while sensors are assigned to it
- A sensor cannot be created if its roomId does not exist
- A MAINTENANCE sensor cannot accept new readings
- Posting a new reading updates Sensor.currentValue

## Sample curl Commands
Use these in order so linked resources exist.

PowerShell (recommended on Windows): use curl.exe (not curl) and `--%` for JSON payload commands.

If you prefer native PowerShell requests (no quote escaping issues), use:

```powershell
$room = @{ id = "LIB-301"; name = "Library Quiet Study"; capacity = 120 } | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8080/campus_api/api/v1/rooms" -Method Post -Body $room -ContentType "application/json" -UseBasicParsing
```

1) Discovery endpoint
curl.exe -X GET http://localhost:8080/campus_api/api/v1

2) Create a room
curl.exe --% -X POST http://localhost:8080/campus_api/api/v1/rooms -H "Content-Type: application/json" -d "{\"id\":\"LIB-301\",\"name\":\"Library Quiet Study\",\"capacity\":120}"

3) List all rooms
curl.exe -X GET http://localhost:8080/campus_api/api/v1/rooms

4) Create a sensor linked to the room
curl.exe --% -X POST http://localhost:8080/campus_api/api/v1/sensors -H "Content-Type: application/json" -d "{\"id\":\"CO2-001\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0.0,\"roomId\":\"LIB-301\"}"

5) Filter sensors by type
curl.exe -X GET "http://localhost:8080/campus_api/api/v1/sensors?type=CO2"

6) Add a reading for a sensor
curl.exe --% -X POST http://localhost:8080/campus_api/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d "{\"value\":438.7}"

7) Get reading history for a sensor
curl.exe -X GET http://localhost:8080/campus_api/api/v1/sensors/CO2-001/readings

Git Bash / WSL alternative (same flow, no --% needed):

1) curl -X GET http://localhost:8080/campus_api/api/v1
2) curl -X POST http://localhost:8080/campus_api/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":120}'
3) curl -X GET http://localhost:8080/campus_api/api/v1/rooms
4) curl -X POST http://localhost:8080/campus_api/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
5) curl -X GET "http://localhost:8080/campus_api/api/v1/sensors?type=CO2"
6) curl -X POST http://localhost:8080/campus_api/api/v1/sensors/CO2-001/readings -H "Content-Type: application/json" -d '{"value":438.7}'
7) curl -X GET http://localhost:8080/campus_api/api/v1/sensors/CO2-001/readings

## Expected Status Codes
- 200 OK for successful GET and delete confirmation
- 201 Created for successful POST creation
- 403 Forbidden when posting readings to a MAINTENANCE sensor
- 409 Conflict when deleting a room that still has sensors
- 422 Unprocessable Entity when sensor creation references a missing room
- 500 Internal Server Error for unexpected failures (generic JSON error body)
