package es.upv.pros.pvalderas.composition.bpmn.utils;

import java.util.HashMap;
import java.util.Map;
import org.dom4j.Namespace;



public class NameSpaceManager {
	
	private static NameSpaceManager instance;
	
	public static NameSpaceManager getCurrentInstance(){
		if(instance==null) instance=new NameSpaceManager();
		return instance;
	}
	
	private Map<String,String> uris = new HashMap<String,String>();
	
	private NameSpaceManager(){
		uris.put("bpmn","http://www.omg.org/spec/BPMN/20100524/MODEL");
		uris.put("xsi","http://www.w3.org/2001/XMLSchema-instance");
		uris.put("bpmndi","http://www.omg.org/spec/BPMN/20100524/DI");
		uris.put("dc","http://www.omg.org/spec/DD/20100524/DC");
		uris.put("di","http://www.omg.org/spec/DD/20100524/DI");
		uris.put("camunda","http://camunda.org/schema/1.0/bpmn");
	}
	
	public Namespace getNameSpace(String key){
		if(uris.containsKey(key))
			return new Namespace(key,uris.get(key));
		else 
			return null;
	}
	
	public Map<String,String> getAllUris(){
		return uris;
	}
}
