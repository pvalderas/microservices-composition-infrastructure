package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;

@Component
public class UpdateElementManager {
	
	
	@Autowired
	AdaptationRuleCatalogue catalogue;
	
	private BpmnModelInstance model;

	@Autowired
	BPMNElementManager elementManager;
	
	public int applyAdaptation(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm, Boolean newEventExists) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		this.model=model;
	
		catalogue.configure(toAdaptParticipant, modifiedParticipant, change, model, modelToConfirm);
		
		switch(change.getString("elementType")){
			case "IntermediateThrowEvent": 
			case "EndEvent":
					if(this.hasAttachedData(change.getString("name"))){
						if(this.attachedDataIsPropagated(change.getString("name"))){
							return catalogue.executeRule(7); // RULE #7
						}else{
							return catalogue.executeRule(8); // RULE #8
						}
					}else{
						if(newEventExists)
								return catalogue.executeRule(6); // RULE #6
						else
								return catalogue.executeRule(5); // RULE #5
					}
			case "IntermediateCatchEvent": 
			case "StartEvent":
					if(this.hasAttachedData(change.getString("name"))){
						
					}else{
						return catalogue.executeRule(9); // RULE #9
					}
		} 
		return EvolutionProcess.GLOBAL_ADAPTATION;
	}
	
	private boolean hasAttachedData(String event){
		return false;
	}
	
	private boolean attachedDataIsPropagated(String event){
		return false;
	}
	
	
	
}