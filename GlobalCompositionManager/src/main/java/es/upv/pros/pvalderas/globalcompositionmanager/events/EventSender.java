package es.upv.pros.pvalderas.globalcompositionmanager.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.ChangeConfirmation;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.eventbroker.utils.RabbitMQConfig;

@Component
public class EventSender {
	
	public void adaptationResponse(AdaptationResponse response) throws JSONException{
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
		
			String topic=response.getTo().toLowerCase()+"."+response.getComposition().toLowerCase();
			
			try {			
				Connection connection = RabbitMQConfig.getconnection();
			
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(RabbitMQConfig.RESPONSE_EXCHANGE, BuiltinExchangeType.TOPIC);
	
				channel.basicPublish(RabbitMQConfig.RESPONSE_EXCHANGE, topic, null, response.toJSON().getBytes());

				channel.close();
				connection.close();
				
				System.out.println("Sent Evolution Response Message to "+ response.getTo()+": "+response.getResponse());
				
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void confirmChange(ChangeConfirmation confirmation) throws JSONException{
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
			String topic=confirmation.getComposition().toLowerCase();
			try {			
				
				/*ChangeConfirmation confirmation=new ChangeConfirmation();
				confirmation.setConfirmed(response);
				confirmation.setComposition(composition);*/
				
				Connection connection = RabbitMQConfig.getconnection();
			
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(RabbitMQConfig.CONFIRMATION_EXCHANGE, BuiltinExchangeType.TOPIC);

				channel.basicPublish(RabbitMQConfig.CONFIRMATION_EXCHANGE, topic, null, confirmation.toJSON().getBytes());

				channel.close();
				connection.close();
				
				System.out.println("Sent Change Confirmation : "+confirmation.toJSON());
				
			} catch (IOException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}
	
}
