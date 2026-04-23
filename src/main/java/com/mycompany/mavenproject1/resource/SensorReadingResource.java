package com.mycompany.mavenproject1.resource;

import com.mycompany.mavenproject1.exception.SensorUnavailableException;
import com.mycompany.mavenproject1.model.Sensor;
import com.mycompany.mavenproject1.model.SensorReading;
import com.mycompany.mavenproject1.store.InMemoryStore;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final InMemoryStore store = InMemoryStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getSensorReadings() {
        ensureSensorExists();
        List<SensorReading> source = store.getReadings(sensorId);
        List<SensorReading> readings;
        synchronized (source) {
            readings = new ArrayList<>(source);
        }
        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addSensorReading(SensorReading reading, @Context UriInfo uriInfo) {
        if (reading == null) {
            throw new BadRequestException("Sensor reading payload is required");
        }

        Sensor sensor = ensureSensorExists();

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException("Sensor " + sensorId + " is in MAINTENANCE mode and cannot accept new readings.");
        }

        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() <= 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        store.addReading(sensorId, reading);
        synchronized (sensor) {
            sensor.setCurrentValue(reading.getValue());
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(reading.getId()).build())
                .entity(reading)
                .build();
    }

    private Sensor ensureSensorExists() {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found: " + sensorId);
        }
        return sensor;
    }
}
