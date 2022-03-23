package es.upv.pros.pvalderas.compositioncoordinator.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.ChangeConfirmation;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.eventbroker.utils.RabbitMQConfig;
import es.upv.pros.pvalderas.compositioncoordinator.bpmn.FileManager;
import es.upv.pros.pvalderas.compositioncoordinator.dao.DAO;

@Component
public class EventManager {
	
	private static String REGISTERED="";

	@Autowired
	private RuntimeService runtimeService;
	
	@Autowired
	private DAO dao;
	
	@Autowired
	private FileManager fileManager;
	
	private String thisMicroservice;
	private String composition;
	private List<String> messages;

	public void registerEventListener(String thisMicroservice, String composition) throws IOException, TimeoutException{
		if(!REGISTERED.contains(composition)){
			REGISTERED=REGISTERED+composition+";";
			this.thisMicroservice=thisMicroservice;
			this.composition=composition;
			switch(BrokerConfig.getBrokerType()){
				case RabbitMQConfig.ID: rabbitmqRegisterEvent(); break;
			}
		}
		messages=new ArrayList<String>();
		BpmnModelInstance fragment=Bpmn.readModelFromFile(fileManager.getBPMNFile(composition));
		for(Message message:fragment.getModelElementsByType(Message.class)){
			messages.add(message.getName());
		}
	}
	
	private void rabbitmqRegisterEvent() throws IOException, TimeoutException{	
		Connection connection = RabbitMQConfig.getconnection();	
		runtimeEvents(connection);
		adaptationEvents(connection);
		responseFromChanges(connection);
		confirmationChanges(connection);
	}
	
	private void runtimeEvents(Connection connection) throws IOException{
		//String topic=thisMicroservice.toLowerCase()+"."+composition.toLowerCase()+".*";
		String topic=composition.toLowerCase()+".*";
		
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(RabbitMQConfig.RUNTIME_EXCHANGE, BuiltinExchangeType.TOPIC);
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, RabbitMQConfig.RUNTIME_EXCHANGE, topic);

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				 
				
					String message=new String(body);
					JSONObject messageJSON;
					try {
						
						messageJSON = new JSONObject(message);
						
						if(messages.contains(messageJSON.getString("message"))){
							Clients.currentClient.put(composition.toLowerCase(), messageJSON.getString("client")); 
							runtimeService.createMessageCorrelation(messageJSON.getString("message")).correlate();	
							System.out.println("Received Message: "+ message);
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
				
			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
	
	private void adaptationEvents(Connection connection) throws IOException{
		String topic=thisMicroservice.toLowerCase()+"."+composition.toLowerCase();
		
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(RabbitMQConfig.ADAPTATION_EXCHANGE, BuiltinExchangeType.TOPIC);
		String COLA_CONSUMER = channel.queueDeclare().getQueue();
		channel.queueBind(COLA_CONSUMER, RabbitMQConfig.ADAPTATION_EXCHANGE, topic);

		Consumer consumer = new DefaultConsumer(channel) {
			 @Override
			 public void handleDelivery(String consumerTag, Envelope envelope, 
					 					AMQP.BasicProperties properties, byte[] body) throws IOException {
				
					String adaptation=new String(body);
					
					
					try {
						AdaptedFragment adaptedFragment=AdaptedFragment.parseJSON(adaptation);
						dao.getParticipantChanges().save(adaptedFragment.getModifiedMicroservice(), 
								-1, 
								adaptedFragment.getComposition(), 
								adaptedFragment.getChangesJSON(), 
								adaptedFragment.getBpmn(),
								adaptedFragment.getBpmnToConfirm(),
								adaptedFragment.getType());
		/*
						switch(adaptedFragment.getType()){
							case EvolutionProcess.AUTOMATIC: break;
															 
							case EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE: dao.saveParticipantChange(adaptedFragment.getMicroservice(), 
																										-1, 
																										composition, 
																										"", 
																										adaptedFragment.getBpmn(),
																										adaptedFragment.getType());
																			  break;
						}
			*/		
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
				
			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
	
	
	private void responseFromChanges(Connection connection) throws IOException{
		String topic=thisMicroservice.toLowerCase()+"."+composition.toLowerCase();
		
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
						if(!adaptationResponse.getResponse()){
							dao.getLocalChanges().rejectLocalChangesByComposition(composition);
						}else if(adaptationResponse.getResponse()){
							if(adaptationResponse.getFrom().equals("Global")) dao.getLocalChanges().acceptGlobalLocalChangesByComposition(composition);
							else{
								/*Integer accepted=dao.getLocalChanges().addTrueResponseToLocalChangeByComposition(adaptationResponse.getComposition());
								if(accepted==dao.getLocalChanges().getAffectedParticipantsByComposition(composition)){
									dao.getLocalChanges().acceptGlobalAndParticipantsLocalChangesByComposition(composition);
								}*/
								dao.getLocalChanges().addTrueResponseToLocalChangeByComposition(adaptationResponse.getComposition());
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
	
	
	private void confirmationChanges(Connection connection) throws IOException{
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
						
						if(confirmation.getModifiedMicroservice().equalsIgnoreCase(thisMicroservice)){
							dao.getLocalChanges().acceptGlobalAndParticipantsLocalChangesByComposition(composition);
						}else{
							if(confirmation.isConfirmed()){
								dao.getParticipantChanges().confirmByComposition(confirmation.getComposition());
							}else{
								dao.getParticipantChanges().rejectByComposition(confirmation.getComposition());
							}
						}
						
					} catch (JSONException e) {
						e.printStackTrace();
					}

			 }
		 };
		channel.basicConsume(COLA_CONSUMER, true, consumer);
	}
}
