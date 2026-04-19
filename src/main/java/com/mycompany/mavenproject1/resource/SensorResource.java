package com.mycompany.mavenproject1.resource;

import com.mycompany.mavenproject1.exception.LinkedResourceNotFoundException;
import com.mycompany.mavenproject1.model.Room;
import com.mycompany.mavenproject1.model.Sensor;
import com.mycompany.mavenproject1.store.InMemoryStore;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final InMemoryStore store = InMemoryStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = store.getSensors().values().stream()
                .filter(sensor -> type == null || type.isBlank() || (sensor.getType() != null && sensor.getType().equalsIgnoreCase(type)))
                .collect(Collectors.toList());

        return Response.ok(sensors).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        validateSensor(sensor);

        Room room = store.getRoom(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Cannot create sensor. Referenced room does not exist: " + sensor.getRoomId());
        }

        if (store.getSensor(sensor.getId()) != null) {
            throw new ClientErrorException("Sensor already exists: " + sensor.getId(), Response.Status.CONFLICT);
        }

        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);

        synchronized (room) {
            if (room.getSensorIds() == null) {
                room.setSensorIds(new java.util.ArrayList<>());
            }
            room.getSensorIds().add(sensor.getId());
        }

        return Response.created(uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build())
                .entity(sensor)
                .build();
    }

    @Path("/{sensorId}/readings")
    public SensorReadingResource sensorReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private void validateSensor(Sensor sensor) {
        if (sensor == null) {
            throw new BadRequestException("Sensor payload is required");
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            throw new BadRequestException("Sensor id is required");
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            throw new BadRequestException("Sensor type is required");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            throw new BadRequestException("Sensor roomId is required");
        }
    }
}
