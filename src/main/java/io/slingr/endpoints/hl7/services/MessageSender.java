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

	private HapiContext context;
	private String serverName;
	private String ip;
	private int port;
	private AppLogs appLogger;

	public MessageSender(HapiContext context, String serverName, String ip, int port, AppLogs appLogger) {
		super();
		this.context = context;
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
					appLogger.info("Attempting to connect to sender channel [" + serverName + "], IP: [" + ip + "].");
					context = new DefaultHapiContext();
					connection = context.newClient(ip, port, false);
					initiator = connection.getInitiator();
					appLogger.info(
							"Sender channel [" + serverName + "], IP: [" + ip + "] started in port [" + port + "]!");
					serverConnected.set(true);
				} catch (HL7Exception e) {
					appLogger.error("Could not start channel [" + serverName + "], IP: [" + ip + "].", e);
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e2) {
					}
				}
			}
			try {
				Thread.sleep(10000);
				serverConnected.set(connection.isOpen());
				if (!connection.isOpen()) {
					appLogger.warn("The connection with sender channel [" + serverName + "], IP: [" + ip + "] was lost. We will try to reconnect...");
					connection.close();
				}
			} catch (InterruptedException e) {
			}
		}
	}
}
