package es.upv.pros.pvalderas.globalcompositionmanager.events;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.ChangeConfirmation;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.eventbroker.utils.RabbitMQConfig;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.FileManager;
import es.upv.pros.pvalderas.globalcompositionmanager.dao.DAO;

@Component
public class EventManager {
	
	@Autowired
	private DAO dao;
	
	@Autowired
	private FileManager fileManager;

	public void registerEventListener(String composition) throws IOException, TimeoutException{	
		switch(BrokerConfig.getBrokerType()){
			case RabbitMQConfig.ID: rabbitmqRegisterEvent(composition); break;
		}
	}
	
	private void rabbitmqRegisterEvent(String composition) throws IOException, TimeoutException{	
		Connection connection = RabbitMQConfig.getconnection();	
		//rabbitmqConfirmationChange(connection, composition);
		rabbitmqChangeResponse(connection, composition);
	}
	
	
	/*private void rabbitmqConfirmationChange(Connection connection, String composition) throws IOException{
		String topic=composition.toLowerCase();
		
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(RabbitMQConfig.CONFIRMATION_EXCHANGE, BuiltinExchangeType.TOPIC);
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, RabbitMQConfig.CONFIRMATION_EXCHANGE, topic);

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				
					try {
						ChangeConfirmation confirmation=ChangeConfirmation.parseJSON(new String(body));
						
						if(confirmation.isConfirmed()){
							String xmlToConfirm=dao.getDirtyComposition().getToConfirmBPMN(composition);
							String fileName=fileManager.saveCompositionFile(composition, xmlToConfirm);
					        dao.getComposition().save(composition, composition, fileName);
					        dao.getDirtyComposition().confirm(composition);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}*/
	
	
	private void rabbitmqChangeResponse(Connection connection, String composition) throws IOException{
		String topic="*."+composition.toLowerCase();
		
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(RabbitMQConfig.RESPONSE_EXCHANGE, BuiltinExchangeType.TOPIC);
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, RabbitMQConfig.RESPONSE_EXCHANGE, topic);

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				
				 try {
						AdaptationResponse adaptationResponse=AdaptationResponse.parseJSON(new String(body));
						if(!adaptationResponse.getFrom().equals("Global")){
							if(adaptationResponse.getResponse()){
								dao.getDirtyComposition().addResponse(composition);
							}else{
								dao.getDirtyComposition().unAccept(composition);
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
	
	private Properties props;
	 private Properties getProps(){
		 if(this.props==null){
			 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
			 yamlFactory.setResources(new ClassPathResource("application.yml"));
			 this.props = yamlFactory.getObject();
		 }
        return this.props;
	 }
	
}
