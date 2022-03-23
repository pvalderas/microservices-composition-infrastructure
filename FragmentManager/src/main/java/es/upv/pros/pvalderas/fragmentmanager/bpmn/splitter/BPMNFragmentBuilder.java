package es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter;

public interface BPMNFragmentBuilder {
	public String getMicroserviceId();
	public String getFragmentId();
	public String asXML();
}
