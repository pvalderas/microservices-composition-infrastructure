package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.joiner;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPathException;
import org.jaxen.JaxenException;

import es.upv.pros.pvalderas.composition.bpmn.utils.XMLQuery;

public class Joiner {
	private Document bpmn;	
	private Document fragment;	
	private XMLQuery queryBpmn;
	private XMLQuery queryFragment;
	
	private String fragmentProcessId;
	private String fragmentParticipantId;
	private String fragmentParticipantName;
	private String bpmnProcessId;
	private String bpmnParticipantId;
	
	private Map<String, String> messageSources;
	private Map<String, String> messageTargets;
	private Map<String, String> messageColors;
	
	public Joiner(String bpmn, String fragment) throws DocumentException{
		this.bpmn = DocumentHelper.parseText(bpmn);
		this.fragment = DocumentHelper.parseText(fragment);
		queryFragment=new XMLQuery(this.fragment);
		queryBpmn=new XMLQuery(this.bpmn);
	}
	
	public String join() throws JaxenException{
		
		Node fragmentProcess=this.getFragmentProcess();
		Node bpmnProcess=this.getBPMNProcess();

		if(bpmnProcess!=null){
			
			this.messageSources=new Hashtable<String, String>();
			this.messageTargets=new Hashtable<String, String>();
			this.messageColors=new Hashtable<String, String>();
			
			
			
			Node diagramBpmn = queryBpmn.selectSingleNode("(//bpmndi:BPMNDiagram/bpmndi:BPMNPlane)");
			getMessageIds();
			this.removeOldViews(diagramBpmn);
			
			Element root=this.bpmn.getRootElement();
			
			List<Element> elements = root.elements();
			elements.add(elements.indexOf(bpmnProcess), (Element)fragmentProcess.clone());
			root.remove(bpmnProcess);
			//root.add((Element)fragmentProcess.clone());
			this.addNewViews(diagramBpmn);
			
		}
		
		return this.bpmn.asXML();
	}
	
	
	private Node getFragmentProcess() throws JaxenException{
		Node fragmentParticipant=queryFragment.selectSingleNode("//bpmn:participant[@id!='collapsedPoolParticipant']");
		fragmentProcessId=fragmentParticipant.valueOf("@processRef");
		fragmentParticipantId=fragmentParticipant.valueOf("@id");
		fragmentParticipantName=fragmentParticipant.valueOf("@name");
		Node fragmentProcess=queryFragment.selectSingleNode("//bpmn:process[@id='"+fragmentProcessId+"']");
		return fragmentProcess;
	}
	
	private Node getBPMNProcess() throws JaxenException{
		Node bpmnParticipant=queryBpmn.selectSingleNode("//bpmn:participant[@id='"+fragmentParticipantId+"']");
		
		if(bpmnParticipant==null){
			bpmnParticipant=queryBpmn.selectSingleNode("//bpmn:participant[@name='"+fragmentParticipantName+"']");
		}
		
		if(bpmnParticipant!=null){
			bpmnProcessId=bpmnParticipant.valueOf("@processRef");
			bpmnParticipantId=bpmnParticipant.valueOf("@id");
		
			Node bpmnProcess=queryBpmn.selectSingleNode("//bpmn:process[@id='"+bpmnProcessId+"']");
			return bpmnProcess;
		}
		
		return null;
	}
	
	
	private void removeOldViews(Node diagramBpmn) throws JaxenException{
		
		List<Node> viewsBpmn = queryBpmn.selectNodes("(//bpmndi:BPMNShape | //bpmndi:BPMNEdge)");
		
		String idQuery = "//bpmn:process[@id='"+bpmnProcessId+"']/*[@id='%%']";
		
		
		for(Node view: viewsBpmn){
			String refID=view.valueOf("@bpmnElement");
			if(refID.equals(bpmnParticipantId) || 
					queryBpmn.selectSingleNode(idQuery.replace("%%", refID))!=null){
				((Element)diagramBpmn).remove(view);
			}
		}
	}
	
