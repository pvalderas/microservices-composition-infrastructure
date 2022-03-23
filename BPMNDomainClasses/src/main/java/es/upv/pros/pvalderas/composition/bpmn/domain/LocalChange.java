package es.upv.pros.pvalderas.composition.bpmn.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalChange {

		private Integer changeId;
		private String description;
		private String date;
		private Integer accepted;
		private String fragment;
		private String composition;
		private String xml;
		private String dirtyXml;
		private String microservice;
		
		public Integer getChangeId() {
			return changeId;
		}
		public void setChangeId(Integer id) {
			this.changeId = id;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String desc) {
			this.description = desc;
		}
		public String getDate() {
			return date;
		}
		public void setDate(String date) {
			this.date = date;
		}
		public Integer getAccepted() {
			return accepted;
		}
		public void setAccepted(Integer accepted) {
			this.accepted = accepted;
		}
		public String getFragment() {
			return fragment;
		}
		public void setFragment(String fragment) {
			this.fragment = fragment;
		}
		public String getComposition() {
			return composition;
		}
		public void setComposition(String composition) {
			this.composition = composition;
		}
		public String getXml() {
			return xml;
		}
		public void setXml(String xml) {
			this.xml = xml;
		}
		public String getDirtyXml() {
			return dirtyXml;
		}
		public void setDirtyXml(String dirtyXml) {
			this.dirtyXml = dirtyXml;
		}
		public String getMicroservice() {
			return microservice;
		}
		public void setMicroservice(String microservice) {
			this.microservice = microservice;
		}
		
		public String toJSON() throws JSONException{
			JSONObject changesMsg=new JSONObject();
	    	changesMsg.put("microservice",microservice);
	    	changesMsg.put("changeId",changeId);
	    	changesMsg.put("composition",composition);
	    	changesMsg.put("description", description);
	    	changesMsg.put("dirtyXml",dirtyXml);
	    	changesMsg.put("fragment",fragment);
	    	changesMsg.put("date",date);
	    	changesMsg.put("xml",xml);
	    	changesMsg.put("accepted",accepted);
	    	
	    	return changesMsg.toString();
		}
		
		public static LocalChange parseLocalChange(String localChangeJSON) throws JSONException{
			LocalChange change=new LocalChange();
			
			JSONObject changeJSON=new JSONObject(localChangeJSON);
			change.setAccepted(changeJSON.getInt("accepted"));
			change.setChangeId(changeJSON.getInt("changeId"));
			change.setComposition(changeJSON.getString("composition"));
			change.setDate(changeJSON.getString("date"));
			change.setDescription(changeJSON.getString("description"));
			change.setDirtyXml(changeJSON.getString("dirtyXml"));
			change.setFragment(changeJSON.getString("fragment"));
			change.setMicroservice(changeJSON.getString("microservice"));
			change.setXml(changeJSON.getString("xml"));
			
			return change;
		}
		
}
