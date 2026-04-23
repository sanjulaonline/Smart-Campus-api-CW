// Student ID: 20240408
// Name: Sanjula Herath
package com.mycompany.mavenproject1;

import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class Mavenproject1 {

    private static final String DEFAULT_URI = "http://0.0.0.0:8080/";
    
    private static URI getBaseUri() {
        String envUri = System.getenv("BASE_URI");
        return URI.create(envUri != null && !envUri.isBlank() ? envUri : DEFAULT_URI);
    }
    public static HttpServer startServer() {
        ResourceConfig config = ResourceConfig.forApplication(new ApiApplication());
        return GrizzlyHttpServerFactory.createHttpServer(getBaseUri(), config);
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = startServer();
        System.out.println("Smart Campus API started at http://localhost:8080/api/v1");
        System.out.println("Press ENTER to stop the server...");
        System.in.read();
        server.shutdownNow();
    }
}
