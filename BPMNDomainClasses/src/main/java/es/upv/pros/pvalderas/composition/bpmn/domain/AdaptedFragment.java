package es.upv.pros.pvalderas.composition.bpmn.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class AdaptedFragment {
	private String composition;
	private String microservice;
	private String modifiedMicroservice;
	private Integer type;
	private String bpmn;
	private String bpmnToConfirm;
	private String changesJSON;
	
	public String getComposition() {
		return composition;
	}
	public void setComposition(String composition) {
		this.composition = composition;
	}
	public String getBpmn() {
		return bpmn;
	}
	public void setBpmn(String bpmn) {
		this.bpmn = bpmn;
	}
	public String getMicroservice() {
		return microservice;
	}
	public void setMicroservice(String microservice) {
		this.microservice = microservice;
	}
	public Integer getType() {
		return type;
	}
	public void setType(Integer type) {
		this.type = type;
	}
	public String getChangesJSON() {
		return changesJSON;
	}
	public void setChangesJSON(String changesJSON) {
		this.changesJSON = changesJSON;
	}
	public String getModifiedMicroservice() {
		return modifiedMicroservice;
	}
	public void setModifiedMicroservice(String modifiedMicroservice) {
		this.modifiedMicroservice = modifiedMicroservice;
	}
	
	public String getBpmnToConfirm() {
		return bpmnToConfirm;
	}
	public void setBpmnToConfirm(String bpmnToConfirm) {
		this.bpmnToConfirm = bpmnToConfirm;
	}
	public String toJSON() throws JSONException{
		JSONObject j=new JSONObject();
		j.put("composition", composition);
		j.put("microservice", microservice);
		j.put("modifiedMicroservice", modifiedMicroservice);
		j.put("type", type);
		j.put("bpmn", bpmn);
		j.put("bpmnToConfirm", bpmnToConfirm);
		j.put("changesJSON", changesJSON);
		
		return j.toString();
	}
	
	public static AdaptedFragment parseJSON(String adaptedFragmentJSON) throws JSONException{
		JSONObject j=new JSONObject(adaptedFragmentJSON);
		AdaptedFragment adaptedFragment=new AdaptedFragment();
		adaptedFragment.setBpmn(j.getString("bpmn"));
		adaptedFragment.setBpmnToConfirm(j.getString("bpmnToConfirm"));
		adaptedFragment.setComposition(j.getString("composition"));
		adaptedFragment.setMicroservice(j.getString("microservice"));
		adaptedFragment.setModifiedMicroservice(j.getString("modifiedMicroservice"));
		adaptedFragment.setType(j.getInt("type"));
		adaptedFragment.setChangesJSON(j.getString("changesJSON"));
		return adaptedFragment;
	}
}
