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

//	private static final String ovpnHardCoded = "cipher AES-256-CBC\n" + "setenv FORWARD_COMPATIBLE 1\n" + "client\n"
//			+ "server-poll-timeout 4\n" + "nobind\n" + "remote 34.125.30.14 1194 udp\n"
//			+ "remote 34.125.30.14 1194 udp\n" + "remote 34.125.30.14 443 tcp\n" + "remote 34.125.30.14 1194 udp\n"
//			+ "remote 34.125.30.14 1194 udp\n" + "remote 34.125.30.14 1194 udp\n" + "remote 34.125.30.14 1194 udp\n"
//			+ "remote 34.125.30.14 1194 udp\n" + "dev tun\n" + "dev-type tun\n" + "ns-cert-type server\n"
//			+ "setenv opt tls-version-min 1.0 or-highest\n" + "reneg-sec 604800\n" + "sndbuf 0\n" + "rcvbuf 0\n"
//			+ "auth-user-pass\n" + "comp-lzo no\n" + "verb 3\n" + "setenv PUSH_PEER_INFO\n" + "\n" + "<ca>\n"
//			+ "-----BEGIN CERTIFICATE-----\n" + "MIICuDCCAaCgAwIBAgIEX/izCTANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApP\n"
//			+ "cGVuVlBOIENBMB4XDTIxMDEwMTE5MzEyMVoXDTMxMDEwNjE5MzEyMVowFTETMBEG\n"
//			+ "A1UEAwwKT3BlblZQTiBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"
//			+ "AKthXZgzQmsQ9cFDEOYpqvUomH/wdhGzHvDvdPycb2V8v4ExdjEhhbu6284p3MLf\n"
//			+ "mJneG+Q71wcGkpiVUxwA4Wy5koQ2uhDaCdUUDnQPLg822i5gt6q9OG0bf67ZSPGV\n"
//			+ "uDdmbF3ou0EIcIuS0magXXs2RAOQ1ObMHyTEN36RpHVn8kr1p+wMN7NP9j3tSkqm\n"
//			+ "PvchUTsvi/CHlLNUwff5ftHeaSBLrWcsCmQirJcDUbLa1tvJMGZymJ37lc72Pl1S\n"
//			+ "npWuj1kSu+u9EcvmX+SCJvxO8hletMH/7tdtmc5vcLTZ/w/Uf0srT8aG1PyvZ5ta\n"
//			+ "H9pFp6OtF9HlWD/UAkX18TMCAwEAAaMQMA4wDAYDVR0TBAUwAwEB/zANBgkqhkiG\n"
//			+ "9w0BAQsFAAOCAQEAMpUayrvehXPXYcMcOEpZSBF8xYEMhbUxNYErV7vMF2yWtBPD\n"
//			+ "RbONUViKN9ecOsqWch4Y4pi5m93giYK7cXwOeyC+Yk2DUdU0zABE6ZRXbhKj8uGt\n"
//			+ "EqBDiljWUHCWFd6/NVfU1cMGnmcB8nzgqZ5JDyntqpTjcSPxuRK2DsYTdveuCL4s\n"
//			+ "Rgf9xtlFLeRM+OHmt7x3R3Gv8uD5I1lf/b1bA+Xd0WSHuo3lG7jyN8w4ijhTTf3d\n"
//			+ "tY7pyAQgaRKAWfrq4OrSB8aha+RTCVVhQu5+nxYDWLUUoU2KGT3BmCSiOzwWzElo\n"
//			+ "RaoVaS468A0DMIueevmf0FdxtvSHGQjWtTzPrw==\n" + "-----END CERTIFICATE-----\n" + "</ca>\n" + "\n"
//			+ "<cert>\n" + "-----BEGIN CERTIFICATE-----\n"
//			+ "MIICwzCCAaugAwIBAgIBBjANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApPcGVu\n"
//			+ "VlBOIENBMB4XDTIxMDIyNDE0MzQ0M1oXDTMxMDMwMTE0MzQ0M1owEzERMA8GA1UE\n"
//			+ "AwwIbGlzYW5kcm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCk3YsY\n"
//			+ "7EMyZMgfzBA2Kep91obxOy+Lly5NFsXR+EWosZGeinl9bEgdMmoQaSNqDyXTGdMn\n"
//			+ "g58GwKY9eFsi7/N3KqHMFjy5QBdlSmUzP7vv36S8XWR7eULhoCwuVQbyVkR42OcU\n"
//			+ "3hMZq5PiC9FfqLBYpm+cCj7zv0oVUa6kXHo/4cxjWPtbxoTDbw64qHgoDi/bcZuk\n"
//			+ "Rsrs3dyKYdaeq0CKXkcD4Vv7ymhEQS1nKukm6mZ5hQcRkYvqAgJr19DLd1u7tWWK\n"
//			+ "mn7gFffiA8X1YeQmgYO/Yj+uvDp3ke7bB1eOMvBxRN0OiMN2Bd3or+FerdXuf2H6\n"
//			+ "YMQGLhguJl2XMNU5AgMBAAGjIDAeMAkGA1UdEwQCMAAwEQYJYIZIAYb4QgEBBAQD\n"
//			+ "AgeAMA0GCSqGSIb3DQEBCwUAA4IBAQBls+NqqvfqLgtmSYRfBpXZKt5ux3FUA/nu\n"
//			+ "vd2cERqh8g0WFkTbcBqUEjXfBr1lKG/mrJtmWn50GeD1Aw0U9dHN1uUDbcjkSAyZ\n"
//			+ "IxloS7r/MtrcMiQyJu2bw2AVNVFUt3vjJC81oGr99y+PL2Wv7dXivnNZTqRgSmk9\n"
//			+ "Gf68vrw2e9qz7jNsH6z81YIsxVMIx+2JSqJbL2KFWL2pcQeMmaWFOkvIqbXUGZh5\n"
//			+ "aS3giw5eRLsMKRAnRiN8/2mHAw6KfIrocTO9hpfsDe6CI87djA6Z+YHV/Ikp/1bs\n"
//			+ "gs+WGtTRQu9Zt4FYSxzmKV1rjwPSBA8/9fEQ1Wig0LiJjuAuZvhX\n" + "-----END CERTIFICATE-----\n" + "</cert>\n"
//			+ "\n" + "<key>\n" + "-----BEGIN PRIVATE KEY-----\n"
//			+ "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCk3YsY7EMyZMgf\n"
//			+ "zBA2Kep91obxOy+Lly5NFsXR+EWosZGeinl9bEgdMmoQaSNqDyXTGdMng58GwKY9\n"
//			+ "eFsi7/N3KqHMFjy5QBdlSmUzP7vv36S8XWR7eULhoCwuVQbyVkR42OcU3hMZq5Pi\n"
//			+ "C9FfqLBYpm+cCj7zv0oVUa6kXHo/4cxjWPtbxoTDbw64qHgoDi/bcZukRsrs3dyK\n"
//			+ "Ydaeq0CKXkcD4Vv7ymhEQS1nKukm6mZ5hQcRkYvqAgJr19DLd1u7tWWKmn7gFffi\n"
//			+ "A8X1YeQmgYO/Yj+uvDp3ke7bB1eOMvBxRN0OiMN2Bd3or+FerdXuf2H6YMQGLhgu\n"
//			+ "Jl2XMNU5AgMBAAECggEAFmYjJ9exqX8X0XogGbvIKCClZNDtvMFlgQO/DK/2htzZ\n"
//			+ "M/kMWIYFpjHbRy+BqD82nF+92HiBeqFh9KgNecmwAljAcsHyrhiPFSvuY/Ac0JJK\n"
//			+ "B22I4V2wQIdKeWED+JjP5LtTafpiDfLJvMRuRdjdXK4AK+sYcnVi01Ktr5cA5PzI\n"
//			+ "uyvmff1Itck54ypsOWrG3rDBF0DjGEyKfgFN23QrvCSbZC17oNd/+0us9uecB8Mw\n"
//			+ "GaA86FFqZz2yCuAp1UC7IaCj4bh0u3jp8BYOkutZ9PldjzKc9RjPfDQ7JSYzRGpj\n"
//			+ "5dc5JApt6/chXo9y7ZX9RUGXUW5RPnSenhBwHU7KIQKBgQDQ6Sglu9SGQraIzSJl\n"
//			+ "qMX+77Twc0jJnYdSc3B/pXinJPXPhGu1JoskwBKHriXKOPwMVbydVgtOaLYsIBBO\n"
//			+ "DBntY8e/gixXyF/9W1ZOPeImKrxrOOAWlcD1ueb2rymDBhSw/yWafn9qQ5kODyCW\n"
//			+ "WhjssmLoOxYsh2AafvQZ1MwbqwKBgQDKBtK2hOz4qhEWAmaUHKMJUS6Hii3Hiyae\n"
//			+ "BvaE1qNTscLfq128gIONf7T3Elyre6OaZ2VSQXAUxkvM2WbFhj+/sudHqU/WBDpO\n"
//			+ "6R8hb3NpVLAXltWIyicRiOOSAfdZZtWOVQ0eW33oFCYLt/R0RlZ/G3uRWezmthF3\n"
//			+ "vfGvPQUOqwKBgQCg0KqQCXv68kXzHv34hagMrFd7tTL1yKbPyH90M1uiRuhWG1iS\n"
//			+ "NR5tZtGc0mjSt3UGxcUQ1JiVfjOl2fTTL6uRuvqbqEgcNun1bScNycNZoBI0865E\n"
//			+ "lHjab4WkpYfuDXNGINLAj9njYi/iy7BJLQf8xQEwRbO3CWroWovrioyoSwKBgQC0\n"
//			+ "f14Zdi+mh4OWb3SoleFQPLjeniUCqB9LhxsPWhk4eJSMc7Xe6c8fR98t87jZ+TAV\n"
//			+ "jpVm5VI3LWeh0QbSKXyhUDz0uJ+8rbBOuld27vVlQSXHHZsQiNRZBT2VQK52sLrS\n"
//			+ "XMjIu6OFCyNmyubcT8+N5scC+R4AjuCSZCEfmNwA2wKBgF/7MRquDlvgDPW+94yr\n"
//			+ "MUkCffuO2Uj8FZl2+bexULeOinlLQUiyY70xensbc9aakbJuuHZRu2keHY1zHZQG\n"
//			+ "OdoQWUbobvofkclw28IQfQd8+0rKMWHpfFR33WmQpc1XjBowdByKNgSdDqMg7s6j\n" + "LRR3fFOJQIN+jLE68rDJvE61\n"
//			+ "-----END PRIVATE KEY-----\n" + "</key>\n" + "\n" + "key-direction 1\n" + "<tls-auth>\n" + "\n"
//			+ "-----BEGIN OpenVPN Static key V1-----\n" + "f7b344aaded82442f7f71bd7ad05d7f4\n"
//			+ "6f8932a959f4e0f65c5e76c152acf69d\n" + "2a70a5539c656963e64127ad1931b8f3\n"
//			+ "c1f31190aebbedb375664acf2598d948\n" + "add2361f31b934c5401aafb10ea7ffa4\n"
//			+ "d803f20983fb3ee29c906a8faf5f9674\n" + "5624e187d97ad2043048e7ab05d11b30\n"
//			+ "ccbeebe0ff18a32884b240567c94f114\n" + "ac04ddcbb52e378b155eeaecd8236362\n"
//			+ "2ee3acd1b57c48c484976df40e738761\n" + "fb4255a84b0dae6f091ca92e7447bd65\n"
//			+ "fa84a129906d70be993b11a81ce0a9e6\n" + "207d3d926d879bb788d98c68401cadcf\n"
//			+ "d489e86d58ccd7b0b75a0dc24f7e5205\n" + "109623e399aa4a8849d9174677116f44\n"
//			+ "861bfd200512b66039db125de4e200f3\n" + "-----END OpenVPN Static key V1-----\n" + "</tls-auth>\n" + "";

	@Override
	public void endpointStarted() {
		appLogger.info("Initializing endpoint...");
		appLogger.info("OVPN received: " + ovpn);
		appLogger.info("Username: " + vpnUsername);
		appLogger.info("Pass: " + vpnPassword);

		VpnService vpnService = new VpnService();
//		String ovpnFilePath = vpnService.createOvpnFile(ovpnHardCoded);// delete when implementing
        String ovpnFilePath = vpnService.createOvpnFile(ovpn);
		String credentialsFilePath = vpnService.createLoginFile(vpnUsername, vpnPassword);
		if (ovpnFilePath != null && credentialsFilePath != null) {
			String connectionResult = vpnService.connectToVpn(ovpnFilePath, credentialsFilePath);
			appLogger.info("VPN Connection result: " + connectionResult);
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
					appLogger.info("Sender channel [" + name + "] started!");
					initiators.put(name, initiator);
				} catch (HL7Exception e) {
					appLogger.info("Could not start channel [" + name + "]");
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