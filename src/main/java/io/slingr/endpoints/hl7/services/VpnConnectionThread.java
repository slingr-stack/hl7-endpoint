package io.slingr.endpoints.hl7.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VpnConnectionThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(VpnConnectionThread.class);
	private static final String NEW_LINE = System.getProperty("line.separator");

	public void run(String ovpnFilePath, String credentialsFilePath) {
		logger.info("THE VPN THREAD IS RUNNING");

		List<String> vpnConnectioncommandParams = new ArrayList<>();

    		scriptDocker();

    		vpnConnectioncommandParams.add("openvpn");
//		vpnConnectioncommandParams.add("/usr/local/Cellar/openvpn/2.5.1/sbin/openvpn");
		vpnConnectioncommandParams.add("--config");
		vpnConnectioncommandParams.add(ovpnFilePath);
		vpnConnectioncommandParams.add("--verb");
		vpnConnectioncommandParams.add("6");
		vpnConnectioncommandParams.add("--auth-user-pass");
		vpnConnectioncommandParams.add(credentialsFilePath);

		StringBuilder result = new StringBuilder(80);
		try {
			ProcessBuilder pbVpnConnection = new ProcessBuilder(vpnConnectioncommandParams).redirectErrorStream(true);
			Process process = pbVpnConnection.start();
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while (true) {
					String line = in.readLine();
					if (line == null)
						break;
					logger.info("VPN STATUS: " + line);
					result.append(line).append(NEW_LINE);
				}
			}
		} catch (IOException e) {
			logger.error("An error occurred while connecting to the VPN.");
			e.printStackTrace();
		}
		logger.info("VPN connection result " + result.toString());
	}
	public void scriptDocker() {
		List<String> vpnConnectionScript1commandParams = new ArrayList<>();
		List<String> vpnConnectionScript2commandParams = new ArrayList<>();
		List<String> vpnConnectionScript3commandParams = new ArrayList<>();

		vpnConnectionScript1commandParams.add("mkdir");
		vpnConnectionScript1commandParams.add("-p");
		vpnConnectionScript1commandParams.add("/dev/net");

		vpnConnectionScript2commandParams.add("mknod");
		vpnConnectionScript2commandParams.add("/dev/net/tun");
		vpnConnectionScript2commandParams.add("c");
		vpnConnectionScript2commandParams.add("10");
		vpnConnectionScript2commandParams.add("200");

		vpnConnectionScript3commandParams.add("chmod");
		vpnConnectionScript3commandParams.add("600");
		vpnConnectionScript3commandParams.add("/dev/net/tun");

		try {
			ProcessBuilder pbVpnScript1Connection = new ProcessBuilder(vpnConnectionScript1commandParams)
					.redirectErrorStream(true);
			pbVpnScript1Connection.start();

			ProcessBuilder pbVpnScript2Connection = new ProcessBuilder(vpnConnectionScript2commandParams)
					.redirectErrorStream(true);
			pbVpnScript2Connection.start();

			ProcessBuilder pbVpnScript3Connection = new ProcessBuilder(vpnConnectionScript3commandParams)
					.redirectErrorStream(true);
			pbVpnScript3Connection.start();

		} catch (IOException e) {
			logger.error("An error occurred while executing linux commands.");
			e.printStackTrace();
		}
	}	
}
