package io.slingr.endpoints.hl7.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VpnService {

	private static final Logger logger = LoggerFactory.getLogger(VpnService.class);

	private static final String NEW_LINE = System.getProperty("line.separator");

	public String createOvpnFile(String ovpnContent) {
		if (ovpnContent != null && !ovpnContent.isEmpty()) {
			File tempFile = null;
			try {
				tempFile = File.createTempFile("client-", ".ovpn");
				tempFile.deleteOnExit();
				FileWriter writer = new FileWriter(tempFile.getAbsolutePath());
				writer.write(ovpnContent);
				writer.close();
				logger.info(
						"OVPN client file successfully created in the following path: " + tempFile.getAbsolutePath());
			} catch (IOException e) {
				logger.error("An error occurred while creating client.ovpn file.");
				e.printStackTrace();
			}
			return tempFile.getAbsolutePath();
		} else {
			logger.error("The ovpn file content cannot be empty.");
			return null;
		}
	}

	public String createLoginFile(String vpnUsername, String vpnPassword) {
		if (vpnUsername != null && !vpnUsername.isEmpty() && vpnPassword != null && !vpnPassword.isEmpty()) {
			File tempFile = null;
			try {
				tempFile = File.createTempFile("credentials-", ".ovpn");
				tempFile.deleteOnExit();
				FileWriter writer = new FileWriter(tempFile.getAbsolutePath());
				writer.write(vpnUsername);
				writer.write(System.getProperty("line.separator"));
				writer.write(vpnPassword);
				writer.close();
				logger.info("OVPN credentials file successfully created in the following path: "
						+ tempFile.getAbsolutePath());
			} catch (IOException e) {
				logger.error("An error occurred while creating credentials.ovpn file.");
				e.printStackTrace();
			}
			return tempFile.getAbsolutePath();
		} else {
			logger.error("Username and Password must be completed.");
			return null;
		}
	}



	public void killVpnConnection() {
		List<String> commandParams = new ArrayList<>();
		commandParams.add("killall");
		commandParams.add("-SIGINT");
		commandParams.add("openvpn");
		StringBuilder result = new StringBuilder(80);
		try {
			ProcessBuilder pb = new ProcessBuilder(commandParams).redirectErrorStream(true);
			Process process = pb.start();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while (true) {
					String line = in.readLine();
					if (line == null)
						break;
					result.append(line).append(NEW_LINE);
				}
			}
		} catch (IOException e) {
			logger.error("An error occurred while connecting to the VPN.");
			e.printStackTrace();
		}
		logger.info("VPN connection result " + result.toString());
	}


}
