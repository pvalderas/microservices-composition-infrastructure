package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNColors;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public class Rule1 implements Rule{

	@Override
	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change,
			BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementManager)
			throws JSONException {
		
		String deleteElement=change.getString("name");
		
		Process toAdaptProcess=toAdaptParticipant.getProcess();
		Process modifiedProcess=modifiedParticipant.getProcess();
		
		Event modifiedEvent=null;
		Event modifiedEvent2=null;
		for(IntermediateCatchEvent catchEvent:toAdaptProcess.getChildElementsByType(IntermediateCatchEvent.class)){
			if(catchEvent.getName().equalsIgnoreCase(deleteElement)){
				modifiedEvent=catchEvent;
				modifiedEvent2=((Event)modelToConfirm.getModelElementById(catchEvent.getId()));
			}
		}
		if(modifiedEvent==null){
			StartEvent startEventtoAdapt=toAdaptProcess.getChildElementsByType(StartEvent.class).iterator().next();
			modifiedEvent=startEventtoAdapt;
			modifiedEvent2=((Event)modelToConfirm.getModelElementById(modifiedEvent.getId()));
		}
		
		Event previousThrowingEvent=(Event)getPreviousThrowingNode(modifiedParticipant, deleteElement);
		
		if(previousThrowingEvent!=null){
			elementManager.addMessageFlow(modelToConfirm, modelToConfirm.getModelElementById(previousThrowingEvent.getId()), modifiedEvent2, null);
			elementManager.addMessageFlow(model, previousThrowingEvent, modifiedEvent, BPMNColors.ORANGE);
			
			modifiedEvent.setName(previousThrowingEvent.getName());
			modifiedEvent2.setName(previousThrowingEvent.getName());
		}else{
			StartEvent startEvent=modifiedProcess.getChildElementsByType(StartEvent.class).iterator().next();
			Event sourceEvent=startEvent;
			for(MessageFlow messageFlow:model.getDefinitions().getChildElementsByType(Collaboration.class).iterator().next().getChildElementsByType(MessageFlow.class)){
				if(messageFlow.getTarget().getId().equals(startEvent.getId())) sourceEvent=(Event)messageFlow.getSource();
			}
			
			elementManager.addMessageFlow(model, sourceEvent, modifiedEvent, BPMNColors.ORANGE);
			if(startEvent!=sourceEvent) elementManager.addMessageFlow(modelToConfirm, modelToConfirm.getModelElementById(sourceEvent.getId()), modifiedEvent2, null);
			modifiedEvent.setName(startEvent.getName());
			modifiedEvent2.setName(startEvent.getName());
		}
		
		modifiedEvent.getDiagramElement().setAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke",BPMNColors.ORANGE);
		
		return EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE;
	}
	
	private Event getPreviousThrowingNode(Participant modifiedParticipant, String event){
		for(Event e:modifiedParticipant.getProcess().getChildElementsByType(Event.class)){
			if(e.getName()!=null && e.getName().equalsIgnoreCase(event)){
				return (Event)getPreviousThrowingNode(e.getPreviousNodes().list().get(0), new ArrayList<String>());
			}
		}
		return null;
	}
	
	
	private FlowNode getPreviousThrowingNode(FlowNode e, List<String> path){
		FlowNode node=e;
		path.add(node.getId());
		if(node instanceof IntermediateThrowEvent) return node;
		else if(node instanceof BoundaryEvent){
			FlowNode attached=((BoundaryEvent)node).getAttachedTo();
			if(!path.contains(attached.getId())) return getPreviousThrowingNode(attached, path);
		}
		else if(node.getPreviousNodes().list().size()==0) return null;
		else{
			for(int i=0;i<node.getPreviousNodes().list().size();i++){
				FlowNode node2= getPreviousThrowingNode(node.getPreviousNodes().list().get(i),path);
				if(node2!=null) return node2;
			}
		}
		return null;
	}
	
	
}
