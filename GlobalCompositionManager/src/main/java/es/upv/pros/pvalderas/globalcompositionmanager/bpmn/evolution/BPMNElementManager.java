package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.BaseElement;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.Text;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.stereotype.Component;

@Component
public class BPMNElementManager {

	
	public void attachTextAnnotationToParticipant(BpmnModelInstance model, Participant participant, String textContent, String color){
		ModelElementInstance collaboration=model.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		
		TextAnnotation textAnnotation=model.newInstance(TextAnnotation.class);
		Text text=model.newInstance(Text.class);
		Association asoc=model.newInstance(Association.class);
		
		text.setTextContent(textContent);
		textAnnotation.setText(text);

		collaboration.addChildElement(textAnnotation);
		collaboration.addChildElement(asoc);
		
		asoc.setSource(participant);
		asoc.setTarget(textAnnotation);
		
		BpmnDiagram diagram=this.getDiagram(model, collaboration.getAttributeValue("id"));
		BpmnShape poolShape=this.getshape(diagram, participant.getId());
		
		double x=poolShape.getBounds().getX();//+poolShape.getBounds().getWidth();
		double y=poolShape.getBounds().getY()+poolShape.getBounds().getHeight()/2;
		
		BpmnShape shapeAnnotation=createShape(model, x-200, y-15, 130, 30, color);
		shapeAnnotation.setBpmnElement(textAnnotation);
		
		BpmnEdge edgeAsoc=createEdge(model,x,y,x-70,y, color, false);
		edgeAsoc.setBpmnElement(asoc);
		
		diagram.getBpmnPlane().addChildElement(shapeAnnotation);
		diagram.getBpmnPlane().addChildElement(edgeAsoc);
		
	}
	
	public void addSequenceFlow(BpmnModelInstance model, Process process, FlowNode source, FlowNode target, String color){
		SequenceFlow sequenceFlow=model.newInstance(SequenceFlow.class);
		sequenceFlow.setSource(source);
		sequenceFlow.setTarget(target);
		
		process.addChildElement(sequenceFlow);
		
		addEdgeFlow(model, sequenceFlow, source, target, color);
	}
	
	public void addMessageFlow(BpmnModelInstance model, Event source, Event target, String color){
		MessageFlow messageFlow=model.newInstance(MessageFlow.class);
		messageFlow.setSource(source);
		messageFlow.setTarget(target);
		
		ModelElementInstance collaboration=model.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		collaboration.addChildElement(messageFlow);
		
		addEdgeFlow(model, messageFlow, source, target, color);
	}
	
	private void addEdgeFlow(BpmnModelInstance model, BaseElement flow, BaseElement source, BaseElement target, String color){
		ModelElementInstance collaboration=model.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		BpmnDiagram diagram=this.getDiagram(model, collaboration.getAttributeValue("id"));
		
		BpmnShape sourceShape=this.getshape(diagram, source.getId());
		BpmnShape targetShape=this.getshape(diagram, target.getId());
		
		double sourceX=sourceShape.getBounds().getX();
		double sourceY=sourceShape.getBounds().getY();
		double sourceWidth=sourceShape.getBounds().getWidth();
		double sourceHeight=sourceShape.getBounds().getHeight();
		
		double targetX=targetShape.getBounds().getX();
		double targetY=targetShape.getBounds().getY();
		double targetWidth=targetShape.getBounds().getWidth();
		double targetHeight=targetShape.getBounds().getHeight();
		
		BpmnEdge edge=null;
		if(sourceX>targetX){
			edge=createEdge(model,sourceX, sourceY+sourceHeight/2, targetX+targetWidth, targetY+targetHeight/2, color, true);
		}else{
			edge=createEdge(model,sourceX+sourceWidth, sourceY+sourceHeight/2, targetX, targetY+targetHeight/2, color, true);
		}
		edge.setBpmnElement(flow);
		diagram.getBpmnPlane().addChildElement(edge);
	}
	
	
	
