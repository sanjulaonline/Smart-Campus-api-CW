package com.mycompany.mavenproject1.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover(@Context UriInfo uriInfo) {
        URI base = uriInfo.getBaseUri();

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", base.resolve("rooms").toString());
        resources.put("sensors", base.resolve("sensors").toString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "Smart Campus Sensor & Room Management API");
        payload.put("version", "v1");
        payload.put("contact", "facilities-api-admin@university.example");
        payload.put("resources", resources);

        return Response.ok(payload).build();
    }
}