	private void addNewViews(Node diagramBpmn) throws JaxenException{
		List<Node> viewsFragment = queryFragment.selectNodes("(//bpmndi:BPMNShape | //bpmndi:BPMNEdge)");
		
		String idQuery = "//bpmn:process[@id='"+fragmentProcessId+"']/*[@id='%%']";
		
		for(Node view: viewsFragment){
			String refID=view.valueOf("@bpmnElement");
			if(refID.equals(fragmentParticipantId) || queryFragment.selectSingleNode(idQuery.replace("%%", refID))!=null){
				((Element)diagramBpmn).add((Element)view.clone());
			}
		}
		
		for(Map.Entry<String,String> message:this.messageSources.entrySet()){
			/*NameSpaceManager nsm=NameSpaceManager.getCurrentInstance();
			Element edge = DocumentHelper.createElement(new QName("BPMNEdge", nsm.getNameSpace("bpmndi")));
			edge.addAttribute("id", message.getKey()+"_di");
			edge.addAttribute("bpmnElement", message.getKey());	*/
			
			List<Node> shape=queryBpmn.selectNodes("//bpmndi:BPMNEdge[@bpmnElement='"+message.getKey()+"']/di:waypoint");
			String values[]=message.getValue().split(";");
			if(shape.size()>0){
				((Element)shape.get(0)).addAttribute("x", values[0]);
				((Element)shape.get(0)).addAttribute("y", values[1]);
			}
			if(this.messageColors.get(message.getKey())!=null){
				Node edge=queryBpmn.selectSingleNode("//bpmndi:BPMNEdge[@bpmnElement='"+message.getKey()+"']");
				((Element)edge).addAttribute("bioc:stroke", this.messageColors.get(message.getKey()));
				((Element)edge).addAttribute("xmlns:bioc","http://bpmn.io/schema/bpmn/biocolor/1.0");
			}
		}
		
		for(Map.Entry<String,String> message:this.messageTargets.entrySet()){			
			List<Node> shape=queryBpmn.selectNodes("//bpmndi:BPMNEdge[@bpmnElement='"+message.getKey()+"']/di:waypoint");
			String values[]=message.getValue().split(";");
			((Element)shape.get(shape.size()-1)).addAttribute("x", values[0]);
			((Element)shape.get(shape.size()-1)).addAttribute("y", values[1]);
			if(this.messageColors.get(message.getKey())!=null){
				Node edge=queryBpmn.selectSingleNode("//bpmndi:BPMNEdge[@bpmnElement='"+message.getKey()+"']");
				((Element)edge).addAttribute("bioc:stroke", this.messageColors.get(message.getKey()));
				//((Element)edge).addAttribute("xmlns:color","http://www.omg.org/spec/BPMN/non-normative/color/1.0");
				((Element)edge).addAttribute("xmlns:bioc","http://bpmn.io/schema/bpmn/biocolor/1.0");
			}
		}

	}
	
	private void getMessageIds() throws JaxenException{
		List<Node> messages = queryBpmn.selectNodes("//bpmn:messageFlow"); 
		for(Node message:messages){
			String source=message.valueOf("@sourceRef");
			String target=message.valueOf("@targetRef");
			String id=message.valueOf("@id");
			
			Node sourceShape=queryFragment.selectSingleNode("//bpmndi:BPMNShape[@bpmnElement='"+source+"']/dc:Bounds");
			Node targetShape=queryFragment.selectSingleNode("//bpmndi:BPMNShape[@bpmnElement='"+target+"']/dc:Bounds");
			Node fragmentMessage=null;
			if(sourceShape!=null){
				String x=String.valueOf(Integer.parseInt(sourceShape.valueOf("@x"))+Integer.parseInt(sourceShape.valueOf("@width"))/2);
				String y=String.valueOf(Integer.parseInt(sourceShape.valueOf("@y"))+Integer.parseInt(sourceShape.valueOf("@height")));
				this.messageSources.put(id,x+";"+y);
				
				fragmentMessage=queryFragment.selectSingleNode("//bpmn:messageFlow[@sourceRef='"+source+"']");
				
			}
			if(targetShape!=null){
				String x=String.valueOf(Integer.parseInt(targetShape.valueOf("@x"))+Integer.parseInt(targetShape.valueOf("@width"))/2);
				String y=String.valueOf(Integer.parseInt(targetShape.valueOf("@y"))+Integer.parseInt(targetShape.valueOf("@height")));
				this.messageTargets.put(id,x+";"+y);
				
				fragmentMessage=queryFragment.selectSingleNode("//bpmn:messageFlow[@targetRef='"+target+"']");
			}	
			if(fragmentMessage!=null){
				Node edge=queryFragment.selectSingleNode("//bpmndi:BPMNEdge[@bpmnElement='"+fragmentMessage.valueOf("@id")+"']");
				try{
					String color=edge.valueOf("@bioc:stroke");	
					if(color!=null && color.trim().length()>0) this.messageColors.put(id, color);
				}catch(XPathException e){}// if @bioc:stroke an exeption is triggered
				fragmentMessage=null;
			}

		}
			
	}

	
}
