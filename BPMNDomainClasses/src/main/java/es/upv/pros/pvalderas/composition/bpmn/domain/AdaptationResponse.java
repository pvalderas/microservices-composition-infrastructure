package es.upv.pros.pvalderas.composition.bpmn.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class AdaptationResponse {
	private Integer changeId;
    private String from;
    private String to;
    private String composition;
    private Boolean response;
	public Integer getChangeId() {
		
		return changeId;
	}
	public void setChangeId(Integer changeId) {
		this.changeId = changeId;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getComposition() {
		return composition;
	}
	public void setComposition(String composition) {
		this.composition = composition;
	}
	public Boolean getResponse() {
		return response;
	}
	public void setResponse(Boolean response) {
		this.response = response;
	}
    
    public String toJSON() throws JSONException{
    	JSONObject j=new JSONObject();
    	
    	j.put("changeId", changeId);
    	j.put("from", from);
    	j.put("to", to);
    	j.put("composition", composition);
    	j.put("response", response);
    	
    	return j.toString();
    }
    
    public static AdaptationResponse parseJSON(String adaptationResponseJSON) throws JSONException{
    	JSONObject responseJSON=new JSONObject(adaptationResponseJSON);
    	
    	AdaptationResponse response=new AdaptationResponse();
    	if(responseJSON.has("changeId")) response.setChangeId(responseJSON.getInt("changeId"));
    	response.setComposition(responseJSON.getString("composition"));
    	response.setFrom(responseJSON.getString("from"));
    	response.setTo(responseJSON.getString("to"));
    	response.setResponse(responseJSON.getBoolean("response"));
    	return response;
    }
}
