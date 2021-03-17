package io.slingr.endpoints.hl7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.framework.annotations.ApplicationLogger;
import io.slingr.endpoints.framework.annotations.EndpointConfiguration;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.EndpointProperty;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.hl7.services.MessageSender;
import io.slingr.endpoints.hl7.services.VpnConnectionThread;
import io.slingr.endpoints.hl7.services.VpnService;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.utils.Json;

@SlingrEndpoint(name = "hl7")
public class Hl7Endpoint extends Endpoint {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(Hl7Endpoint.class);

	// We use a HAPI context for pretty much everything
	HapiContext context = new DefaultHapiContext();
	// Servers listen for messages
	Map<String, HL7Service> servers = new HashMap<String, HL7Service>();
	// Initiators allow to send messages
	Map<String, MessageSender> initiators = new HashMap<String, MessageSender>();

	ArrayList<Channel> senderChannels = new ArrayList<>();

	ArrayList<Channel> receiverChannels = new ArrayList<>();

	private VpnConnectionThread vpnThread;
	private ExecutorService executor = Executors.newSingleThreadExecutor();
	
	private Events events;

	@ApplicationLogger
	protected AppLogs appLogger;

	@EndpointProperty
	private Boolean connectToVpn;

	@EndpointProperty
	private String vpnUsername;

	@EndpointProperty
	private String vpnPassword;

	@EndpointProperty
	private String ovpn;

	@EndpointConfiguration
	private Json configuration;

	public Hl7Endpoint() {
	}

	@Override
	public void endpointStarted() {
		appLogger.info("Initializing endpoint...");
		if (connectToVpn) {
			VpnService vpnService = new VpnService();
			String ovpnFilePath = vpnService.createOvpnFile(ovpn);
			String credentialsFilePath = vpnService.createLoginFile(vpnUsername, vpnPassword);
			if (ovpnFilePath != null && credentialsFilePath != null) {
				vpnThread = new VpnConnectionThread(ovpnFilePath, credentialsFilePath, appLogger);
				executor.execute(vpnThread);
			} else {
				appLogger.error("There was a fatal error creating the VPN configurations file");
				return;
			}
			int i = 0;
			while (!vpnThread.isConnected()) {
				try {
					Thread.sleep(10000);
					appLogger.info("Waiting for the VPN to get connected...");
				} catch (InterruptedException e) {
				}
				if (i > 12) {
					appLogger.error("After several attempts, it was not possible to connect to the VPN");
					return;
				}
				i++;
			}
			appLogger.info("VPN is connected...");
		}
		ReceivingApplication handler = new Receiver(events()); // We trigger an event every time we receive a message
		for (Json channel : configuration.jsons("channels")) {
			String name = channel.string("name");
			String type = channel.string("type");
			String ip = channel.string("ip");
			int port = Integer.parseInt(channel.string("port"));

			Channel ch = new Channel(name, type, ip, port);

			if (ch.getType().equals("receiver")) {
				receiverChannels.add(ch);
			} else {
				senderChannels.add(ch);
			}
		}

		for (Channel channel : receiverChannels) {
			HL7Service server = context.newServer(channel.getPort(), false);
			server.registerApplication("*", "*", handler); // Support all message types
			server.start();
			appLogger.info("Receiver channel [" + channel.getName() + "] started in port [" + channel.getPort() + "]!");
			servers.put(channel.getName(), server);
		}

		for (Channel channel : senderChannels) {
			MessageSender sender = new MessageSender(context, channel.getName(), channel.getIp(), channel.getPort(),
					appLogger);

			ExecutorService SenderServerExecutor = Executors.newSingleThreadExecutor();
			SenderServerExecutor.execute(sender);
			initiators.put(channel.getName(), sender);
		}
	}

	@Override
	public void endpointStopped(String cause) {
		VpnService vpnService = new VpnService();
		appLogger.info("Stopping servers...Reason: " + cause);
		for (HL7Service server : servers.values()) {
			server.stop();
		}
		appLogger.info("Closing VPN connection...");
		vpnService.killVpnConnection();
		executor.shutdownNow();// kills the thread if the endpoint stops
	}

	@EndpointFunction(name = "_sendHl7Message")
	public String sendHl7Message(Json params) throws Exception {
		Parser parser = context.getPipeParser();
		String responseString = "";
		try {
			Message msg = parser.parse(params.string("message"));
			Initiator init = initiators.get(params.string("channel")).getInitiator();
			if (init != null) {
				Message response = init.sendAndReceive(msg);
				responseString = parser.encode(response);
			} else {
				throw new Exception("Sender channel [" + params.string("channel") + "] was not found or is not opened");
			}
		} catch (HL7Exception | LLPException | IOException e) {
			events.send("messageError", Json.map().set("error", e));
			throw new Exception(e);
		}
		return responseString;
	}
}

class Receiver implements ReceivingApplication {
	private Events events;
	private AppLogs appLogger;

	public Receiver(Events events, AppLogs appLogger) {
		this.events = events;
		this.appLogger = appLogger;
	}

	public boolean canProcess(Message theIn) {
		return true;
	}

	public Message processMessage(Message message, Map<String, Object> metadata)
			throws ReceivingApplicationException, HL7Exception {
		@SuppressWarnings("resource")
		String encodedMessage = new DefaultHapiContext().getPipeParser().encode(message);
		Json data = Json.map().set("message", encodedMessage);
		events.send("messageArrived", Json.map().set("data", data)); // Notify event
		try {
			return message.generateACK(); // Generate an acknowledgment message and return it
		} catch (IOException e) {
			appLogger.error("There has been an error returning the acknowledgment", e);
			throw new HL7Exception(e);
		}
	}
}