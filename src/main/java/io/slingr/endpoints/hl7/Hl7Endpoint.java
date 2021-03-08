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
	
	private static final String NEW_LINE = System.getProperty("line.separator");

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
			+ "# Generated on Fri Mar  5 21:07:14 2021 by openvpn-access-server-1-vm\n"
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
			+ "MIICwzCCAaugAwIBAgIBCDANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApPcGVu\n"
			+ "VlBOIENBMB4XDTIxMDIyNjIxMDcxMloXDTMxMDMwMzIxMDcxMlowEzERMA8GA1UE\n"
			+ "AwwIbGlzYW5kcm8wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDX1pgF\n"
			+ "Cp2BZM89SrmrsbK7WRGKXnzrHvProEgGUa8l3NqAMQYlr9uXLPN4k2WkvxfR0eWZ\n"
			+ "otN9KweczDS++TN5zRnhDCxe8UvubKn3eSMXXMkeOl2zwE9y6kpCTNgx3GcuiEGq\n"
			+ "Gs60iWiCnQPsmnh4AyjHqGoAs8wjzI++3NVNNPOVI8zcpwznWarkZJ5lUvuyZw/a\n"
			+ "x+FOpIrb092qY1keUWKiyLn4DYm8kTYD+2MnpWx42vdHB347YJOMX3JxHpY7JRh+\n"
			+ "9N+qThJxOjnRLtHTgaMhT2HpXec8r05BlqDNvWQ9L1VxMtmKVegfIzNRduQmMM5U\n"
			+ "JoN+pxtYVx+PUh9bAgMBAAGjIDAeMAkGA1UdEwQCMAAwEQYJYIZIAYb4QgEBBAQD\n"
			+ "AgeAMA0GCSqGSIb3DQEBCwUAA4IBAQBgSr97JKv3C3qc/jq76Nnvr25amNkyU/aM\n"
			+ "RPM8RTbe4RoLlW2JFM8Xw8sUBcskSdbNNQi1ZrnQBgtgL2Xze3I0Yzfb1KbQV/Ve\n"
			+ "692u/hZfIbUGevsU7zucuWVOrwfZXl6yHgH909vvXPWEltGm6ErIIUyJpe44LRop\n"
			+ "qPw4Exzoyie7CX+mBWYafgYxviN2dnIXi5tWe2YZ1VpZw3+lWpTLvTjH9N8iLlao\n"
			+ "7Zz5fIpsrB1VTdkgNn300pmVlYStxpJhAkA2YH59A/xcDsyiukEgZH/ZzDUJ8g5h\n"
			+ "kl+r/7wUQktoSIo0bFHN6Y+TCurgMkGCFSTMYv9Fy0W8Zb7FTWOO\n"
			+ "-----END CERTIFICATE-----\n"
			+ "</cert>\n"
			+ "\n"
			+ "<key>\n"
			+ "-----BEGIN PRIVATE KEY-----\n"
			+ "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDX1pgFCp2BZM89\n"
			+ "SrmrsbK7WRGKXnzrHvProEgGUa8l3NqAMQYlr9uXLPN4k2WkvxfR0eWZotN9Kwec\n"
			+ "zDS++TN5zRnhDCxe8UvubKn3eSMXXMkeOl2zwE9y6kpCTNgx3GcuiEGqGs60iWiC\n"
			+ "nQPsmnh4AyjHqGoAs8wjzI++3NVNNPOVI8zcpwznWarkZJ5lUvuyZw/ax+FOpIrb\n"
			+ "092qY1keUWKiyLn4DYm8kTYD+2MnpWx42vdHB347YJOMX3JxHpY7JRh+9N+qThJx\n"
			+ "OjnRLtHTgaMhT2HpXec8r05BlqDNvWQ9L1VxMtmKVegfIzNRduQmMM5UJoN+pxtY\n"
			+ "Vx+PUh9bAgMBAAECggEBAMurUdQgCjSZff1mUAI2MmQbTjP1qYbstFs56f0cg1wF\n"
			+ "JaIloJlbcqka37x9ykJFmCKEIFZzkYHhYtOQ1o0G9o4O/kagiBYnN/GKVHCTvItF\n"
			+ "IPsHNHh0FgRoFR6vCKs0QaFlLiFoHnm52Aa4R4Wy1c7ZnFebEjwLbayyUUpXdGfe\n"
			+ "vcA75Sc1eLxer6LputFMDvqFzAQsAgQ84ETAlowIUB4CmMjccOFkW2yjNrNRgvUR\n"
			+ "QJNKBb+2YRVI4Pm+IuvTIS7TbEawoXzDKaZz+6EajX9OLhz9+CH9Ri/CzAXL4V6f\n"
			+ "GdNF/HFzmR/+NpyrbDK2zJbBEkYj4EU4sprFntf10YECgYEA80ULHRCvkfozorEV\n"
			+ "M6K9TM+4miuI0o+0BNbnx91l+Fj3imjhMInuxVo896hdqnfCEeUbIAKdWtu6He6s\n"
			+ "bWvQxwcc+ZwoKI9Zd7qBqmqSot0cL6OmuYxh4TP3L5doH7eN5gyO3C0SjIVb1zcL\n"
			+ "zHA/QUAzQCLriSJlmCBKRuKYW7sCgYEA4yIQ1RLAQAdv8sitXr4m1y+f8884aJ16\n"
			+ "0QnLiuxp9WfG4t3+DIgJgLse1mLNMWlS7OBBMLMqGHaM8SWWwHYMo9IUFFiZodBV\n"
			+ "K7mSC48WDvsM7seAFcoBxuBWcW/4pLn0JWLXO7Z+joafezDbWXmoTVOqemIvOEiy\n"
			+ "TwEkT6XkgOECgYEA35qT54hhyMzwz1bzxP2OAF9iMBtZ7Sj3cAdVDqnbQ5YLGmbi\n"
			+ "rinqle4m4gEY70qa0LYE47xjhJM5FLBAP2d9hKNuJ6U4aGey9dExxEGnPRn+AW/s\n"
			+ "HNsDUOviBhWmOJkQTec3HPw084LU8xH+v1BhZAmt0gMG+orqVRVBELzRDGcCgYB9\n"
			+ "Db18wwUse6vMXWbu+fzCj4senqHtH3+GZkE17PJt1kRvllAQsDmjMeNJoD5fjSDG\n"
			+ "ILZjzNFJhMQW5V45+wT8kWbSyPFVYTHzkAlz6cT4XIsDUL4ZwQ33ZH7TmBWrBGMC\n"
			+ "zoe4bcMCT+aw2fZ7LftXD9CV70cWUxgxXWyTmb/UoQKBgQCR0awniT0dKMUUAmHO\n"
			+ "G43Pbih4FGZihhJrCMsTF1E616pjX+AaAcgdlTjhrk1J8XIqOid2KE/vv9fqy3Ur\n"
			+ "qHC4Lsaj8ECEI+CBo6K82+02sKOuiU4Zr2Bnf1IlSvXHae+jbIn/YnpdxTPUBsCS\n"
			+ "Huq3yuor1IaoNmJTCBvsjpmYFA==\n"
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
			+ "## xvJhQzeiFrDvJNPDOUGL4doJEp9SgaUjtbcL8Gi7vcVvgNAy3S\n"
			+ "## PsSeKyEgXPraIvK8TiMYU6UyMO+0nyEjf4xRHX89617sf/6Kkb\n"
			+ "## X++ainV03prM7gfgm9vu3vYXYShcB/fFqcw/Wv+yj6s/j2XtVG\n"
			+ "## nIdUpsPuEIhaIujvBEfIN5w6Ecc4BFhTEE223cFi94YF2eYBnY\n"
			+ "## pW3yodONqdNgRMLhzp0XdNv6DVow7fQQF4kOb/Tc1agBGnVCxL\n"
			+ "## AyOvoA/hmIHBdLfgpmcX4h9jZCLtG2Ts8Sjb32h+F71zsiz0EV\n"
			+ "## uNwZGdRuFAuMZVaFonXBG+vF9nS4qWO+IYx+4UphNg==\n"
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
//			String connectionResult = vpnService.connectToVpn(ovpnFilePath, credentialsFilePath);
			String connectionResult = connectToVpn(ovpnFilePath, credentialsFilePath);
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
	public String connectToVpn(String ovpnFilePath, String credentialsFilePath) {
		List<String> commandParams = new ArrayList<>();
//		commandParams.add("sudo"); only for mac
		commandParams.add("/usr/local/Cellar/openvpn/2.5.1/sbin/openvpn");
		commandParams.add("--config");
		commandParams.add(ovpnFilePath);
		commandParams.add("--verb");
		commandParams.add("6");
		commandParams.add("--auth-user-pass");
		commandParams.add(credentialsFilePath);

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
			appLogger.error("An error occurred while connecting to the VPN. [" + e.getMessage() + "]");
			e.printStackTrace();
		}
		appLogger.info("VPN connection result " + result.toString());
		return result.toString();
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