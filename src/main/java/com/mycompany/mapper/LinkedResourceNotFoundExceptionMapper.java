package com.mycompany.mavenproject1.mapper;

import com.mycompany.mavenproject1.exception.LinkedResourceNotFoundException;
import com.mycompany.mavenproject1.model.ApiError;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        int statusCode = 422;
        ApiError error = new ApiError(
                statusCode,
                "Unprocessable Entity",
                exception.getMessage(),
                System.currentTimeMillis(),
                requestPath());

        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }

    private String requestPath() {
        return uriInfo == null ? "" : "/" + uriInfo.getPath();
    }
}

