package es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jaxen.JaxenException;

import es.upv.pros.pvalderas.composition.bpmn.utils.XMLQuery;

public class SplittedSimplifier {
	private Document bpmn;	
	private XMLQuery query;
	private String compositionName;
	
	public SplittedSimplifier(String compositionName, String BPMNFragment) throws DocumentException{
		this.bpmn = DocumentHelper.parseText(BPMNFragment);
		this.query=new XMLQuery(this.bpmn);
		this.compositionName=compositionName;
	}
	
	public String simplify() throws JaxenException{
		this.updateIds();
		this.removeMessages();
		return bpmn.asXML();
	}
	
	private void updateIds() throws JaxenException{
		Node fragmentParticipant=query.selectSingleNode("//bpmn:participant[@id!='collapsedPoolParticipant']");
		String fragmentProcessId=fragmentParticipant.valueOf("@processRef");
		((Element)fragmentParticipant).addAttribute("processRef", fragmentProcessId.replace(compositionName+"_", ""));
		
		Node fragmentProcess=query.selectSingleNode("//bpmn:process[@id='"+fragmentProcessId+"']");
		((Element)fragmentProcess).addAttribute("id", fragmentProcessId.replace(compositionName+"_", ""));
	}
	
	private void removeMessages() throws JaxenException{
		
		List<Node> messages=query.selectNodes("//bpmn:message");
		for(Node message:messages){
			message.detach();
		}
		
		List<Node> messageDefs=query.selectNodes("//bpmn:messageEventDefinition");
		for(Node messageDef:messageDefs){
			Element m=(Element)messageDef;
			if(m.attribute("messageRef")!=null) m.remove(m.attribute("messageRef"));
		}
		
	}
	

		
}
