# HL7 Endpoint

## Overview

The HL7 allows to send and receive HL7 messages through MLLP and HTTP.

Additionally, it has some tools to make it easy to work with HL7 messages.

For more information please refer to [docs](https://slingr-stack.github.io/platform/endpoints_hl7.html).

## Parameters

In order to connect to a VPN, some parameters need to be configured. Username and password are required to connect to a VPN along with the ovpn configuration file. The content of this file needs to be copied into the OVPN parameter.

Besides credentials, channels need to be configured to listen and send messages. When configuring a channel of type "Sender", the IP provided by the VPN administrator has to be configured.

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This endpoint is licensed under the Apache License 2.0. See the `LICENSE` file for more details.



