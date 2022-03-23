package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules.Rule;

@Component
public class AdaptationRuleCatalogue {
	
	@Autowired
	BPMNElementManager elementManager;
	
	private Participant toAdaptParticipant;
	private Participant modifiedParticipant;
	private JSONObject change;
	private BpmnModelInstance model;
	private BpmnModelInstance modelToConfirm;
	
	public void configure(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change, BpmnModelInstance model, BpmnModelInstance modelToConfirm){
		this.toAdaptParticipant=toAdaptParticipant;
		this.modifiedParticipant=modifiedParticipant;
		this.change=change;
		this.model=model;
		this.modelToConfirm=modelToConfirm;
	}
	
	public int executeRule(Integer num) throws InstantiationException, IllegalAccessException, ClassNotFoundException, JSONException{
		return ((Rule)Class.forName("es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules.Rule"+num).newInstance()).execute(toAdaptParticipant, modifiedParticipant, change, model, modelToConfirm, elementManager);
	}
}