	private BpmnDiagram getDiagram(BpmnModelInstance model, String id){
		BpmnDiagram diagram=null;
		for(BpmnDiagram d:model.getDefinitions().getBpmDiagrams()){
			if(d.getBpmnPlane().getBpmnElement().getId().equals(id)) diagram=d;
		}
		return diagram;
	}
	
	private BpmnShape getshape(BpmnDiagram diagram, String id){
		BpmnShape shape=null;
		for(BpmnShape p:diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class)){
			if(p.getAttributeValue("bpmnElement").equals(id)) shape=p;
		}
		return shape;
	}
	
	private BpmnEdge createEdge(BpmnModelInstance model, double sourceX, double sourceY, double targetX, double targetY, String color, boolean corner){
		BpmnEdge edge=model.newInstance(BpmnEdge.class);
		System.out.println(edge.getId()+"-->"+color);
		if(color!=null) edge.setAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke",color);

		if(corner){
			Waypoint edgeWaypoint1=model.newInstance(Waypoint.class);
			edgeWaypoint1.setX(sourceX);
			edgeWaypoint1.setY(sourceY);
			Waypoint edgeWaypoint2=model.newInstance(Waypoint.class);
			edgeWaypoint2.setX(sourceX);
			edgeWaypoint2.setY(targetY);
			Waypoint edgeWaypoint3=model.newInstance(Waypoint.class);
			edgeWaypoint3.setX(targetX);
			edgeWaypoint3.setY(targetY);
			edge.addChildElement(edgeWaypoint1);
			edge.addChildElement(edgeWaypoint2);
			edge.addChildElement(edgeWaypoint3);
		}else{
			Waypoint edgeWaypoint1=model.newInstance(Waypoint.class);
			edgeWaypoint1.setX(sourceX);
			edgeWaypoint1.setY(sourceY);
			Waypoint edgeWaypoint2=model.newInstance(Waypoint.class);
			edgeWaypoint2.setX(targetX);
			edgeWaypoint2.setY(targetY);
			edge.addChildElement(edgeWaypoint1);
			edge.addChildElement(edgeWaypoint2);
		}

		return edge;
	}
	
	private BpmnShape createShape(BpmnModelInstance model, double x, double y, double width, double height, String color){
		BpmnShape shape=model.newInstance(BpmnShape.class);
		
		shape.setAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke",color);
		Bounds shapeBounds=model.newInstance(Bounds.class);
		shapeBounds.setX(x);
		shapeBounds.setY(y);
		shapeBounds.setWidth(width);
		shapeBounds.setHeight(height);
		shape.setBounds(shapeBounds);
		
		return shape;
	}
	
	public void drawElementInColor(BpmnShape shape, String color){
		shape.setAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke",color);
	}
	
	public void drawElementInColor(BpmnEdge edge, String color){
		edge.setAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke",color);
	}
	
	public void deleteElement(BpmnModelInstance model, String processId, ModelElementInstance element){
		deleteShape(element.getAttributeValue("id"),model);
		Process process=(Process)model.getModelElementById(processId);
		process.removeChildElement(element);
	}
	
	public void deleteFlow(BpmnModelInstance model, String processId, ModelElementInstance element){
		deleteEdge(element.getAttributeValue("id"), model);
		Process process=(Process)model.getModelElementById(processId);
		process.removeChildElement(element);
	}
	
	private void deleteShape(String bpmnElement,BpmnModelInstance model){
		ModelElementInstance collaboration=model.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		BpmnDiagram diagram=this.getDiagram(model, collaboration.getAttributeValue("id"));
		
		for(BpmnShape shape:diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class)){
			if(shape.getAttributeValue("bpmnElement").equals(bpmnElement)) diagram.getBpmnPlane().removeChildElement(shape);
		}
	}
	
	private void deleteEdge(String bpmnElement,BpmnModelInstance model){
		ModelElementInstance collaboration=model.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		BpmnDiagram diagram=this.getDiagram(model, collaboration.getAttributeValue("id"));
		
		for(BpmnEdge edge:diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class)){
			if(edge.getBpmnElement()!=null && edge.getBpmnElement().getId().equals(bpmnElement)) diagram.getBpmnPlane().removeChildElement(edge);
		}
	}
}
