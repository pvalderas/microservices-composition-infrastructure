package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedModel;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNElement;
import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.composition.bpmn.domain.LocalChange;

@Component
public class AdaptationProvider {
	
	public static final String ADAPTED_ANNOTATION = "Adapted Microservice";
	public static final String MODIFIED_ANNOTATION = "Modified Microservice";
	
	private BpmnModelInstance model;
	private BpmnModelInstance modelToConfirm;
	private List<Participant> participants;
	private Participant modifiedParticipant;
	
	private String compoFile;
	
	@Autowired
	DeleteElementManager deleteElementManager;
	
	@Autowired
	UpdateElementManager updateElementManager;
	
	@Autowired
	CreateElementManager createElementManager;
	
	@Autowired
	BPMNElementManager elementManager;
	
	public AdaptedModel getAdaptedModel(LocalChange localChange) throws JSONException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		
		loadComposition(localChange.getComposition(), localChange.getMicroservice());
		
		Map<String,Integer> adaptation=new Hashtable<String,Integer>();
		Map<String,String> changes=new Hashtable<String,String>();
		Integer allAutomatic=null;
		
		JSONArray changesJSON=new JSONArray(localChange.getDescription());
		
		boolean newEventExists= this.newEventExists(changesJSON);
		
		for(Participant participant:this.participants){
	
			Process toAdaptProcess=participant.getProcess();
	
			String participantName=participant.getAttributeValue("name");
			
			JSONArray affectingChanges=new JSONArray();
			
			for(int i=0;i<changesJSON.length();i++){
				JSONObject change = changesJSON.getJSONObject(i);
		
				if(isAffectingChange(change, toAdaptProcess)){
					allAutomatic=0;
					int response=-1;

					switch(change.getString("action")){
						case "deleteElement": response=deleteElementManager.applyAdaptation(participant, this.modifiedParticipant, change, model, modelToConfirm); break;
						case "updateElement": response=updateElementManager.applyAdaptation(participant, this.modifiedParticipant, change, model, modelToConfirm, newEventExists); break;
						case "createElement": response=createElementManager.applyAdaptation(participant, this.modifiedParticipant, change, model, modelToConfirm, newEventExists); break;
					}
					if(response==EvolutionProcess.GLOBAL_ADAPTATION){
						adaptation.put(participantName, EvolutionProcess.GLOBAL_ADAPTATION);
					}else{
						allAutomatic+=response; //EvolutionProcess.AUTOMATIC=0, si allAutomatic=0 all adaptations are automatic
					}
					affectingChanges.put(change);
				}
			}
			
			if(adaptation.get(participantName)==null && allAutomatic!=null && allAutomatic==0 ){
				adaptation.put(participantName, EvolutionProcess.AUTOMATIC);
			}else if(adaptation.get(participantName)==null && allAutomatic!=null){
				adaptation.put(participantName, EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE);
				elementManager.attachTextAnnotationToParticipant(model, participant,ADAPTED_ANNOTATION, BPMNColors.ORANGE);
				elementManager.attachTextAnnotationToParticipant(model, modifiedParticipant, MODIFIED_ANNOTATION, BPMNColors.RED);
				
			}
			changes.put(participantName, affectingChanges.toString());
			allAutomatic=null;
		}
		
		AdaptedModel adaptedModel=new AdaptedModel();
		
		for(Map.Entry<String,Integer> adapt:adaptation.entrySet()){
			adaptedModel.addAffectedParticipant(adapt.getKey(), adapt.getValue(),changes.get(adapt.getKey()));
		}
		adaptedModel.setBpmn(Bpmn.convertToString(model));
		adaptedModel.setBpmnToConfirm(Bpmn.convertToString(modelToConfirm));
		adaptedModel.setTotalParticipants(participants.size());
		adaptedModel.setModifiedParticipant(localChange.getMicroservice());
		
		return adaptedModel;
		
	}
	
	private boolean newEventExists(JSONArray changes) throws JSONException{
		
		boolean exists=false;
		for(int i=0;i<changes.length();i++){
			JSONObject change=changes.getJSONObject(i);
			String event=null;
			if(change.getString("action").equals("createElement")) event=change.getString("name");
			if(change.getString("action").equals("updateElement")) event=change.getString("newName");
			if(event!=null){
				for(Process p:this.model.getDefinitions().getChildElementsByType(Process.class)){
					for(Event e:p.getChildElementsByType(Event.class)){
						if(e.getName()!=null && e.getName().equalsIgnoreCase(event)){
							elementManager.drawElementInColor(e.getDiagramElement(), BPMNColors.PURPLE);
							exists=true;
						}
					}
				}
			}
		}
		return exists;
	}
	
	private void loadComposition(String composition, String modifiedMicroservice){
		this.model=Bpmn.readModelFromFile(this.getCompoXMLFile(composition));
		this.modelToConfirm=model.clone();
		this.participants=new ArrayList<Participant>();
		Collection<Participant> participants=model.getModelElementsByType(Participant.class);
		
		for(Participant participant:participants){
			if(participant.getName().equalsIgnoreCase(modifiedMicroservice)) this.modifiedParticipant=participant;
			else this.participants.add(participant);
		}
	}
	
	
	private boolean isAffectingChange(JSONObject change, Process participant) throws JSONException{
		Collection<Event> events=participant.getChildElementsByType(Event.class);
		for(Event e:events){
			if(change.getString("name").equalsIgnoreCase(e.getName())){
					return true;
			}
		}
		return false;
	}
	
	/*
	
	private ArrayList<BPMNElement> getCoordinationElements(Process participant){
		ArrayList<BPMNElement> coordinationElements=new ArrayList<BPMNElement>();
		
		Collection<StartEvent> startEvents=participant.getChildElementsByType(StartEvent.class);
		Collection<EndEvent> endEvents=participant.getChildElementsByType(EndEvent.class);
		Collection<IntermediateThrowEvent> throwEvents=participant.getChildElementsByType(IntermediateThrowEvent.class);
		Collection<IntermediateCatchEvent> catchEvents=participant.getChildElementsByType(IntermediateCatchEvent.class);
	
		for(Event event:startEvents){
			BPMNElement e=this.getBPMNElement(event);
			e.setType("startEvent");
			coordinationElements.add(e);
		}
		
		for(Event event:endEvents){
			BPMNElement e=this.getBPMNElement(event);
			e.setType("endEvent");
			coordinationElements.add(e);
		}
		
		for(Event event:throwEvents){
			BPMNElement e=this.getBPMNElement(event);
			e.setType("IntermediateThrowEvent");
			coordinationElements.add(e);
		}
		
		for(Event event:catchEvents){
			BPMNElement e=this.getBPMNElement(event);
			e.setType("IntermediateCatchEvent");
			coordinationElements.add(e);
		}
		
		return coordinationElements;
	}*/

	/*private BPMNElement getBPMNElement(Event event){
		BPMNElement e=new BPMNElement();
		e.setId(event.getAttributeValue("id"));
		e.setName(event.getAttributeValue("name"));
		return e;
	}*/
	
	
	private File getCompoXMLFile(String composition){
	    
	    File dir=new File("compositions");
	    String compositions[]=dir.list();
	    for(String compo:compositions){
	    	if(compo.indexOf(composition)>=0){
	    			compoFile=compo;
		    }
	    }
	    File bpmn = new File("compositions/"+compoFile);
	    return bpmn;
	}
}
