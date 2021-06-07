package io.slingr.endpoints.hl7;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.uhn.hl7v2.model.v281.segment.EVN;
import ca.uhn.hl7v2.model.v281.segment.MSH;
import ca.uhn.hl7v2.model.v281.segment.PID;
import ca.uhn.hl7v2.model.v281.segment.PV1;
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
import ca.uhn.hl7v2.model.v281.message.ADT_A01;

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
				if (i > 12) {//we try to connect for 2 minutes, after that we return.
					appLogger.error("After several attempts, it was not possible to connect to the VPN");
					return;
				}
				i++;
			}
			appLogger.info("VPN is connected...");
		}
		ReceivingApplication handler = new Receiver(events(), appLogger); // We trigger an event every time we receive a
																			// message
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
		String responseString = "";
		try {
			Parser parser = context.getPipeParser();

			Message msg = parser.parse(params.string("message"));
			Initiator init = initiators.get(params.string("channel")).getInitiator();
			if (init != null) {
				Message response = init.sendAndReceive(msg);
				responseString = parser.encode(response);
			} else {
				throw new Exception("Sender channel [" + params.string("channel") + "] was not found or is not opened");
			}
		} catch (HL7Exception | LLPException | IOException e) {
			throw new Exception("Error sending message through channel [" + params.string("channel") + "]: "+e.getMessage(), e);
		}
		return responseString;
	}

	@EndpointFunction(name = "_sendHl7AsJSON")
	public String sendHl7AsJSON(Json params) throws Exception {
		String responseString = "";

		switch (params.string("messageType").toUpperCase()) {
			case "ACK":
				break;
			case "ADT":
				responseString = buildADTmessage(params);
				break;
			case "BAR":
				break;
			case "DFT":
				break;
			case "MDM":
				break;
			case "MFN":
				break;
			case "ORM":
				break;
			case "ORU":
				break;
			case "QRY":
				break;
			case "RAS":
				break;
			case "RDE":
				break;
			case "RGV":
				break;
			case "SIU":
				break;
			default:
				responseString = "No messageType specified";
		}

		return responseString;
	}

	private String buildADTmessage(Json params) throws HL7Exception, IOException {
		String encodedMessage;
		switch (params.string("triggerEvent").toUpperCase()) {
			case "A01":
				ADT_A01 adt = new ADT_A01();
				adt.initQuickstart("ADT", "A01", "P");

				// Populate the MSH Segment
				MSH mshSegment = adt.getMSH();
				mshSegment.getSendingApplication().getNamespaceID().setValue("TestSendingSystem");
				mshSegment.getSendingFacility().getNamespaceID().setValue("TestSendingFacility");
				mshSegment.getReceivingApplication().getNamespaceID().setValue(params.string("receivingApplication").isEmpty() ? "TestReceivingSystem" : params.string("receivingApplication"));
				mshSegment.getReceivingFacility().getNamespaceID().setValue(params.string("receivingApplication").isEmpty() ? "TestReceivingFacility" : params.string("receivingFacility"));
				mshSegment.getSequenceNumber().setValue("123");

				//Populate the EVN Segment
				EVN evnSegment = adt.getEVN();
				evnSegment.getRecordedDateTime().setValue(new Date());

				// Populate the PID Segment
				PID pid = adt.getPID();
				//THIS SHOULD BE ITERATED, BECAUSE THE PATIENT MAY HAVE SEVERAL NAMES
				//for (Json patientName : params.string("patientNames") {
					pid.getPatientIdentifierList(0).getIDNumber().setValue(params.string("patientID"));
					pid.getPatientName(0).getFamilyName().getSurname().setValue(params.string("patientLastName"));
					pid.getPatientName(0).getGivenName().setValue(params.string("patientFirstName"));
					pid.getPatientName(0).getSecondAndFurtherGivenNamesOrInitialsThereof().setValue(params.string("patientOtherNames"));
					pid.getPatientName(0).getSuffixEgJRorIII().setValue(params.string("patientNameSuffix"));
					pid.getPatientName(0).getPrefixEgDR().setValue(params.string("PatientNamePrefix"));
					pid.getPatientName(0).getDegreeEgMD().setValue(params.string("PatientNameDegree"));
					pid.getMotherSMaidenName(0).getFamilyName().getSurname().setValue(params.string("patientMothersMaidenLastName"));
				//}
					//Making conversion from String to LocalDateTime and from LocalDateTime to Date
					pid.getDateTimeOfBirth().setValue(Date.from(LocalDateTime.parse(params.string("patientDateOfBirth")).atZone(ZoneId.systemDefault()).toInstant()));

				// Populate the PV1 Segment
				PV1 pv1 = adt.getPV1();
				pv1.getPatientClass().getIdentifier().setValue(params.string("patientClass"));
				pv1.getAssignedPatientLocation().getPointOfCare().getNamespaceID().setValue(params.string("pointOfCare"));
				pv1.getAssignedPatientLocation().getRoom().getNamespaceID().setValue(params.string("room"));
				pv1.getAssignedPatientLocation().getBed().getNamespaceID().setValue(params.string("bed"));
				pv1.getAssignedPatientLocation().getFacility().getNamespaceID().setValue(params.string("facility"));
				pv1.getAssignedPatientLocation().getBuilding().getNamespaceID().setValue(params.string("building"));
				pv1.getAssignedPatientLocation().getFloor().getNamespaceID().setValue(params.string("floor"));
				pv1.getAdmissionType().getIdentifier().setValue(params.string("admissionType"));
				pv1.getPreadmitNumber().getIDNumber().setValue(params.string("preadmitNumber"));
				pv1.getPreadmitNumber().getIdentifierTypeCode().setValue(params.string("preadmitNumberType"));

				// Now, let's encode the message and look at the output
				Parser parser = context.getPipeParser();
				encodedMessage = parser.encode(adt);
				break;
			default:
				encodedMessage = "triggerEvent is empty";
				break;
		}

		System.out.println("Printing ER7 Encoded Message:");
		System.out.println(encodedMessage);

		return encodedMessage;
	}
}

class Receiver implements ReceivingApplication<Message> {
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