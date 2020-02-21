package es.upv.pros.pvalderas.compositioncoordinator.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

@Component
public class EventManager {
	
	private static String host;
	private static String port;
	
	private static String brokerType;
	
	@Autowired
	private RuntimeService runtimeService;

	public void registerEventListener(String microservice) throws IOException, TimeoutException{
		
		switch(brokerType){
			case "rabbitmq": rabbitmqRegisterEvent(microservice); break;
		}
	
	}
	
	private void rabbitmqRegisterEvent(String microservice) throws IOException, TimeoutException{
		
		String exchange=getRABBITMQ_EXCHANGE();
		
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);
		factory.setPort(Integer.parseInt(port));
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.exchangeDeclare(exchange, BuiltinExchangeType.TOPIC);
		 
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, exchange, microservice.toLowerCase());

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				 
				
					 String message=new String(body);
					
					 runtimeService.createMessageCorrelation(message).correlate();
					
					 System.out.println("Received Message: "+ message);
				
			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}

	public static String getRABBITMQ_EXCHANGE() {
		return RABBITMQ_EXCHANGE!=null?RABBITMQ_EXCHANGE:DEFAULT_RABBITMQ_EXCHANGE;
	}

	public static void setRABBITMQ_EXCHANGE(String name) {
		RABBITMQ_EXCHANGE = name;
	}

	public static String getHost() {
		return host;
	}

	public static void setHost(String host) {
		EventManager.host = host;
	}

	public static String getPort() {
		return port;
	}

	public static void setPort(String port) {
		EventManager.port = port;
	}

	public static String getBrokerType() {
		return brokerType;
	}

	public static void setBrokerType(String brokerType) {
		EventManager.brokerType = brokerType;
	}
	
	
	public static final String RABBITMQ="rabbitmq";
	private static String RABBITMQ_EXCHANGE;
	public static final String DEFAULT_RABBITMQ_EXCHANGE="processes";
	
}
