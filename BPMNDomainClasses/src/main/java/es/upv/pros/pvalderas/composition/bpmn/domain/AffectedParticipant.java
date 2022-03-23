package es.upv.pros.pvalderas.composition.bpmn.domain;

public class AffectedParticipant{
	private String microservice;
	private Integer adaptationType;
	private String changesJSON;
	
	
	public AffectedParticipant(String microservice, Integer type, String changesJSON) {
		this.microservice = microservice;
		this.adaptationType = type;
		this.changesJSON=changesJSON;
	}
	
	public String getChangesJSON() {
		return changesJSON;
	}
	public void setChangesJSON(String changesJSON) {
		this.changesJSON = changesJSON;
	}
	public String getMicroservice() {
		return microservice;
	}
	public void setMicroservice(String microservice) {
		this.microservice = microservice;
	}
	public Integer getAdaptationType() {
		return adaptationType;
	}
	public void setAdaptationType(Integer type) {
		this.adaptationType = type;
	}
	
}
