package io.slingr.endpoints.hl7;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.uhn.hl7v2.*;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.v281.message.ADT_A02;
import ca.uhn.hl7v2.model.v281.message.ADT_A03;
import ca.uhn.hl7v2.model.v281.message.OML_O21;
import ca.uhn.hl7v2.model.v281.segment.*;

import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.exceptions.ErrorCode;
import io.slingr.endpoints.framework.annotations.*;
import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.app.Initiator;
import ca.uhn.hl7v2.llp.LLPException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import ca.uhn.hl7v2.model.v281.message.ADT_A01;

import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.hl7.services.MessageSender;
import io.slingr.endpoints.hl7.services.VpnConnectionThread;
import io.slingr.endpoints.hl7.services.VpnService;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.utils.Json;

import static io.slingr.endpoints.hl7.jsonHelper.JsonHelper.*;
import static io.slingr.endpoints.hl7.populators.SegmentPopulator.*;

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

	public static String appName;

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
		appName = properties().getApplicationName();
		//We validate the message constructed against our custom rules defined in the "hl7v281ValidationBuilder" class.
		//context.setValidationRuleBuilder(new Hl7v281ValidationBuilder());
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
			throw new Exception("Error sending message through channel [" + params.string("channel") + "]: " + e.getMessage(), e);
		}
		return responseString;
	}

	@EndpointFunction(name = "_sendHl7FromJSON")
	public String sendHl7FromJSON(Json params) throws Exception {
		String responseString = "";
		//We check that the message header exists, and that it has messageType and triggerEvent on in.
		if(!params.contains("messageHeader")){
			throw EndpointException.permanent(ErrorCode.ARGUMENT, "messageHeader property is required");
		}
		Json messageHeader = singleJsonPropertyParse("messageHeader",params.string("messageHeader"));
		if (!messageHeader.contains("messageType")) {
			throw EndpointException.permanent(ErrorCode.ARGUMENT, "messageHeader.messageType property is required");
		} else if (!messageHeader.contains("triggerEvent")) {
			throw EndpointException.permanent(ErrorCode.ARGUMENT, "messageHeader.triggerEvent property is required");
		}

		String messageType = messageHeader.string("messageType");
		String triggerEvent = messageHeader.string("triggerEvent");
		switch (messageType.toUpperCase()) {
			case "ACK":
				break;
			case "ADT":
				responseString = buildAdtMessage(triggerEvent,params);
				break;
			case "BAR":
				break;
			case "DFT":
				break;
			case "MDM":
				break;
			case "MFN":
				break;
			case "OML":
				responseString = buildOmlMessage(triggerEvent,params);
				break;
			case "ORM":
				//responseString = buildOrmMessage(triggerEvent,params);
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
				throw EndpointException.permanent(ErrorCode.ARGUMENT, "The value for 'messageType': [" + messageType + "] is not supported. The only supported values are: ACK,ADT,BAR,DFT,MDM,MFN,ORM,ORU,QRY,RAS,RDE,RGV & SIU");
		}

		return responseString;
	}

	private String buildAdtMessage(String triggerEvent,Json params) throws HL7Exception, IOException {
		String encodedMessage = "";
		Parser parser = context.getPipeParser();

		switch (triggerEvent.toUpperCase()) {
			case "A01":
				ADT_A01 adt_a01 = new ADT_A01();
				adt_a01.initQuickstart("ADT", "A01", "P");
				//We set the required "Recorded Date/Time" Field here as it is required, independently of it having more info
				adt_a01.getEVN().getRecordedDateTime().setValue(new Date());
				populateMessage(adt_a01,params);

				// Now, let's encode the message
				encodedMessage = encodeMessage(parser,adt_a01);
				break;

			case "A02":
				ADT_A02 adt_a02 = new ADT_A02();
				adt_a02.initQuickstart("ADT", "A02", "P");

				// Now, let's encode the message
				encodedMessage = encodeMessage(parser,adt_a02);
				break;

			case "A03":
				ADT_A03 adt_a03 = new ADT_A03();
				adt_a03.initQuickstart("ADT", "A03", "P");

				// Now, let's encode the message
				encodedMessage = encodeMessage(parser,adt_a03);
				break;

			default:
				throw EndpointException.permanent(ErrorCode.ARGUMENT, "The value for triggerEvent: [" + triggerEvent + "] is not supported. The only supported values for an ADT message are: A01 to A62");
		}

		System.out.println("Printing ER7 Encoded Message:");
		System.out.println(encodedMessage);

		return encodedMessage;
	}

	private String buildOmlMessage(String triggerEvent,Json params) throws HL7Exception, IOException {
		String encodedMessage = "";
		Parser parser = context.getPipeParser();

		switch (triggerEvent.toUpperCase()) {
			case "O21":
				OML_O21 oml_o21 = new OML_O21();
				oml_o21.initQuickstart("OML","O21","P");
				populateMessage(oml_o21,params);
				encodedMessage = encodeMessage(parser,oml_o21);
				break;
			case "O33":
				break;
			case "O35":
				break;
			case "O39":
				break;
			default:
				throw EndpointException.permanent(ErrorCode.ARGUMENT, "The value for triggerEvent: [" + triggerEvent + "] is not supported. The only supported values for an ORM messages are: O21,O33,O35 & O39");
		}
		return encodedMessage;
	}

	public void processHl7Message (String msg) throws HL7Exception {
		Parser parser = context.getPipeParser();
		Message decodedMessage = parser.parse(msg);
		MSH msh = (MSH) decodedMessage.get("MSH");
		String messageType = msh.getMessageType().getMessageCode().getValue();
		String triggerEvent = msh.getMessageType().getTriggerEvent().getValue();
		switch (messageType){
			case "ADT":
				return;
			case "OML":
				return;
		}
	}

	private void handleParseError(HL7Exception error) {
		//We should see if we can handle errors here.
		throw EndpointException.permanent(ErrorCode.ARGUMENT, error.getMessage());
	}
	private String encodeMessage(Parser parser, AbstractMessage msg) {
		String encodedMessage = "";
		try {
			encodedMessage = parser.encode(msg);
		} catch (HL7Exception err) {
			//We handle the error here to centralize the messages from all kind of messages
			handleParseError(err);
		}
		return encodedMessage;
	}

	//WEBHOOKS
	@EndpointWebService(path = "receiveHl7",methods = {RestMethod.POST})
	private WebServiceResponse receiveHl7MessageHttp(WebServiceRequest request) {
		Json responseBody = Json.map();
		responseBody.set("Example","Something");
		WebServiceResponse response = new WebServiceResponse(responseBody);
		response.setHttpCode(202);
		response.setHeader(Parameter.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
		response.setHeader("value", "someHeader");
		System.out.println("Request es:"+request.toString());
		System.out.println("Response es:"+response.toString());
		return response;
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
		String stringMessage = new DefaultHapiContext().getPipeParser().encode(message);
		Json data = Json.map().set("message", stringMessage);
		try {
			MessageProcessor msgProcessor = new MessageProcessor(events);
			msgProcessor.processOmlMessage(message,((MSH)message.get("MSH")).getMessageType().getTriggerEvent().getValue());
			return message.generateACK(); // Generate an acknowledgment message and return it
		} catch (IOException e) {
			appLogger.error("There has been an error returning the acknowledgment", e);
			throw new HL7Exception(e);
		}
	}
}
