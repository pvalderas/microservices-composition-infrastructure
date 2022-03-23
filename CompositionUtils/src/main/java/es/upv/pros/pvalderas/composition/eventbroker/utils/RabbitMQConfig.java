package es.upv.pros.pvalderas.composition.eventbroker.utils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConfig {
	
		public static final String ID="rabbitmq";

		public static final String RUNTIME_EXCHANGE="composition";
		public static final String RESPONSE_EXCHANGE="evolution_response";
		public static final String ADAPTATION_EXCHANGE="fragment_adaptation";
		public static final String EVOLUTION_EXCHANGE="evolution";
		public static final String CONFIRMATION_EXCHANGE="eevolution_confirmation";
		
		public static Connection getconnection() throws IOException, TimeoutException{
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(BrokerConfig.getHost());
			factory.setPort(Integer.parseInt(BrokerConfig.getPort()));
			if(BrokerConfig.getVirtualHost()!=null) factory.setVirtualHost(BrokerConfig.getVirtualHost());
			if(BrokerConfig.getUser()!=null) factory.setUsername(BrokerConfig.getUser());
			if(BrokerConfig.getPassword()!=null) factory.setPassword(BrokerConfig.getPassword());
			Connection connection = factory.newConnection();
			return connection;
		}

}
