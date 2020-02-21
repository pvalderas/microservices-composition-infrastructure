package es.upv.pros.pvalderas.composition.bpmn.domain;

public class BPMNComposition
{
    private String id;
    private String name;
    private String xml;
    
    public String getId() {
        return this.id;
    }
    
    public void setId(final String id) {
        this.id = id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public String getXml() {
        return this.xml;
    }
    
    public void setXml(final String xml) {
        this.xml = xml;
    }
}
