package io.slingr.endpoints.hl7.services;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.Initiator;
import io.slingr.endpoints.services.AppLogs;

public class MessageSender implements Runnable {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

	// We use a HAPI context for pretty much everything
	HapiContext context;

	private String serverName;
	private String ip;
	private int port;
	private AppLogs appLogger;

	public MessageSender(String serverName, String ip, int port, AppLogs appLogger) {
		this.serverName = serverName;
		this.ip = ip;
		this.port = port;
		this.appLogger = appLogger;
	}

	private Initiator initiator;
	private AtomicBoolean serverConnected = new AtomicBoolean(false);

	private Connection connection;

	public boolean isConnected() {
		return serverConnected.get();
	}

	public Initiator getInitiator() {
		return initiator;
	}

	@Override
	public void run() {
		while (true) {
			while (!isConnected()) {
				try {
					Thread.sleep(2000);
					appLogger.info("Attempting to connect to sender channel [" + serverName + "], IP: [" + ip + "].");
					context = new DefaultHapiContext();
					connection = context.newClient(ip, port, false);
					initiator = connection.getInitiator();
					appLogger.info(
							"Sender channel [" + serverName + "], IP: [" + ip + "] started in port [" + port + "]!");
					serverConnected.set(true);
				} catch (HL7Exception | InterruptedException e) {
					appLogger.error("Could not start channel [" + serverName + "], IP: [" + ip + "]. Reason: "
							+ e.getMessage());
				}

			}
			try {
				Thread.sleep(10000);
				serverConnected.set(connection.isOpen());
				if (!connection.isOpen()) {
					connection.close();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
