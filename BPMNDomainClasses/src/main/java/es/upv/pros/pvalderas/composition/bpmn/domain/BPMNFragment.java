package es.upv.pros.pvalderas.composition.bpmn.domain;

public class BPMNFragment
{
    private String composition;
    private String id;
    private String xml;
    private String microservice;
    private Integer numParticipants;
    
    public String getComposition() {
        return this.composition;
    }
    
    public void setComposition(final String composition) {
        this.composition = composition;
    }
    
    public String getId() {
        return this.id;
    }
    
    public void setId(final String id) {
        this.id = id;
    }
    
    public String getXml() {
        return this.xml;
    }
    
    public void setXml(final String xml) {
        this.xml = xml;
    }
    
    public String getMicroservice() {
        return this.microservice;
    }
    
    public void setMicroservice(final String microservice) {
        this.microservice = microservice;
    }

	public Integer getNumParticipants() {
		return numParticipants;
	}

	public void setNumParticipants(Integer numParticipants) {
		this.numParticipants = numParticipants;
	}
    
}