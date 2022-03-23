package es.upv.pros.pvalderas.composition.bpmn.domain;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AdaptedModel {
	private String bpmn;
	private String bpmnToConfirm;
	private String composition;
	private String modifiedParticipant;
	private List<AffectedParticipant> affectedParticipants=new ArrayList<AffectedParticipant>();
	private int totalParticipants;
	private int responses;

	// -1 (Not confirmed) 
	// 0 (Rejected) 
	// 1 (Accepted only by Global) 
	// 2 (Accepted by Global and Affected) 
	// -2 (Accepted by Global rejected by some participant)
	private Integer confirmed;
	
	public String getBpmn() {
		return bpmn;
	}
	public void setBpmn(String bpmn) {
		this.bpmn = bpmn;
	}
	public String getComposition() {
		return composition;
	}
	public void setComposition(String composition) {
		this.composition = composition;
	}
	public void addAffectedParticipant(String microservice, Integer type, String changesJSON){
		affectedParticipants.add(new AffectedParticipant(microservice, type, changesJSON));
	}
	public List<AffectedParticipant> getAffectedParticipants(){
		return affectedParticipants;
	}
	public int getTotalParticipants() {
		return totalParticipants;
	}
	public void setTotalParticipants(int totalParticipants) {
		this.totalParticipants = totalParticipants;
	}
	public String getModifiedParticipant() {
		return modifiedParticipant;
	}
	public void setModifiedParticipant(String modifiedParticipant) {
		this.modifiedParticipant = modifiedParticipant;
	}
	public String getBpmnToConfirm() {
		return bpmnToConfirm;
	}
	public void setBpmnToConfirm(String bpmnToConfirm) {
		this.bpmnToConfirm = bpmnToConfirm;
	}
	
	public Integer getConfirmed() {
		return confirmed;
	}
	public void setConfirmed(Integer confirmed) {
		this.confirmed = confirmed;
	}
	
	public int getResponses() {
		return responses;
	}
	public void setResponses(int responses) {
		this.responses = responses;
	}
	public String toJSON() throws JSONException{
		JSONObject adaptedModel=new JSONObject();
		JSONArray types=new JSONArray();
		for(AffectedParticipant t:affectedParticipants){
			JSONObject j=new JSONObject();
			j.put("microservice", t.getMicroservice());
			j.put("type", t.getAdaptationType());
			j.put("changesJSON", t.getChangesJSON());
			types.put(j);
		}
		adaptedModel.put("responses", responses);
		adaptedModel.put("bpmn", bpmn);
		adaptedModel.put("bpmnToConfirm", bpmnToConfirm);
		adaptedModel.put("composition", composition);
		adaptedModel.put("adaptationType", types);
		adaptedModel.put("totalParticipants", totalParticipants);
		adaptedModel.put("modifiedParticipant", modifiedParticipant);
		adaptedModel.put("confirmed", confirmed);
		return adaptedModel.toString();
	}
	
	public static AdaptedModel parseJSON(String adaptation) throws JSONException{
		JSONObject adaptedModelJSON=new JSONObject(adaptation);
		AdaptedModel model=new AdaptedModel();
		model.setBpmn(adaptedModelJSON.getString("bpmn"));
		model.setBpmnToConfirm(adaptedModelJSON.getString("bpmnToConfirm"));
		model.setComposition(adaptedModelJSON.getString("composition"));
		model.setTotalParticipants(adaptedModelJSON.getInt("totalParticipants"));
		model.setModifiedParticipant(adaptedModelJSON.getString("modifiedParticipant"));
		model.setConfirmed(adaptedModelJSON.getInt("confirmed"));
		JSONArray types=adaptedModelJSON.getJSONArray("adaptationType");
		for(int i=0;i<types.length();i++){
			model.addAffectedParticipant(types.getJSONObject(i).getString("microservice"),types.getJSONObject(i).getInt("type"), types.getJSONObject(i).getString("changesJSON"));
		}
		model.setResponses(adaptedModelJSON.getInt("responses"));
		return model;
	}
	
	public String getAffectedParticipantsJSON() throws JSONException{
		JSONArray types=new JSONArray();
		for(AffectedParticipant t:affectedParticipants){
			JSONObject j=new JSONObject();
			j.put("microservice", t.getMicroservice());
			j.put("type", t.getAdaptationType());
			j.put("changesJSON", t.getChangesJSON());
			types.put(j);
		}
		return types.toString();
	}
	
	public void setAffectedParticipantsJSON(String participantsJSON) throws JSONException{
		JSONArray types=new JSONArray(participantsJSON);
		for(int i=0;i<types.length();i++){
			this.addAffectedParticipant(types.getJSONObject(i).getString("microservice"),types.getJSONObject(i).getInt("type"), types.getJSONObject(i).getString("changesJSON"));
		}
	}
}
