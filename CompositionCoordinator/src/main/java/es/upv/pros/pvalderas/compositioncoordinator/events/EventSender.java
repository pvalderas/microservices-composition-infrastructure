package es.upv.pros.pvalderas.compositioncoordinator.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.ChangeConfirmation;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.eventbroker.utils.RabbitMQConfig;

@Component
public class EventSender implements JavaDelegate {
	
	FixedValue message;
	FixedValue microservice;

	@Override
	public void execute(DelegateExecution execution) {
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
			try {
				
				
				Connection connection = RabbitMQConfig.getconnection();
			
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(RabbitMQConfig.RUNTIME_EXCHANGE, BuiltinExchangeType.TOPIC);
				
				 
				
				
				String composition=message.getExpressionText().substring(0,message.getExpressionText().indexOf("_"));
				//String topic=microservice.getExpressionText().toLowerCase()+"."+composition.toLowerCase()+"."+client;
				String client=Clients.currentClient.get(composition.toLowerCase()); 
				
				String topic=composition.toLowerCase()+"."+client;
				
				try {
					JSONObject messageJSON = new JSONObject();
					messageJSON.put("message",message.getExpressionText());
					messageJSON.put("client",client);
					channel.basicPublish(RabbitMQConfig.RUNTIME_EXCHANGE, topic, null, messageJSON.toString().getBytes());
				} catch (JSONException e) {
					e.printStackTrace();
				}

				channel.close();
				connection.close();
				
				System.out.println("Sent Message: "+ message.getExpressionText());
				
			} catch (IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
	
	public void evolutionChange(String message){
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
			try {			
				Connection connection = RabbitMQConfig.getconnection();
			
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(RabbitMQConfig.EVOLUTION_EXCHANGE, BuiltinExchangeType.DIRECT);
	
				channel.basicPublish(RabbitMQConfig.EVOLUTION_EXCHANGE, "", null, message.getBytes());

				channel.close();
				connection.close();
				
				System.out.println("Sent Evolution Message: "+ message);
				
			} catch (IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void responseToChange(AdaptationResponse response) throws JSONException{
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
			try {			
				Connection connection = RabbitMQConfig.getconnection();
				String topic=response.getTo().toLowerCase()+"."+response.getComposition().toLowerCase();
			
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
	
}
