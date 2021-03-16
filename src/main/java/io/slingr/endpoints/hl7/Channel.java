package io.slingr.endpoints.hl7;

public class Channel {

	private String name;
	private String type;
	private String ip;
	private int port;
	
	public Channel(String name, String type, String ip, int port) {
		super();
		this.name = name;
		this.type = type;
		this.ip = ip;
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
