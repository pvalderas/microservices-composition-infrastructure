package es.upv.pros.pvalderas.composition.eventbroker.utils;

import java.util.Properties;

public class BrokerConfig {
	
	private static String host;
	private static String port;
	private static String virtualHost;
	private static String user;
	private static String password;
	
	private static String brokerType;


	public static String getHost() {
		return host;
	}

	public static void setHost(String host) {
		BrokerConfig.host = host;
	}
	
	public static String getVirtualHost() {
		return virtualHost;
	}

	public static void setVirtualHost(String virtualHost) {
		BrokerConfig.virtualHost = virtualHost;
	}

	public static String getPort() {
		return port;
	}

	public static void setPort(String port) {
		BrokerConfig.port = port;
	}
	
	public static String getUser() {
		return user;
	}

	public static void setUser(String user) {
		BrokerConfig.user = user;
	}
	
	public static String getPassword() {
		return password;
	}

	public static void setPassword(String password) {
		BrokerConfig.password = password;
	}

	public static String getBrokerType() {
		return brokerType;
	}

	public static void setBrokerType(String brokerType) {
		BrokerConfig.brokerType = brokerType;
	}
	
	
	public static void configMessageBroker(Properties props){
        BrokerConfig.setBrokerType(props.getProperty("composition.messagebroker.type"));
        BrokerConfig.setHost(props.getProperty("composition.messagebroker.host"));
        BrokerConfig.setVirtualHost(props.getProperty("composition.messagebroker.virtualHost"));
        BrokerConfig.setPort(props.getProperty("composition.messagebroker.port"));
        BrokerConfig.setUser(props.getProperty("composition.messagebroker.user"));
        BrokerConfig.setPassword(props.getProperty("composition.messagebroker.password")); 
    }
	
}
