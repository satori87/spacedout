package com.bbg.server.upnp;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;



public class Punch {

	
	@SuppressWarnings("unused")
	public static boolean configPNP(int tcpPort, int udpPort) {
		try {
			GatewayDiscover gatewayDiscover = new GatewayDiscover();
			Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();
			if (gateways.isEmpty()) {return false;}
			GatewayDevice activeGW = gatewayDiscover.getValidGateway();
			if (null == activeGW) { //sometimes validgateway doesnt, if not lets try the Map
				Iterator<InetAddress> it = gateways.keySet().iterator();
				if(!it.hasNext()) {return false;}
				activeGW = gateways.get(it.next());
				if(activeGW == null) {return false;}
			}
			PortMappingEntry portMapping = new PortMappingEntry();
			InetAddress localAddress = activeGW.getLocalAddress();
			String externalIPAddress = activeGW.getExternalIPAddress();
			if (!activeGW.getSpecificPortMappingEntry(tcpPort,"TCP",portMapping)) {
				if (!activeGW.addPortMapping(tcpPort,tcpPort,localAddress.getHostAddress(),"TCP","bbg")) {
					return false;
				}
			} 
			if (!activeGW.getSpecificPortMappingEntry(udpPort,"UDP",portMapping)) {
				if (!activeGW.addPortMapping(udpPort,udpPort,localAddress.getHostAddress(),"UDP","bbg")) {
					return false;
				}
			} 
		} catch (Exception ex) {
			System.out.println(ex.toString());
		} finally {}
		return true;
	}

}
