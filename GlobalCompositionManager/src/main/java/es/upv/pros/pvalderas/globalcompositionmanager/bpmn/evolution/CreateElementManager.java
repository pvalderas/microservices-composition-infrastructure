package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;

@Component
public class CreateElementManager {
	
	
	@Autowired
	AdaptationRuleCatalogue catalogue;
	
	private BpmnModelInstance model;

	
	public int applyAdaptation(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm, boolean newEventExists) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		this.model=model;
	
		catalogue.configure(toAdaptParticipant, modifiedParticipant, change, model, modelToConfirm);
		
		switch(change.getString("elementType")){
			case "IntermediateThrowEvent": 
			case "EndEvent":
					if(change.getString("name")!=null){
						if(this.hasAttachedData(change.getString("name"))){
							
						}else{
							if(newEventExists)
									return catalogue.executeRule(13); // RULE #1
							else
									return catalogue.executeRule(12); // RULE #2
						}
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
		
}