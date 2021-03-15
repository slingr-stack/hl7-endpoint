package io.slingr.endpoints.hl7;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	List<MessageSender> messageSenders = new ArrayList<>();

	private VpnConnectionThread vpnThread;
	private ExecutorService executor = Executors.newSingleThreadExecutor();

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
		if (!connectToVpn) {
			endpointStopped("The VPN connection option is disabled. Please check the endpoint configuration.");
		}
		VpnService vpnService = new VpnService();
		String ovpnFilePath = vpnService.createOvpnFile(ovpn);
		String credentialsFilePath = vpnService.createLoginFile(vpnUsername, vpnPassword);
		if (ovpnFilePath != null && credentialsFilePath != null) {
			vpnThread = new VpnConnectionThread(ovpnFilePath, credentialsFilePath, appLogger);
			executor.execute(vpnThread);
		} else {
			appLogger.error("There was a fatal error creating the VPN configurations file");
			endpointStopped("There was a fatal error creating the VPN configurations file");
		}
		while (!vpnThread.isConnected()) {
			try {
				Thread.sleep(3000);
				appLogger.info("Waiting for the VPN to get connected...");
			} catch (InterruptedException e) {
				appLogger.error("There was a fatal error connecting to the VPN");
				e.printStackTrace();
			}
		}
		appLogger.info("VPN is connected...");
		ReceivingApplication handler = new Receiver(events()); // We trigger an event every time we receive a message
		for (Json channel : configuration.jsons("channels")) {
			String name = channel.string("name");
			String type = channel.string("type");
			String ip = channel.string("ip");
			int port = Integer.parseInt(channel.string("port"));

			if (type.equals("receiver")) {
				HL7Service server = context.newServer(port, false);
				server.registerApplication("*", "*", handler); // Support all message types
				server.start();
				appLogger.info("Receiver channel [" + name + "] started in port [" + port + "]!");
				servers.put(name, server);
			} else {
				if (vpnThread.isConnected()) {
					MessageSender sender = new MessageSender(name, ip, port, appLogger);
					ExecutorService SenderServerExecutor = Executors.newSingleThreadExecutor();
					SenderServerExecutor.execute(sender);
					messageSenders.add(sender);
					while (!sender.isConnected()) {
						try {
							Thread.sleep(2000);
							appLogger.info("Waiting for the " + name + " server to get connected...");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if (sender.isConnected()) {
						initiators.put(name, sender);
					}
				}
			}
		}
//		while (true) {
//			for (MessageSender messageSender : messageSenders) {
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				if (messageSender.gotDisconnected()) {
//					String name = messageSender.getServerName();
//					String ip = messageSender.getIp();
//					int port = messageSender.getPort();
//					messageSender.interruptThread();
//					MessageSender newSender = new MessageSender(name, ip, port, appLogger);
//					ExecutorService SenderServerExecutor = Executors.newSingleThreadExecutor();
//					SenderServerExecutor.execute(newSender);
//					messageSenders.add(newSender);
//				}
//			}
//		}
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
			appLogger.info("Parsing message...");
			Message msg = parser.parse(params.string("message"));
			appLogger.info("Channel: " + params.string("channel"));
			appLogger.info("Sending message... ");
			Initiator init = initiators.get(params.string("channel")).getInitiator();
			if (init != null) {
				Message response = init.sendAndReceive(msg);
				responseString = parser.encode(response);
				appLogger.info("Message sent!");
			} else {
				throw new Exception("Server not found, please check the endpoint configuration");
			}
		} catch (HL7Exception e) {
			appLogger.error("HL7 exception: " + e.getMessage());
			e.printStackTrace();
		} catch (LLPException e) {
			appLogger.error("LLP exception: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			appLogger.error("IO exception: " + e.getMessage());
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

	public Message processMessage(Message message, Map<String, Object> metadata)
			throws ReceivingApplicationException, HL7Exception {
		@SuppressWarnings("resource")
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