package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public class Rule5 implements Rule {

	@Override
	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change,
			BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementCreator)
			throws JSONException {
		
		
		for(Event event:toAdaptParticipant.getProcess().getChildElementsByType(Event.class)){
			if((event instanceof IntermediateCatchEvent || event instanceof StartEvent) && 
					event.getName()!=null && event.getName().equalsIgnoreCase(change.getString("name"))){
						((Event)modelToConfirm.getModelElementById(event.getId())).setName(change.getString("newName"));
			}
		}
		
		return EvolutionProcess.AUTOMATIC;
	}

}
