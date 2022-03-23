package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;

@Component
public class DeleteElementManager {
	
	
	@Autowired
	AdaptationRuleCatalogue catalogue;
	
	private Participant modifiedParticipant;
	private Participant affectedParticipant;
	
	public int applyAdaptation(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		this.modifiedParticipant=modifiedParticipant;
		this.affectedParticipant=toAdaptParticipant;
	
		catalogue.configure(toAdaptParticipant, modifiedParticipant, change, model, modelToConfirm);
		
		switch(change.getString("elementType")){
			case "IntermediateThrowEvent": 
			case "EndEvent":
					if(this.hasAttachedData(change.getString("name"))){
						if(this.isPropagatedData(change.getString("name"))){
							return catalogue.executeRule(3); // RULE #3
						}else{
							return catalogue.executeRule(4); // RULE #4
						}
					}else{
						if(this.isPreviousThrowingEventInAffected(change.getString("name")))
								return catalogue.executeRule(2); // RULE #2
						else
								return catalogue.executeRule(1); // RULE #1
					}
			case "IntermediateCatchEvent": 
			case "StartEvent":
							return EvolutionProcess.GLOBAL_ADAPTATION;
		} 
		return EvolutionProcess.GLOBAL_ADAPTATION;
	}
	
	private boolean hasAttachedData(String event){
		return false;
	}
	
	private boolean isPropagatedData(String event){
		return false;
	}
	
	private boolean isPreviousThrowingEventInAffected(String event){
		
		for(Event e:this.modifiedParticipant.getProcess().getChildElementsByType(Event.class)){
			if(e.getName()!=null && e.getName().equalsIgnoreCase(event)){
				FlowNode node=getPreviousNode(e, new ArrayList<String>());
				
				if(node!=null){
					for(Event e2:this.affectedParticipant.getProcess().getChildElementsByType(Event.class)){
						if((e2 instanceof IntermediateThrowEvent || e2 instanceof EndEvent) &&
								e2.getName()!=null && e2.getName().equalsIgnoreCase(node.getName()))
									return true;
					}
				}
				
			}
		}
		
		return false;
	}
	
	private FlowNode getPreviousNode(FlowNode e, List<String> path){
		FlowNode node=e;
		path.add(node.getId());
		if(node instanceof IntermediateCatchEvent || node instanceof StartEvent) return node;
		else if(node instanceof BoundaryEvent){
			FlowNode attached=((BoundaryEvent)node).getAttachedTo();
			if(!path.contains(attached.getId())) return getPreviousNode(attached, path);
		}
		else if(node.getPreviousNodes().list().size()==0) return null;
		else{
			for(int i=0;i<node.getPreviousNodes().list().size();i++){
				FlowNode node2= getPreviousNode(node.getPreviousNodes().list().get(i),path);
				if(node2!=null) return node2;
			}
		}
		return null;
	}
	
}