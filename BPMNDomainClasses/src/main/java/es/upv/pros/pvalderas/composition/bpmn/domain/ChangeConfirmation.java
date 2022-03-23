package es.upv.pros.pvalderas.composition.bpmn.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class ChangeConfirmation {
	private String composition;
	private boolean confirmed;
	private String modifiedMicroservice;
	public String getComposition() {
		return composition;
	}
	public void setComposition(String composition) {
		this.composition = composition;
	}
	public boolean isConfirmed() {
		return confirmed;
	}
	public void setConfirmed(boolean accepted) {
		this.confirmed = accepted;
	}
	public String getModifiedMicroservice() {
		return modifiedMicroservice;
	}
	public void setModifiedMicroservice(String modifiedMicroservice) {
		this.modifiedMicroservice = modifiedMicroservice;
	}
	public String toJSON() throws JSONException{
		JSONObject j=new JSONObject();
		j.put("composition", composition);
		j.put("accepted", confirmed);
		j.put("modifiedMicroservice", modifiedMicroservice);
		return j.toString();
	}
	
	public static ChangeConfirmation parseJSON(String confirmationJSON) throws JSONException{
		ChangeConfirmation confirmation=new ChangeConfirmation();
		JSONObject j=new JSONObject(confirmationJSON);
		confirmation.setConfirmed(j.getBoolean("accepted"));
		confirmation.setComposition(j.getString("composition"));
		confirmation.setModifiedMicroservice(j.getString("modifiedMicroservice"));
		return confirmation;
	}
}
