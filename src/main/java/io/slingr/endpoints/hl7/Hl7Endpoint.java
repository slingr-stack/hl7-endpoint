package io.slingr.endpoints.hl7;

import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.framework.annotations.ApplicationLogger;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.EndpointProperty;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.utils.Json;
import org.apache.log4j.Logger;

@SlingrEndpoint(name = "hl7")
public class Hl7Endpoint extends Endpoint {
    private static final Logger logger = Logger.getLogger(Hl7Endpoint.class);

    @ApplicationLogger
    private AppLogs appLogger;

    @EndpointProperty
    private String username;

    @EndpointProperty
    private String password;

    @EndpointProperty
    private String integrationCode;

    @EndpointProperty
    private String pollingEnabled;

    @EndpointProperty
    private String pollingFrequency;

    @EndpointProperty
    private String entitiesToPoll;

    public Hl7Endpoint() {
    }

    @Override
    public void endpointStarted() {
    }

    @EndpointFunction(name = "_sendMessage")
    public Json sendMessage(Json params) {
        Json result = Json.map();
        return result;
    }    
}
