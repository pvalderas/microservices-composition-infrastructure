package es.upv.pros.pvalderas.compositioncoordinator.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@Component
public class EventSender implements JavaDelegate {
	
	FixedValue message;
	FixedValue microservice;

	@Override
	public void execute(DelegateExecution execution) {
		if(EventManager.getBrokerType().equals(EventManager.RABBITMQ)){
			try {
				ConnectionFactory factory = new ConnectionFactory();
				factory.setHost(EventManager.getHost());
				factory.setPort(Integer.parseInt(EventManager.getPort()));
				Connection connection;
			
				connection = factory.newConnection();
				
				Channel channel = connection.createChannel();
				
				channel.exchangeDeclare(EventManager.getRABBITMQ_EXCHANGE(), BuiltinExchangeType.TOPIC);
				
				channel.basicPublish(EventManager.getRABBITMQ_EXCHANGE(), microservice.getExpressionText().toLowerCase(), null, message.getExpressionText().getBytes());
				
				channel.close();
				connection.close();
				
				System.out.println("Sent Message: "+ message.getExpressionText());
				
			} catch (IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
