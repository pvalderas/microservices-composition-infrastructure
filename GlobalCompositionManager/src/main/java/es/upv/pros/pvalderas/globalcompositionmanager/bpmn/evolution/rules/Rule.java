package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public interface Rule {

	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementManager) throws JSONException;

}
