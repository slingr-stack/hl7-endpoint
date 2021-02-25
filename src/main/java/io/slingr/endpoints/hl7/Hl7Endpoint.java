package io.slingr.endpoints.hl7;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.ConnectionListener;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

@SlingrEndpoint(name = "hl7")
public class Hl7Endpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(Hl7Endpoint.class);
    // We use a HAPI context for pretty much everything
    HapiContext context = new DefaultHapiContext();
    // Servers listen for messages
    Map<String, HL7Service> servers = new HashMap<String, HL7Service>();
    // Initiators allow to send messages
    Map<String, Initiator> initiators = new HashMap<String, Initiator>();

    @ApplicationLogger
    protected AppLogs appLogger;

    @EndpointConfiguration
    private Json configuration;

    public Hl7Endpoint() {
    }

    @Override
    public void endpointStarted() {
        appLogger.info("Initializing endpoint...");
        ReceivingApplication handler = new Receiver(events()); // We trigger an event every time we receive a message
        for (Json channel : configuration.jsons("channels")) {
            String name = channel.string("name");
            String type = channel.string("type");
            String ip = channel.string("ip");
            int port = parseInt(channel.string("port"));

            if (type.equals("receiver")) {
                HL7Service server = context.newServer(port, false);
                server.registerApplication("*", "*", handler); // Support all message types
                server.start();
                appLogger.info("Receiver channel ["+name+"] started!");
                servers.put(name, server);
            } else {
                try {
                    Connection connection = context.newClient(ip, port, false);
                    Initiator initiator = connection.getInitiator();
                    appLogger.info("Sender channel ["+name+"] started!");
                    initiators.put(name, initiator);
                } catch (HL7Exception e) {
                    appLogger.info("Could not start channel ["+name+"]");
                }
            }
        }
    }

    @Override
    public void endpointStopped(String cause) {
        appLogger.info("Stopping servers...");
        for (HL7Service server : servers.values()) {
            server.stop();
        }
    }

    @EndpointFunction(name = "_sendMessage")
    public String sendMessage(Json params) {
        Parser parser = context.getPipeParser();
        String responseString = "";
        try {
            appLogger.info("Parsing message...");
            Message msg = parser.parse(params.string("message"));
            appLogger.info("Sending message... ");
            Message response = initiators.get(params.string("channel")).sendAndReceive(msg);
            responseString = parser.encode(response);
            appLogger.info("Message sent!");
        } catch (HL7Exception e) {
            appLogger.info("HL7 exception");
            e.printStackTrace();
        } catch (LLPException e) {
            appLogger.info("LLP exception");
            e.printStackTrace();
        } catch (IOException e) {
            appLogger.info("IO exception");
            e.printStackTrace();
        }
        return responseString;
    }
}

class Receiver implements ReceivingApplication {
    private Events events;

    public Receiver(Events events) {
        this.events = events;
    }

    public boolean canProcess(Message theIn) {
        return true;
    }
    public Message processMessage(Message message, Map<String, Object> metadata) throws ReceivingApplicationException, HL7Exception {
        String encodedMessage = new DefaultHapiContext().getPipeParser().encode(message);
        Json data = Json.map().set("message", encodedMessage);
        events.send("messageArrived", Json.map().set("data", data)); // Notify event
        try {
            return message.generateACK(); // Generate an acknowledgment message and return it
        } catch (IOException e) {
            throw new HL7Exception(e);
        }
    }
}