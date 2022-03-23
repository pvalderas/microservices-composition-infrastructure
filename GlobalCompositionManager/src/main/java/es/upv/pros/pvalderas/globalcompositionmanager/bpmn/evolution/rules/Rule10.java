package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public class Rule10 implements Rule {

	@Override
	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change,
			BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementCreator)
			throws JSONException {
		
		//TODO
		
		return EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE;
	}


}
