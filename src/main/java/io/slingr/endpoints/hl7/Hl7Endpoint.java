package io.slingr.endpoints.hl7;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
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
import io.slingr.endpoints.hl7.services.VpnConnectionThread;
import io.slingr.endpoints.hl7.services.VpnService;
import io.slingr.endpoints.services.AppLogs;
import io.slingr.endpoints.services.Events;
import io.slingr.endpoints.utils.Json;

@SlingrEndpoint(name = "hl7")
public class Hl7Endpoint extends Endpoint {

	// We use a HAPI context for pretty much everything
	HapiContext context = new DefaultHapiContext();
	// Servers listen for messages
	Map<String, HL7Service> servers = new HashMap<String, HL7Service>();
	// Initiators allow to send messages
	Map<String, Initiator> initiators = new HashMap<String, Initiator>();
	
//	private static final String NEW_LINE = System.getProperty("line.separator");

	@ApplicationLogger
	protected AppLogs appLogger;

	@EndpointProperty
	private String connectToVpn;

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

	private static final String ovpnHardCoded = "\n"
			+ "cipher AES-256-CBC\n"
			+ "\n"
			+ "setenv FORWARD_COMPATIBLE 1\n"
			+ "client\n"
			+ "server-poll-timeout 4\n"
			+ "nobind\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 443 tcp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "remote 34.125.30.14 1194 udp\n"
			+ "dev tun\n"
			+ "dev-type tun\n"
			+ "ns-cert-type server\n"
			+ "setenv opt tls-version-min 1.0 or-highest\n"
			+ "reneg-sec 604800\n"
			+ "sndbuf 0\n"
			+ "rcvbuf 0\n"
			+ "auth-user-pass\n"
			+ "\n"
			+ "comp-lzo no\n"
			+ "verb 3\n"
			+ "setenv PUSH_PEER_INFO\n"
			+ "\n"
			+ "<ca>\n"
			+ "-----BEGIN CERTIFICATE-----\n"
			+ "MIICuDCCAaCgAwIBAgIEX/izCTANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApP\n"
			+ "cGVuVlBOIENBMB4XDTIxMDEwMTE5MzEyMVoXDTMxMDEwNjE5MzEyMVowFTETMBEG\n"
			+ "A1UEAwwKT3BlblZQTiBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"
			+ "AKthXZgzQmsQ9cFDEOYpqvUomH/wdhGzHvDvdPycb2V8v4ExdjEhhbu6284p3MLf\n"
			+ "mJneG+Q71wcGkpiVUxwA4Wy5koQ2uhDaCdUUDnQPLg822i5gt6q9OG0bf67ZSPGV\n"
			+ "uDdmbF3ou0EIcIuS0magXXs2RAOQ1ObMHyTEN36RpHVn8kr1p+wMN7NP9j3tSkqm\n"
			+ "PvchUTsvi/CHlLNUwff5ftHeaSBLrWcsCmQirJcDUbLa1tvJMGZymJ37lc72Pl1S\n"
			+ "npWuj1kSu+u9EcvmX+SCJvxO8hletMH/7tdtmc5vcLTZ/w/Uf0srT8aG1PyvZ5ta\n"
			+ "H9pFp6OtF9HlWD/UAkX18TMCAwEAAaMQMA4wDAYDVR0TBAUwAwEB/zANBgkqhkiG\n"
			+ "9w0BAQsFAAOCAQEAMpUayrvehXPXYcMcOEpZSBF8xYEMhbUxNYErV7vMF2yWtBPD\n"
			+ "RbONUViKN9ecOsqWch4Y4pi5m93giYK7cXwOeyC+Yk2DUdU0zABE6ZRXbhKj8uGt\n"
			+ "EqBDiljWUHCWFd6/NVfU1cMGnmcB8nzgqZ5JDyntqpTjcSPxuRK2DsYTdveuCL4s\n"
			+ "Rgf9xtlFLeRM+OHmt7x3R3Gv8uD5I1lf/b1bA+Xd0WSHuo3lG7jyN8w4ijhTTf3d\n"
			+ "tY7pyAQgaRKAWfrq4OrSB8aha+RTCVVhQu5+nxYDWLUUoU2KGT3BmCSiOzwWzElo\n"
			+ "RaoVaS468A0DMIueevmf0FdxtvSHGQjWtTzPrw==\n"
			+ "-----END CERTIFICATE-----\n"
			+ "</ca>\n"
			+ "\n"
			+ "<cert>\n"
			+ "-----BEGIN CERTIFICATE-----\n"
			+ "MIICwzCCAaugAwIBAgIBCjANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApPcGVu\n"
			+ "VlBOIENBMB4XDTIxMDMwMTE4NTgwMVoXDTMxMDMwNjE4NTgwMVowEzERMA8GA1UE\n"
			+ "AwwIbGlzYW5kcm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDfdDO5\n"
			+ "i043RjiPWSYoQEiC4iCW6TEo6jRMvmGny88IessRP1lbl5WnYJ+glZxo31ceUTfr\n"
			+ "S+t7uvDvAl1itEFlkdOd0FaXijXFzAMr5bfproWy4gnFcz6EF1iCWpZpB06nqlIq\n"
			+ "V5t/xwQaVpq7E8J4atFvxXRqsxgZU5LtVXIFLC9XMz6RNnjjolimHDyoSnZNu5DJ\n"
			+ "0GbtmpWUcf9Bm8f5NbfJZ1v7scFDg86j8KP+3GchytR9GlR7B1TZlJsie0rRWR+H\n"
			+ "8hY2ePGLHp4OZgSamF4KpUjZivSyfGs59ajpwZL3q24EIUzKWs/WOfGAWQGQM3/0\n"
			+ "WG8X5rHKXi2A2ckBAgMBAAGjIDAeMAkGA1UdEwQCMAAwEQYJYIZIAYb4QgEBBAQD\n"
			+ "AgeAMA0GCSqGSIb3DQEBCwUAA4IBAQAMzs/7Wqlceuuf7dGK8PIHrtagyoCobo2S\n"
			+ "BP6cNH1Lx6kSNLcbmfcQZlYQ3V8b37E38ctQXtb+MCRd8Ri4eLGibMtRkvLsgLtf\n"
			+ "kA9CP8pv0HfKZAlbHixigSV3rUmdadxvRi/QOoVtvldu+Dd85shqpmrx3ozoEVVg\n"
			+ "LufjETml0sDGajIIWt7DEULLh5JG0g7BJrHg86g4vUTW90Vd0ek2oD3bgtcv8rjT\n"
			+ "YN2RT+oTxIGvvIZsOxfYNcoj8I3iWeDHWXQ6O1qn9W3x+vh0ntQCFl7DPYKdVJPI\n"
			+ "8lwXJWubf49vsluI/tDKUKAx9F5puFmFA54p5gbNZplynrQVrCkN\n"
			+ "-----END CERTIFICATE-----\n"
			+ "</cert>\n"
			+ "\n"
			+ "<key>\n"
			+ "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDfdDO5i043RjiP\n"
			+ "WSYoQEiC4iCW6TEo6jRMvmGny88IessRP1lbl5WnYJ+glZxo31ceUTfrS+t7uvDv\n"
			+ "Al1itEFlkdOd0FaXijXFzAMr5bfproWy4gnFcz6EF1iCWpZpB06nqlIqV5t/xwQa\n"
			+ "Vpq7E8J4atFvxXRqsxgZU5LtVXIFLC9XMz6RNnjjolimHDyoSnZNu5DJ0GbtmpWU\n"
			+ "cf9Bm8f5NbfJZ1v7scFDg86j8KP+3GchytR9GlR7B1TZlJsie0rRWR+H8hY2ePGL\n"
			+ "Hp4OZgSamF4KpUjZivSyfGs59ajpwZL3q24EIUzKWs/WOfGAWQGQM3/0WG8X5rHK\n"
			+ "Xi2A2ckBAgMBAAECggEAUfBqhz9uDxbQ5r/qHhh0footKSmZvUckpn+pj75MzO86\n"
			+ "R7jTK6MKXbYw0tIJ6Or9J0DhIIdWcqi6cOqNFATFdlljIBulpSwpS77j6tTx+97j\n"
			+ "aBU7QAxigE2PRz2wqhLj5NewCZbWjqSL/JyFYNR+G2oQIsxlRDsoOyFKX31Vsk8x\n"
			+ "29V2wZkB1PmtNSUS2rtl3i5VVbIyMLwZAUVDlb0adsjPn/sm7FQeV2cXy1sEEtDD\n"
			+ "tp3p1I/Uv/MGB5iFVlp1g5S95RTB4Y5LCQflXpWsGwcg3KO9H7M0zdkdTCz1Vukb\n"
			+ "Kyz1XW4M3wZcT2E03gEUcgRiddaxlwhurggi4/FIAQKBgQD9H06gp74QNZBc0k+m\n"
			+ "6l4LNv7XWciSQ/x8Q9NDEiL+WzlCHNCsOIlskOq2PJfhFUxhHAAsR9f+VDN4xs0p\n"
			+ "aZQpHkhhGBk+6lHLMgr5YhPkWs8/1m3ONiMH1rGGfkh8D9aLv5i1ACctqUGj2Kdk\n"
			+ "TgFGHVr/tQpFud/2RSO5L9YOuQKBgQDh/owhliDEOjgjjWIlQQKRrTDBk2baFd1m\n"
			+ "CmreqtzVVgaeJSUZsiqadIAMngcggbakyySk8X3kIUpr+0YdBqAaOJSl9Z0c8KAH\n"
			+ "FFvBlCvVazaVeK8xp1KSg7KShmPev6/dEyvxJl6SNQFcKenFczd+OlYRLqzYxg7a\n"
			+ "TRTHBXcoiQKBgDHVwx5ASFxan2SXB5WeWQuuNThi9elON1yj4ek3neokPb61e6Qc\n"
			+ "sXnNgliEz4ZCMjqAT8xoDK+HFmc2BNidb800qB1tqDLJ4Xa3EJAO7kmsU1eHOTE7\n"
			+ "WWRGscb///dlCuGSxFcGg7AqvcCrfDF0Zi3Ow8bKdw5JTT3oWOMx9twJAoGBANUh\n"
			+ "yiiWvFozieRFn20A7ZpTmqrFB/ffLQjiBD3xKAlucxlMcLvWIoW+H1FG6/PFQWoT\n"
			+ "R4DjW1X964EA3fPb3rw75jzJ8Z1sdY+XrJ1YzyocTcMTqS4L3jzdRVkYszFM2F+H\n"
			+ "iCaKiCAdKyYvRQ/5BDngbHER8uMRjspkbZbw2Zr5AoGBAM/tYEx2mvB9VVWYGVz8\n"
			+ "Q0U02YKPH0qtVVjkj5hKU5wVXwyyqZUdHzNsWOwImpKwifLd9arQqrhchJHQ/j/I\n"
			+ "xYBwj5ivLpZJ1DLlY1AL51Ws78koQyCos6lW/DeV6FOGyWhJgxlUVTpKWpnSbHSp\n"
			+ "0rnCzcBEWXm2NTZD2kaUaP/j\n"
			+ "-----END PRIVATE KEY-----\n"
			+ "</key>\n"
			+ "\n"
			+ "key-direction 1\n"
			+ "<tls-auth>\n"
			+ "\n"
			+ "-----BEGIN OpenVPN Static key V1-----\n"
			+ "f7b344aaded82442f7f71bd7ad05d7f4\n"
			+ "6f8932a959f4e0f65c5e76c152acf69d\n"
			+ "2a70a5539c656963e64127ad1931b8f3\n"
			+ "c1f31190aebbedb375664acf2598d948\n"
			+ "add2361f31b934c5401aafb10ea7ffa4\n"
			+ "d803f20983fb3ee29c906a8faf5f9674\n"
			+ "5624e187d97ad2043048e7ab05d11b30\n"
			+ "ccbeebe0ff18a32884b240567c94f114\n"
			+ "ac04ddcbb52e378b155eeaecd8236362\n"
			+ "2ee3acd1b57c48c484976df40e738761\n"
			+ "fb4255a84b0dae6f091ca92e7447bd65\n"
			+ "fa84a129906d70be993b11a81ce0a9e6\n"
			+ "207d3d926d879bb788d98c68401cadcf\n"
			+ "d489e86d58ccd7b0b75a0dc24f7e5205\n"
			+ "109623e399aa4a8849d9174677116f44\n"
			+ "861bfd200512b66039db125de4e200f3\n"
			+ "-----END OpenVPN Static key V1-----\n"
			+ "</tls-auth>\n"
			+ "";


