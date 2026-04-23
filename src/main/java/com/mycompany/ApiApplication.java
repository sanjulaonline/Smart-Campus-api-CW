package com.mycompany.mavenproject1;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class ApiApplication extends ResourceConfig {

    public ApiApplication() {
        packages("com.mycompany.mavenproject1.resource",
                "com.mycompany.mavenproject1.mapper",
                "com.mycompany.mavenproject1.filter");
    }
}

