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

### Conceptual Report

### Introduction
This report details the architectural design and implementation of the “Smart Campus” Sensor & Room Management API. The system is built as a high-performance web service using JAX-RS, focusing on RESTful principles, resource nesting, and robust error handling to manage a complex hierarchy of campus rooms and sensors.

### PART 1: SETUP & DISCOVERY

#### 1.1 Architecture & Configuration: Resource Lifecycle & Synchronisation
In JAX-RS implementations such as Jersey, resource classes—including `RoomResource`, `SensorResource`, and `DiscoveryResource`—are request-scoped by default. This architecture ensures that a new, independent instance of the resource class is instantiated for every incoming HTTP request and subsequently destroyed once the response is dispatched. This design choice inherently guarantees thread safety for instance variables declared within the resource class, as they are never shared between concurrent requests.

However, our API utilizes `ResourceConfig.forApplication(new ApiApplication())` for bootstrapping and relies on an `InMemoryStore` to persist data across multiple requests. To facilitate this persistence, the `InMemoryStore` is implemented as a Singleton. Consequently, multiple request-scoped resource threads concurrently access and mutate this single, shared state. To prevent critical race conditions and maintain data integrity, the following thread-safety mechanisms are employed:

- **Thread-Safe Collections:** The store utilizes `ConcurrentHashMap` for rooms and sensors, ensuring atomic operations for basic data retrieval and updates. Additionally, `Collections.synchronizedList` is used for sensor readings to prevent data corruption during concurrent additions.
- **Synchronized Blocks:** When performing compound mutations involving multiple objects—such as adding a new sensor while simultaneously updating the parent room’s `sensorIds` list within `SensorResource.java`—we utilize explicit `synchronized (room)` blocks. This ensures that the relationship between the two entities remains consistent and accurate.
- **Trade-offs:** While in-memory synchronization is efficient and suitable for this coursework, it lacks scalability in distributed, multi-server RESTful architectures (horizontal scaling). In a production environment, state management would typically be delegated to a distributed database system, such as PostgreSQL or Redis, which handles ACID transactions and concurrency control inherently.

#### 1.2 Discovery Endpoint: Exceptional Justification of HATEOAS
HATEOAS (Hypermedia As The Engine Of Application State) represents a fundamental shift in how clients interact with an API. While traditional static documentation requires developers to hardcode specific endpoint URIs, HATEOAS enables the API to be self-documenting and dynamically discoverable.

By returning resource maps in our `/api/v1` discovery endpoint, we achieve several significant advantages:
1. **Reduced Client Coupling:** Clients do not require prior knowledge of exact URI structures. They only need the primary entry point and the semantic relationships (e.g., following the “rooms” link).
2. **API Evolution and Maintenance:** If the server needs to restructure its endpoints, the server simply updates the link returned in the discovery payload, and the client follows the new path without requiring code changes.
3. **State-Driven Discoverability:** HATEOAS can dictate the application state dynamically, providing links only when they are relevant to the current state or user permissions.

### PART 2: ROOM MANAGEMENT

#### 2.1 Room Implementation: Analysis of ID-only vs. Full-Object Returns
When designing list endpoints like `GET /rooms`, architects must balance the trade-offs between returning full objects versus identifiers (IDs).

- **Full-Object Returns (Current Implementation):** Highly efficient for client-side rendering as it eliminates the need for secondary fetches. However, as datasets grow, this approach increases payload overhead and network bandwidth consumption.
- **ID-only Returns:** Lightweight and fast, but forces the client to make subsequent `GET /rooms/{id}` requests for each item (the “N+1 query” problem), leading to high network latency.
- **Optimization Strategies:** For a scalable API, superior strategies include Field Filtering (allowing clients to request specific fields) and Pagination (limiting the number of objects returned per request).

#### 2.2 Deletion & Logic: Justification of Idempotency
In REST principles, an HTTP method is idempotent if the intended effect on the server of multiple identical requests is the same as the effect of a single request. The `DELETE /rooms/{roomId}` operation is designed to be strictly idempotent.

If a client successfully calls `DELETE`, the room is removed. If the client retries the same request (e.g., due to a timeout), the server will return a `404 Not Found`, but crucially, the underlying server state—the absence of that specific room—remains identical to the state after the first call. No further side effects occur, allowing for safe automatic retry mechanisms.

### PART 3: SENSORS & FILTERING

#### 3.1 Sensor Integrity: 415 Unsupported Media Type Consequences
JAX-RS utilizes the `@Consumes` annotation to define the precise MIME type an endpoint expects. While our logic uses `422 Unprocessable Entity` for semantic errors, JAX-RS provides a primary defense using `415 Unsupported Media Type` for syntactical mismatches.

If a client sends an unsupported format (e.g., XML to a JSON endpoint), JAX-RS intercepts the request immediately. This provides Fail-Fast Security by preventing the server’s parsers from attempting to deserialize potentially malicious payloads and ensures Resource Efficiency by bypassing application-level validation for invalid media types.

#### 3.2 Filtered Retrieval: QueryParams vs. PathParams Contrast
- **PathParams (`/sensors/{id}`):** Strictly hierarchical, used to identify a specific, unique resource.
- **QueryParams (`/sensors?type=temperature`):** Non-hierarchical modifiers used to filter, sort, or paginate a collection.

Query strings are superior for collection filtering because they offer flexibility without altering the fundamental URI structure. Filters are inherently optional and combinable, whereas path-based filtering leads to rigid and unmaintainable URI structures.

### PART 4: SUB-RESOURCES

#### 4.1 Sub-Resource Locator: Managing Complexity & Delegation
The Sub-Resource Locator pattern (`@Path("/{sensorId}/readings")` delegating to `SensorReadingResource`) is a powerful solution for managing API complexity. By utilizing this pattern, `SensorResource` acts as a router, achieving:
- **Single Responsibility Principle:** Code organization is improved by delegating reading-specific logic to a dedicated class.
- **Intuitive URI Structure:** It enforces a logical, nested hierarchy that accurately reflects the parent-child relationship between sensors and their readings.

#### 4.2 Historical Management: Atomicity and Consistency
In a concurrent environment, updating a sensor’s `currentValue` during a new reading post poses a risk of race conditions. While `ConcurrentHashMap` is thread-safe for storage, modifying an object retrieved from it is not inherently atomic. To guarantee a consistent update, we utilize synchronization (e.g., `synchronized (sensor)` blocks). This ensures that recording the reading and updating the parent’s state are executed as an atomic operation, preventing interleaved execution from corrupting the sensor’s state.

### PART 5: ERROR HANDLING

#### 5.1 Specific Exceptions: Analysis of 422 vs. 404
When processing a valid JSON payload with a logical error (e.g., a non-existent `roomId`), `422 Unprocessable Entity` is superior to `404 Not Found`. `404` indicates the URI itself is missing, while `422` communicates that the server understood the request but the semantics were invalid. This precision prevents clients from unnecessarily debugging URI paths.

#### 5.2 Global Safety Net: Cybersecurity Risks of Stack Traces
Exposing raw stack traces to clients represents a critical vulnerability known as Information Disclosure. A stack trace provides attackers with Reconnaissance (frameworks, file paths), Targeted Exploitation (library versions), and Reverse Engineering (logic flow) data. By using a global `ExceptionMapper<Throwable>` to return sanitized `500` responses, we deny malicious actors this data while logging the actual trace securely on the server.
