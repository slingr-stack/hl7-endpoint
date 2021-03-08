package io.slingr.endpoints.hl7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

	private static final String ovpnHardCoded = "# Automatically generated OpenVPN client config file\n"
			+ "# Generated on Mon Mar  8 17:00:13 2021 by openvpn-access-server-1-vm\n"
			+ "\n"
			+ "# Default Cipher\n"
			+ "cipher AES-256-CBC\n"
			+ "# Note: this config file contains inline private keys\n"
			+ "#       and therefore should be kept confidential!\n"
			+ "# Note: this configuration is user-locked to the username below\n"
			+ "# OVPN_ACCESS_SERVER_USERNAME=lisandro\n"
			+ "# Define the profile name of this particular configuration file\n"
			+ "# OVPN_ACCESS_SERVER_PROFILE=lisandro@34.125.30.14\n"
			+ "# OVPN_ACCESS_SERVER_CLI_PREF_ALLOW_WEB_IMPORT=True\n"
			+ "# OVPN_ACCESS_SERVER_CLI_PREF_BASIC_CLIENT=False\n"
			+ "# OVPN_ACCESS_SERVER_CLI_PREF_ENABLE_CONNECT=False\n"
			+ "# OVPN_ACCESS_SERVER_CLI_PREF_ENABLE_XD_PROXY=True\n"
			+ "# OVPN_ACCESS_SERVER_WSHOST=34.125.30.14:443\n"
			+ "# OVPN_ACCESS_SERVER_WEB_CA_BUNDLE_START\n"
			+ "# -----BEGIN CERTIFICATE-----\n"
			+ "# MIIDHDCCAgSgAwIBAgIEX/izDzANBgkqhkiG9w0BAQsFADBHMUUwQwYDVQQDDDxP\n"
			+ "# cGVuVlBOIFdlYiBDQSAyMDIxLjAxLjA4IDE5OjMxOjI3IFVUQyBvcGVudnBuLWFj\n"
			+ "# Y2Vzcy1zZXJ2ZXIwHhcNMjEwMTAxMTkzMTI3WhcNMzEwMTA2MTkzMTI3WjBHMUUw\n"
			+ "# QwYDVQQDDDxPcGVuVlBOIFdlYiBDQSAyMDIxLjAxLjA4IDE5OjMxOjI3IFVUQyBv\n"
			+ "# cGVudnBuLWFjY2Vzcy1zZXJ2ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n"
			+ "# AoIBAQDAO75MwDa6DpdDk9MQPmnCbpp864lefhylIRLnm4N4FH208k3dOQSeOHEG\n"
			+ "# PqoAjTkFaEHpQaSFSqOXRi+buPtiswrmV2ORdGGawSy9P2xx62IZnF0QXZvutVwU\n"
			+ "# /WbqwXsZ2LO2HAF/kZhUsBsCaX7HsO+XSfSQHktIk4hMR8lxtsV/bqqyqD59EDWc\n"
			+ "# M8hx121wlU9ItvAT5wkPxGbzI6m+2lK1tA9jkZ/uxjHIha21w2s/PG8qfd242qHI\n"
			+ "# IIbDXbi5GfeLy41mvo05WbVPYwxouSBHh/XsaMyzqGUdj2i/WlRtPQe0qiJpvf4q\n"
			+ "# N020bZ0x7Prh+v5i/xtJNYQTuyk/AgMBAAGjEDAOMAwGA1UdEwQFMAMBAf8wDQYJ\n"
			+ "# KoZIhvcNAQELBQADggEBAHQY59Qeo2m3aI3jjzemKfdkXuaw/mf0ag8cyIjteJ7s\n"
			+ "# QWchLuDIIb22EbPxpZNOpKRR1WKVBZNiPVDwLn9CTx8ugDQQdsaZ4tHa8tAfPkew\n"
			+ "# 0EW/r8diW+Mxd0pNO4Js2z9k45sIY+O06YG8FXZF08vXma1j2vzz08sM+C0slKjF\n"
			+ "# ly5qwKyYp2vBvLk21XLNcdPQLfB+6j09aIGyATolHlrhb7X0tvyrIsSJsHSS7dq3\n"
			+ "# eqgp0v4Xyr6i9cWPzK1b9y3o5wq8qbQd1LpbvIb2QVUr4z8vNI3hrXCkOFf3TtGB\n"
			+ "# u77zmv+4jFwmhMyZh9CjQB4/cnQZxr6Im8O55oqsfyA=\n"
			+ "# -----END CERTIFICATE-----\n"
			+ "# OVPN_ACCESS_SERVER_WEB_CA_BUNDLE_STOP\n"
			+ "# OVPN_ACCESS_SERVER_IS_OPENVPN_WEB_CA=1\n"
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
			+ "# NOTE: LZO commands are pushed by the Access Server at connect time.\n"
			+ "# NOTE: The below line doesn't disable LZO.\n"
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
			+ "MIICwzCCAaugAwIBAgIBCTANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApPcGVu\n"
			+ "VlBOIENBMB4XDTIxMDMwMTE3MDAxMVoXDTMxMDMwNjE3MDAxMVowEzERMA8GA1UE\n"
			+ "AwwIbGlzYW5kcm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDR9rez\n"
			+ "ArQMn6hp1V/yQsAYwoFADlYtp3RydGFeSYYIcktcYLTInndRIHOTJiMWWq7XJrTY\n"
			+ "jk4RqShN4xpeejTuk8w0oz0X1kHCxb0LGh86FIT6qBR1ya+9W/LcV/TJdWAyRj95\n"
			+ "2a82wmbegAPU9aazSZ7b6mimkcYHGHGA1kAWtoEyd4jKcPYupUXdTTnXyiFOk0DL\n"
			+ "yqgdy2Iv1h5WFSV09cxuWKqAk5Dv1wUVvsIBfdeG4hEAaeI9FxE7wqUV2Nl0ERTZ\n"
			+ "qySfQQ9ILFnijP8E4Mt1xWgI6wMxpdzf29bOE5y2G6bR7hoIJR3iTjpY3au4QVmp\n"
			+ "srusZWvRhMeJfInXAgMBAAGjIDAeMAkGA1UdEwQCMAAwEQYJYIZIAYb4QgEBBAQD\n"
			+ "AgeAMA0GCSqGSIb3DQEBCwUAA4IBAQArrfhBG+CCNi7nk5t3D6u+jgk+1RSlvIVR\n"
			+ "x1JQwzdgQ7d6lH62jbQTfWpFgwZgHThAAVLBkWNsreIC7epFvo2QIVDEbXLQ6QFw\n"
			+ "oqhQmm1dJcq0dvFMJPfOnJ+bPiloqmJpY7TgoYHYjxZ5KH7rDDWqpwQJJjvvBQI3\n"
			+ "3vfsqaZrK4ZHziOaE4Jqp8NKurkQgYCDZ1iEZyVNG+aIxxmlXXX09P7OdXdSqrr2\n"
			+ "D9tvs+NeUAs5JNemlSUdWENKglEKzx/9U4MYkE2gqmxfEiPR3ZtZi76RSVT37mZh\n"
			+ "vxjtOtgxo2ywM1Ly9coQbV25llBQ/gvXxj8wueAeqFRD07tzmLOV\n"
			+ "-----END CERTIFICATE-----\n"
			+ "</cert>\n"
			+ "\n"
			+ "<key>\n"
			+ "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDR9rezArQMn6hp\n"
			+ "1V/yQsAYwoFADlYtp3RydGFeSYYIcktcYLTInndRIHOTJiMWWq7XJrTYjk4RqShN\n"
			+ "4xpeejTuk8w0oz0X1kHCxb0LGh86FIT6qBR1ya+9W/LcV/TJdWAyRj952a82wmbe\n"
			+ "gAPU9aazSZ7b6mimkcYHGHGA1kAWtoEyd4jKcPYupUXdTTnXyiFOk0DLyqgdy2Iv\n"
			+ "1h5WFSV09cxuWKqAk5Dv1wUVvsIBfdeG4hEAaeI9FxE7wqUV2Nl0ERTZqySfQQ9I\n"
			+ "LFnijP8E4Mt1xWgI6wMxpdzf29bOE5y2G6bR7hoIJR3iTjpY3au4QVmpsrusZWvR\n"
			+ "hMeJfInXAgMBAAECggEBAJPIQ6QL0w7PkYpsVQv/T5Yd0Tt029Nmed/VanMi7OQl\n"
			+ "DkYHCzfp0bGYVcDMpwWk7kshQ0jpYKmtlmC76GN3QKpr/N9PxkBw3fhplZWq3S/U\n"
			+ "IYULqNr/CmMNxPKyNelWZE7+gs8RGPJCAoW5NPk/rgWjW90OMHqNQATef4VfR9yz\n"
			+ "caigqvygqgahnZpymEbepZSXw25/syupvCRFKb/b0TiX7uPF8XGhL9m5cHipS1nJ\n"
			+ "Ed+BflQ5Zcqpm6hQ/98L4BM0kw680vLTaDGT2iliw5k024SPlrEQrQAHSsZXeNMM\n"
			+ "P+1uRO56vEM9kq9BXfs9mAvE1NkUB314b7cj/iJP/QECgYEA8pNBcciuZsyyY2Yh\n"
			+ "fs74QmRiJSAvxMirrKxuSJhduvGK4SKW3fJPDjI4qqhcREV2UxNcVD98a6PDQ8vF\n"
			+ "lFWzWx4MbwW52pwa4qkIyjiUIeiFsCwccIidMZnWs2CpfNCLE6mT1R66ExC2rZiy\n"
			+ "x+f2+M+jCJu4it8MUwnb0wG34l8CgYEA3ZVuRyZHikfd57nx0frt3H/UF/6zebDH\n"
			+ "ZvCHdRpM2ro+NofBrmlnC1Z5cjaLDICC4TImSxAAlnRNdn6/Tna8Rk+CnXZwypdn\n"
			+ "YOUh5c/RAshleDSNu49bxT8T61o+suftevcj/7bvPTt70GIA9ObY6sxmBCskXaLQ\n"
			+ "oeJHP/jnu4kCgYEAnuy9Lsf1AyLPKPJLtaHx4sWTy5EH2txCJXIfGu7AUe9YNuIL\n"
			+ "h8Pt6XYbzCtbr9r2h70ie4vQf1EeaDeD7ggr5uGL8d0Fg+VMc/2c5agl3QVTOTXd\n"
			+ "OBk5K7sHgCQvRrf2tl4enb1w2XbUyG1HjFM8aozW2pfzUwCJOYo2/Eqe5+8CgYBq\n"
			+ "cg2e+mGlqS6N0kK757p6kaHWwRGA5Q51ghjghXjkHlIiqga9/X518N24o0EbCWVW\n"
			+ "P1wr9Q0DOe24fdzzGZEKicmqUSqP+Dm+T3SATNwQZSpxm3V248BnOg0co6a5FTd9\n"
			+ "Odo+TqgRF1E8Ysgo3fjYmsZrPDplDQECs4wczDquWQKBgQC07u1+UsDMaCxXAJ2M\n"
			+ "DigpyfU3FViDIVZUYS2/FybIWkDBnw8beUzz1jzN2KhcbU58K7tUZ5LtpStWSofs\n"
			+ "Moz2Zij5/EWxnXlH7HczBoOyEp3BkQGShENsJCTXv7E6hdGDf2m1VZ5PDTa59CX0\n"
			+ "SLMP7hFn5T//vCIGCDch/j4pbw==\n"
			+ "-----END PRIVATE KEY-----\n"
			+ "</key>\n"
			+ "\n"
			+ "key-direction 1\n"
			+ "<tls-auth>\n"
			+ "#\n"
			+ "# 2048 bit OpenVPN static key (Server Agent)\n"
			+ "#\n"
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
			+ "\n"
			+ "## -----BEGIN RSA SIGNATURE-----\n"
			+ "## DIGEST:sha256\n"
			+ "## kfpG6mQFYlWKXTO5nB4ydhq9HyLX83tna+K4wHKcO2OkfZbCax\n"
			+ "## TYxO3Z996sLnQWhe/E/nAwGWyLnL7XTN6wMF3tb4mfLMPQHbEA\n"
			+ "## cySkTyL9MAixJLrz55gp8U3dHLjytOPkh0n5hM3JY1oyF+vFT2\n"
			+ "## JjsPHnDTgrta6CkkGPX+mLamPvTmhv3TAJcI0MzrWBz+s6F7wx\n"
			+ "## PyXYGvliLWXvHW+O5eLIRyTCZbb52n+8PPsw0jFS56xxFgtMsS\n"
			+ "## +LVLm+tpsshcxzu/maa+7SXQyO5d96VNiXyzVk+dFk/xbhnbs0\n"
			+ "## fDn08KvL//JGWRRsckb6dZYJj6uJ/J2qZIv5k1biIw==\n"
			+ "## -----END RSA SIGNATURE-----\n"
			+ "## -----BEGIN CERTIFICATE-----\n"
			+ "## MIIC/DCCAeSgAwIBAgIEX/izEDANBgkqhkiG9w0BAQsFADBHMUUwQwYDVQQDDDxP\n"
			+ "## cGVuVlBOIFdlYiBDQSAyMDIxLjAxLjA4IDE5OjMxOjI3IFVUQyBvcGVudnBuLWFj\n"
			+ "## Y2Vzcy1zZXJ2ZXIwHhcNMjEwMTAxMTkzMTI4WhcNMzEwMTA2MTkzMTI4WjAXMRUw\n"
			+ "## EwYDVQQDDAwzNC4xMjUuMzAuMTQwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n"
			+ "## AoIBAQDIhWPPXGJ/P7wB4h2h9G3ry9JVSuSRhKUNJawtTBxzFQ9ogOvIarM7odyz\n"
			+ "## SGCgA7AGrAu/Sb7Z195Rvak6yOKQlNPOftkXLsoN2H7nLZt6HdEFuIriYmqjMr1F\n"
			+ "## jpobEZVTLSkp67bvbl0aMAIZPMHehtXpMgomS+glGgalxBEJG5g5VItAGlxazeh2\n"
			+ "## b2k4Sjdqt7pRNiXzmdhjC5ALGOwvCaPjZYc1GDW1HWoz2lHg3rUAA+PZRnlu2esF\n"
			+ "## YM6U0s5bU1rEHHndzvCLilT9xsV66/DLlVz7Cq9Ikwd4tQEWi5Y9N+nhoYY52UhT\n"
			+ "## 8uvpWmS3HswFZD5MTWFpfWLqVIZ1AgMBAAGjIDAeMAkGA1UdEwQCMAAwEQYJYIZI\n"
			+ "## AYb4QgEBBAQDAgZAMA0GCSqGSIb3DQEBCwUAA4IBAQA3PJpWln+lCTilYTxaduXa\n"
			+ "## FFgsmQqZibJoSqWMGlH991YMZKx/TgoVbiS+seU5DeBBAEAHOHW1QVXR3t/y4W1o\n"
			+ "## 2t80zk74JTdOQA7ugEyvqVvU8oTiaDuTJwpChekBPf/s6uzcjldoVOLbYYzr7PTa\n"
			+ "## XUKtx3RgA4A9EI6w52lEwwTKaM0K8mTttCnuahEml+Rpslhxo910Bzxhyxwj+wD9\n"
			+ "## rapM9hDutMCcHiEuv3eU8Wj3MCH0hYkGPPHuM1Ec5ZQimulBw7mAhNhzl2g4gfNJ\n"
			+ "## 7bcaa7H4J+jev8UxFAYipQMUEIn1GGm0EZ09KQQ0FBRjGvZxIMHVwyPHr07oQLPD\n"
			+ "## -----END CERTIFICATE-----\n"
			+ "## -----BEGIN CERTIFICATE-----\n"
			+ "## MIIDHDCCAgSgAwIBAgIEX/izDzANBgkqhkiG9w0BAQsFADBHMUUwQwYDVQQDDDxP\n"
			+ "## cGVuVlBOIFdlYiBDQSAyMDIxLjAxLjA4IDE5OjMxOjI3IFVUQyBvcGVudnBuLWFj\n"
			+ "## Y2Vzcy1zZXJ2ZXIwHhcNMjEwMTAxMTkzMTI3WhcNMzEwMTA2MTkzMTI3WjBHMUUw\n"
			+ "## QwYDVQQDDDxPcGVuVlBOIFdlYiBDQSAyMDIxLjAxLjA4IDE5OjMxOjI3IFVUQyBv\n"
			+ "## cGVudnBuLWFjY2Vzcy1zZXJ2ZXIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK\n"
			+ "## AoIBAQDAO75MwDa6DpdDk9MQPmnCbpp864lefhylIRLnm4N4FH208k3dOQSeOHEG\n"
			+ "## PqoAjTkFaEHpQaSFSqOXRi+buPtiswrmV2ORdGGawSy9P2xx62IZnF0QXZvutVwU\n"
			+ "## /WbqwXsZ2LO2HAF/kZhUsBsCaX7HsO+XSfSQHktIk4hMR8lxtsV/bqqyqD59EDWc\n"
			+ "## M8hx121wlU9ItvAT5wkPxGbzI6m+2lK1tA9jkZ/uxjHIha21w2s/PG8qfd242qHI\n"
			+ "## IIbDXbi5GfeLy41mvo05WbVPYwxouSBHh/XsaMyzqGUdj2i/WlRtPQe0qiJpvf4q\n"
			+ "## N020bZ0x7Prh+v5i/xtJNYQTuyk/AgMBAAGjEDAOMAwGA1UdEwQFMAMBAf8wDQYJ\n"
			+ "## KoZIhvcNAQELBQADggEBAHQY59Qeo2m3aI3jjzemKfdkXuaw/mf0ag8cyIjteJ7s\n"
			+ "## QWchLuDIIb22EbPxpZNOpKRR1WKVBZNiPVDwLn9CTx8ugDQQdsaZ4tHa8tAfPkew\n"
			+ "## 0EW/r8diW+Mxd0pNO4Js2z9k45sIY+O06YG8FXZF08vXma1j2vzz08sM+C0slKjF\n"
			+ "## ly5qwKyYp2vBvLk21XLNcdPQLfB+6j09aIGyATolHlrhb7X0tvyrIsSJsHSS7dq3\n"
			+ "## eqgp0v4Xyr6i9cWPzK1b9y3o5wq8qbQd1LpbvIb2QVUr4z8vNI3hrXCkOFf3TtGB\n"
			+ "## u77zmv+4jFwmhMyZh9CjQB4/cnQZxr6Im8O55oqsfyA=\n"
			+ "## -----END CERTIFICATE-----\n"
			+ "";


	@Override
	public void endpointStarted() {
		appLogger.info("Initializing endpoint...");
		appLogger.info("OVPN received: " + ovpn);
		appLogger.info("Username: " + vpnUsername);
		appLogger.info("Pass: " + vpnPassword);

		VpnService vpnService = new VpnService();
		String ovpnFilePath = vpnService.createOvpnFile(ovpnHardCoded);// delete when implementing
//        String ovpnFilePath = vpnService.createOvpnFile(ovpn);
		String credentialsFilePath = vpnService.createLoginFile(vpnUsername, vpnPassword);
		if (ovpnFilePath != null && credentialsFilePath != null) {
			appLogger.info("Connecting to VPN...");			
			String connectionResult = vpnService.connectToVpn(ovpnFilePath, credentialsFilePath);
//			String connectionResult = connectToVpn(ovpnFilePath, credentialsFilePath);
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
//	public String connectToVpn(String ovpnFilePath, String credentialsFilePath) {
//		List<String> commandParams = new ArrayList<>();
////		commandParams.add("sudo"); only for mac
//		commandParams.add("openvpn");
//		commandParams.add("--config");
//		commandParams.add(ovpnFilePath);
//		commandParams.add("--verb");
//		commandParams.add("6");
//		commandParams.add("--auth-user-pass");
//		commandParams.add(credentialsFilePath);
//
//		StringBuilder result = new StringBuilder(80);
//		try {
//			ProcessBuilder pb = new ProcessBuilder(commandParams).redirectErrorStream(true);
//			Process process = pb.start();
//			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
//				while (true) {
//					String line = in.readLine();
//					if (line == null)
//						break;
//					result.append(line).append(NEW_LINE);
//				}
//			}
//		} catch (IOException e) {
//			appLogger.error("An error occurred while connecting to the VPN. [" + e.getMessage() + "]");
//			e.printStackTrace();
//		}
//		appLogger.info("VPN connection result " + result.toString());
//		return result.toString();
//	}	
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