package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.rules;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONException;
import org.json.JSONObject;

import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNColors;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.BPMNElementManager;

public class Rule2 implements Rule{

	
	@Override
	public int execute(Participant toAdaptParticipant, Participant modifiedParticipant, JSONObject change,
			BpmnModelInstance model, BpmnModelInstance modelToConfirm, BPMNElementManager elementManager)
			throws JSONException {
	
		String deleteElement=change.getString("name");
		
		Process toAdaptProcess=toAdaptParticipant.getProcess();
		
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
		
		//Changes in model to Confirm
		SequenceFlow incomingFlow=modifiedEvent2.getIncoming().iterator().next();
		SequenceFlow outgoingFlow=modifiedEvent2.getOutgoing().iterator().next();
		
		elementManager.addSequenceFlow(modelToConfirm, modelToConfirm.getModelElementById(toAdaptProcess.getId()), incomingFlow.getSource(), outgoingFlow.getTarget(), null);
		
		elementManager.deleteFlow(modelToConfirm, toAdaptProcess.getId(), outgoingFlow);
		elementManager.deleteFlow(modelToConfirm, toAdaptProcess.getId(), incomingFlow);
		elementManager.deleteElement(modelToConfirm, toAdaptProcess.getId(),modifiedEvent2);
		
		
		
		//Changes in model to show
		incomingFlow=modifiedEvent.getIncoming().iterator().next();
		outgoingFlow=modifiedEvent.getOutgoing().iterator().next();
		
		elementManager.addSequenceFlow(model, toAdaptProcess, incomingFlow.getSource(), outgoingFlow.getTarget(), BPMNColors.GREEN);
		
		elementManager.drawElementInColor(outgoingFlow.getDiagramElement(), BPMNColors.RED);
		elementManager.drawElementInColor(modifiedEvent.getDiagramElement(), BPMNColors.RED);
		elementManager.drawElementInColor(incomingFlow.getDiagramElement(), BPMNColors.RED);
		
		return EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE;
		
	}
}
