package es.upv.pros.pvalderas.fragmentmanager.events;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedFragment;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.eventbroker.utils.RabbitMQConfig;

@Component
public class EventSender {
	
	public void evolutionChange(AdaptedFragment adaptedFragment) throws JSONException{
		if(BrokerConfig.getBrokerType().equals(RabbitMQConfig.ID)){
			try {			
				String topic=adaptedFragment.getMicroservice().toLowerCase()+"."+adaptedFragment.getComposition().toLowerCase();
				
				Connection connection = RabbitMQConfig.getconnection();
			
				Channel channel = connection.createChannel();
				channel.exchangeDeclare(RabbitMQConfig.ADAPTATION_EXCHANGE, BuiltinExchangeType.TOPIC);
				
				channel.basicPublish(RabbitMQConfig.ADAPTATION_EXCHANGE, topic, null, adaptedFragment.toJSON().getBytes());

				channel.close();
				connection.close();
				
				System.out.println("Sent Evolution Message: "+ adaptedFragment.toJSON());
				
			} catch (IOException | TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
}