	@Override
	public void endpointStarted() {
		appLogger.info("Initializing endpoint...");

		VpnService vpnService = new VpnService();
		String ovpnFilePath = vpnService.createOvpnFile(ovpnHardCoded);// delete when implementing
//        String ovpnFilePath = vpnService.createOvpnFile(ovpn);
		String credentialsFilePath = vpnService.createLoginFile(vpnUsername, vpnPassword);
		if (ovpnFilePath != null && credentialsFilePath != null) {
			appLogger.info("Connecting to VPN...");
			VpnConnectionThread vpnThread = new VpnConnectionThread();
			vpnThread.start();
//			String connectionResult = vpnService.connectToVpn(ovpnFilePath, credentialsFilePath);
//			appLogger.info("VPN Connection result: " + connectionResult);
		} else {
			appLogger.error("There was a fatal error creating the VPN configurations file");
			endpointStopped("There was a fatal error creating the VPN configurations file");
		}
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
				appLogger.info("Receiver channel [" + name + "] started!");
				servers.put(name, server);
			} else {
				try {
					Connection connection = context.newClient(ip, port, false);
					Initiator initiator = connection.getInitiator();
					appLogger.info("Sender channel [" + name + "], IP: [" + ip + "] started!");					
					initiators.put(name, initiator);
				} catch (HL7Exception e) {
					appLogger.info("Could not start channel [" + name + "], IP: [" + ip + "]");
				}
			}
		}
	}

	@Override
	public void endpointStopped(String cause) {
		VpnService vpnService = new VpnService();
		appLogger.info("Stopping servers...");
		for (HL7Service server : servers.values()) {
			server.stop();
		}
		appLogger.info("Closing VPN connection...");
		vpnService.killVpnConnection();
	}

	@EndpointFunction(name = "_sendHl7Message")
	public String sendHl7Message(Json params) {
		Parser parser = context.getPipeParser();
		String responseString = "";
		try {
			appLogger.info("Parsing message...");
			Message msg = parser.parse(params.string("message"));
			appLogger.info("Channel: " + params.string("channel"));
			appLogger.info("Message: " + msg);
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