package com.parasoft.parabank.web;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.WebApplicationException;
import com.parasoft.parabank.service.ParaBankServiceException;
/** Stable plain-text errors for rejected mutations, without exposing server internals. */
@Provider
public final class BankErrorMapper implements ExceptionMapper<Throwable> {
    @Override public Response toResponse(Throwable error) {
        for(Throwable cause=error;cause!=null;cause=cause.getCause())
            if(cause instanceof IllegalArgumentException || cause instanceof ParaBankServiceException)
                return Response.status(400).type("text/plain").entity("Invalid banking request").build();
        int status=error instanceof WebApplicationException ? ((WebApplicationException)error).getResponse().getStatus() : 500;
        return Response.status(status).type("text/plain").entity(status<500?"Invalid request":"Banking service error").build();
    }
}
