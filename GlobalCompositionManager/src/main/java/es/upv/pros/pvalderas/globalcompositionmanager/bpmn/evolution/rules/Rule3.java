package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNColors;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public class Rule3 implements Rule{

	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementManager) throws JSONException{
	
		
		//TODO
		
		return EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE;
	
	}
}
