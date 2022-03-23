package es.upv.pros.pvalderas.composition.bpmn.domain;

import org.json.JSONException;
import org.json.JSONObject;

public class ParticipantChange {

		private String microservice;	
		private Integer id;
		private String composition;
		private String changes;
		private String date;
		private Integer accepted;
		private String dirtyXml;
		private String xmlToConfirm;
		
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		public String getChanges() {
			return changes;
		}
		public void setChanges(String changes) {
			this.changes = changes;
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
		public String getComposition() {
			return composition;
		}
		public void setComposition(String composition) {
			this.composition = composition;
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
		public String getXmlToConfirm() {
			return xmlToConfirm;
		}
		public void setXmlToConfirm(String xmlToConfirm) {
			this.xmlToConfirm = xmlToConfirm;
		}
		
		
		public String toJSON() throws JSONException{
			JSONObject j=new JSONObject();
			
			j.put("microservice",microservice);
			j.put("id",id);
			j.put("composition", composition);
			j.put("changes",changes);
			j.put("date",date);
			j.put("accepted",accepted);
			j.put("dirtyXml",dirtyXml);
			j.put("xmlToConfirm",xmlToConfirm);
			
			return j.toString();
		}
		
		
		public static ParticipantChange parseJSON(String participantChangeJSON) throws JSONException{
			JSONObject j=new JSONObject(participantChangeJSON);
			ParticipantChange change=new ParticipantChange();
			
			change.setMicroservice(j.getString("microservice"));
			change.setId(j.getInt("id"));
			change.setComposition(j.getString("composition"));
			change.setChanges(j.getString("changes"));
			change.setDate(j.getString("date"));
			change.setAccepted(j.has("accepted")?j.getInt("accepted"):null);
			change.setDirtyXml(j.getString("dirtyXml"));
			change.setXmlToConfirm(j.getString("xmlToConfirm"));
			
			return change;
		}
}
