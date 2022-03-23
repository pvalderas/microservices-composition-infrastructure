package es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.impl.instance.FlowNodeRef;
import org.camunda.bpm.model.bpmn.instance.CatchEvent;
import org.camunda.bpm.model.bpmn.instance.Collaboration;
import org.camunda.bpm.model.bpmn.instance.ConditionExpression;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.Event;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.IntermediateThrowEvent;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.LaneSet;
import org.camunda.bpm.model.bpmn.instance.Message;
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.ParallelGateway;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaExecutionListener;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaField;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import es.upv.pros.pvalderas.composition.bpmn.utils.CollapsedPools;

public class BPMNFragmentBuilderFromLane implements BPMNFragmentBuilder{
	private String compositionName;
	
	private BpmnModelInstance model;
	private Lane lane;
	private List<ModelElementInstance> microserviceElements;
	private ModelElementInstance collaboration;
	private Process fragmentProcess;
	private Process modelProcess;
	private BpmnDiagram diagram;
	private String processId;
	private Participant physicalParticipant;
	private Participant microserviceParticipant;
	
	private BpmnModelInstance fragment;
	
	
	public BPMNFragmentBuilderFromLane(String compositionName,BpmnModelInstance model, Lane lane){
		this.compositionName=compositionName;
		this.model=model;
		this.lane=lane;
		this.fragment=model.clone();
		this.collaboration=fragment.getDefinitions().getUniqueChildElementByType(Collaboration.class);
		this.modelProcess=(Process)this.lane.getParentElement().getParentElement();
		this.processId=this.modelProcess.getAttributeValue("id");
		this.fragmentProcess=(Process)fragment.getModelElementById(processId);
		this.fragmentProcess.setAttributeValue("isExecutable", "true");
		//this.modelProcess=model.getModelElementById(processId);
		this.diagram=getDiagram();
		createBPMNFragment();

	}
	
	private void createBPMNFragment(){
		
		microserviceElements=new ArrayList<ModelElementInstance>();
		lane.getChildElementsByType(FlowNodeRef.class).forEach(element->{
			microserviceElements.add(fragment.getModelElementById(element.getRawTextContent()));
		});
		
	
		
		changePoolNames();
		System.out.println("Change Pool Names done");
		System.out.println(this.microserviceParticipant.getName());
		deleteOrAdaptSequenceFlows();
		System.out.println("Delete or Adapt Sequence Flows done");
		deleteBpmnElements();
		System.out.println("Delete BPMN elements done");
		deleteMessages();	
		System.out.println("Delete Messages Flows done");
		
		adaptPoolBoundsAndAddMessage();
		System.out.println("Add Pool Bounds and Messages done");
		addConnectingSequenceFlows();
		System.out.println("Add Connecting Sequence Flows done");
		addServiceTasksDelegateExpressions();
		System.out.println("Add Delegate Expressions done");
	}

/**************** ADAPT POOL NAME  **************/
	private void changePoolNames(){
		// Change the name of the Participant
		for(Participant p:collaboration.getChildElementsByType(Participant.class)){
			if(p.getAttributeValue("processRef")!=null && p.getAttributeValue("processRef").equals(processId)){
				p.setName(lane.getName());
				microserviceParticipant=p;
				String newProcessId=this.compositionName+"_"+lane.getName()+"_Process";
				newProcessId=newProcessId.replaceAll(" ", "");
				p.getProcess().setId(newProcessId);
				p.setAttributeValue("processRef", newProcessId);
				
			}
			if(p.getAttributeValue("name").equalsIgnoreCase(CollapsedPools.IOTPhysicalWorld)){
				p.setName(CollapsedPools.EVENTBUS);
				physicalParticipant=p;
			}
		}
		
	}
/***********************************************/	
	

/***************** DELETE MESSAGES *****************/
	private void deleteMessages(){
		//Delete Messages from Collaboration
		for(MessageFlow messageFlow:collaboration.getChildElementsByType(MessageFlow.class)){
			ModelElementInstance target=fragment.getModelElementById(messageFlow.getAttributeValue("targetRef"));
			ModelElementInstance source=fragment.getModelElementById(messageFlow.getAttributeValue("sourceRef"));
			
			if(target==null || (!microserviceElements.contains(target) && !microserviceElements.contains(source))){
				diagram.getBpmnPlane().removeChildElement(messageFlow.getDiagramElement());
				collaboration.removeChildElement(messageFlow);
			}
		}
	}
/***************************************************/	
	


/********* DELETE OR ADAPT SEQUENCE FLOWS *********/
	private void deleteOrAdaptSequenceFlows(){
		//Delete or adapt sequenceFlows that are not included in the lane of the Fragment Process
		
		List<SequenceFlow> sourceEventstoAdd=new ArrayList<SequenceFlow>();
		List<SequenceFlow> targetEventstoAdd=new ArrayList<SequenceFlow>();
		for(SequenceFlow el:modelProcess.getChildElementsByType(SequenceFlow.class)){
			SequenceFlow sequenceFlow=(SequenceFlow)fragment.getModelElementById(el.getId());
			if(!microserviceElements.contains(sequenceFlow.getSource()) && !microserviceElements.contains(sequenceFlow.getTarget())){
				deleteFlow(sequenceFlow);
			}else{
				if(!microserviceElements.contains(sequenceFlow.getSource())){
					if(!(sequenceFlow.getTarget() instanceof IntermediateCatchEvent) && !(sequenceFlow.getSource() instanceof IntermediateCatchEvent))
						sourceEventstoAdd.add(sequenceFlow);
					else if(sequenceFlow.getSource() instanceof IntermediateCatchEvent){
						microserviceElements.add(sequenceFlow.getSource());
					}
					else{
						deleteFlow(sequenceFlow);
					}		
				}else if(!microserviceElements.contains(sequenceFlow.getTarget())){
					if(!(sequenceFlow.getTarget() instanceof IntermediateCatchEvent) && !(sequenceFlow.getSource() instanceof IntermediateCatchEvent))
						targetEventstoAdd.add(sequenceFlow);
					else{	
						if(sequenceFlow.getSource() instanceof IntermediateCatchEvent){
							microserviceElements.remove(sequenceFlow.getSource());
							
							//BORRO EL FLOW DE ANTES DEL EVENT PORQUE SE QUEDA CON TARGET NULL
							modelProcess.getChildElementsByType(SequenceFlow.class).forEach(flow->{
								if(flow.getTarget().getId().equals(sequenceFlow.getSource().getId())){
									deleteFlow(fragment.getModelElementById(flow.getId()));
								}
							});

						}
						deleteFlow(sequenceFlow);
					}	
				}
			}
		}
		

		checkParallelGatewaysForSource(sourceEventstoAdd);
		checkParallelGatewaysForTarget(targetEventstoAdd);
		cleanParallelGatewaysWithoutTargets(sourceEventstoAdd,targetEventstoAdd);
		
		addSourceEvents(sourceEventstoAdd);
		addTargetEvents(targetEventstoAdd);	
		
	}
	
	private void checkParallelGatewaysForTarget(List<SequenceFlow> targetEventstoAdd){
		List<ParallelGateway> gateWays=new ArrayList<ParallelGateway>();
		for(SequenceFlow flow:targetEventstoAdd){
			if(flow.getSource() instanceof ParallelGateway){
				if(!gateWays.contains(flow.getSource())){
					gateWays.add((ParallelGateway)model.getModelElementById(flow.getSource().getId()));
				}
			}
		}
		
		
		for(ParallelGateway gateWay:gateWays){
			boolean allInAnotherLane=true;
			for(SequenceFlow sequenceFlow: gateWay.getOutgoing()){
				//if(microserviceElements.contains(sequenceFlow.getTarget())){
				if(getLane(gateWay).equals(getLane(sequenceFlow.getTarget()))){
					 allInAnotherLane=false;
				}
			}	
			
			if(allInAnotherLane){
				boolean first=true;
				List<SequenceFlow> listOfFlows=new ArrayList<SequenceFlow>(targetEventstoAdd);
				for(int i=0;i<listOfFlows.size();i++){
					SequenceFlow flow=targetEventstoAdd.get(i);
					
					if(flow.getSource()!=null && flow.getSource().getId().equals(gateWay.getId()) && first){
						SequenceFlow previousFlow=gateWay.getIncoming().iterator().next();
						targetEventstoAdd.set(i, fragment.getModelElementById(previousFlow.getId()));
						deleteFlow(flow);
						first=false;
					}else if(flow.getSource()!=null && flow.getSource().getId().equals(gateWay.getId())){
						deleteFlow(flow);
						targetEventstoAdd.set(i, null);
					}
				}
				targetEventstoAdd.removeIf(seq -> (seq==null));
				microserviceElements.removeIf(element->(element.getAttributeValue("id").equals(gateWay.getId())));
				deleteElement(gateWay);
			}
		}
	}
	
	private void checkParallelGatewaysForSource(List<SequenceFlow> sourceEventstoAdd){
		
		for(SequenceFlow flow:sourceEventstoAdd){
			if(flow.getSource() instanceof ParallelGateway){
				ParallelGateway gateWay=(ParallelGateway)model.getModelElementById(flow.getSource().getId());

				boolean allInAnotherLane=true;
				for(SequenceFlow sequenceFlow: gateWay.getOutgoing()){
					if(getLane(gateWay).equals(getLane(sequenceFlow.getTarget()))){
						 allInAnotherLane=false;
					}
				}	
				
				System.out.println(gateWay.getId());
				System.out.println(gateWay.getOutgoing().size());
				System.out.println(gateWay.getIncoming().size());
				System.out.println(gateWay.getIncoming().iterator().next().getSource());
				
				if(allInAnotherLane)
					//flow.setSource(gateWay.getIncoming().iterator().next().getSource());
					flow.setSource(fragment.getModelElementById(gateWay.getIncoming().iterator().next().getSource().getId()));
				
			}
		}
	}
	
	private Lane getLane(FlowElement element){
		for(Lane lane:this.lane.getParentElement().getChildElementsByType(Lane.class)){
			for(FlowNodeRef flowNodeRef:lane.getChildElementsByType(FlowNodeRef.class)){
				if(flowNodeRef.getTextContent().equals(element.getId())) return lane;
			}
		}
		return null;
	}
	
	private void cleanParallelGatewaysWithoutTargets(List<SequenceFlow> sourceEventstoAdd,List<SequenceFlow> targetEventstoAdd){
		boolean clean;
		do{
			clean=true;
			for(ParallelGateway parallel:fragment.getModelElementsByType(ParallelGateway.class)){
				if(microserviceElements.contains(parallel) && parallel.getOutgoing().size()==0){
					for(SequenceFlow incoming:parallel.getIncoming()){
						sourceEventstoAdd.remove(incoming);
						targetEventstoAdd.remove(incoming);
						deleteFlow(incoming);
					}
					this.microserviceElements.remove(parallel);
					deleteElement(parallel);
					clean=false;
				}
			}
		}while(!clean);
	}
	
	
	private void addSourceEvents(List<SequenceFlow> sourceEventstoAdd){
		int startIndex=0;
		if(!this.isThereAnStartEvent()){
			addSourceEvent(sourceEventstoAdd.get(0), true);
			startIndex=1;
		}
		for(int i=startIndex;i<sourceEventstoAdd.size();i++) addSourceEvent(sourceEventstoAdd.get(i), false);
	}
	
	private void addTargetEvents(List<SequenceFlow> targetEventstoAdd){
		int endIndex=0;
		if(!this.isThereAnEndEvent()){
			addTargetEvent(targetEventstoAdd.get(targetEventstoAdd.size()-1), true);
			endIndex=1;
		}
		for(int i=0;i<targetEventstoAdd.size()-endIndex;i++) addTargetEvent(targetEventstoAdd.get(i), false);
	}
	
	
	private boolean isThereAnStartEvent(){
		
		for(ModelElementInstance element: this.microserviceElements){
			if(element instanceof StartEvent) return true;
		}
		
		return false;
	}
	
	private boolean isThereAnEndEvent(){
		
		for(ModelElementInstance element: this.microserviceElements){
			if(element instanceof EndEvent) return true;
		}
		
		return false;
	}
	
	private void addSourceEvent(SequenceFlow sequenceFlow, boolean isStartEvent){
		
		Event catchEvent;
		if(isStartEvent)
			catchEvent=fragment.newInstance(StartEvent.class);
		else
			catchEvent=fragment.newInstance(IntermediateCatchEvent.class);
		
		String eventName=getEventName(sequenceFlow);
		
		fragmentProcess.addChildElement(catchEvent);
		sequenceFlow.setSource(catchEvent);
		
		BpmnShape targetShape=(BpmnShape)sequenceFlow.getTarget().getDiagramElement();
		
		double x=targetShape.getBounds().getX()-100;
		double y=targetShape.getBounds().getY()+targetShape.getBounds().getHeight()/2-18; // 18 = Event Height (36)/2
		addEventShape(catchEvent, x, y);
		adaptSequenceFlowWayPoint(sequenceFlow,x,x+100,y+18,y+18);
		addMessageFlow(catchEvent, eventName);
		
		this.microserviceElements.add(catchEvent);
	}
	
	private Event addTargetEvent(SequenceFlow sequenceFlow, boolean isEndEvent){
		System.out.println(sequenceFlow.getParentElement().getAttributeValue("id")+"->"+fragmentProcess.getAttributeValue("id"));
		Event throwEvent;
		if(isEndEvent)
			throwEvent=fragment.newInstance(EndEvent.class);
		else
			throwEvent=fragment.newInstance(IntermediateThrowEvent.class);
		
		String eventName=getEventName(sequenceFlow);
		String microserviceName=getTargetMicroseriveName(sequenceFlow);

		MessageEventDefinition messageDefinition=fragment.newInstance(MessageEventDefinition.class);
		messageDefinition.setAttributeValueNs("http://camunda.org/schema/1.0/bpmn", "delegateExpression", "${eventSender}");
		ExtensionElements extensionElements=fragment.newInstance(ExtensionElements.class);
		CamundaField messageField=fragment.newInstance(CamundaField.class);
		messageField.setAttributeValue("name", "message");
		messageField.setAttributeValue("stringValue", this.compositionName+"_"+eventName.replaceAll(" ", "")+"Message");
		CamundaField microserviceField=fragment.newInstance(CamundaField.class);
		microserviceField.setAttributeValue("name", "microservice");
		microserviceField.setAttributeValue("stringValue", microserviceName); 
		extensionElements.addChildElement(messageField);
		extensionElements.addChildElement(microserviceField);
		messageDefinition.addChildElement(extensionElements);
		throwEvent.addChildElement(messageDefinition);
		fragmentProcess.addChildElement(throwEvent);
		sequenceFlow.setTarget(throwEvent);

		BpmnShape sourceShape=(BpmnShape)sequenceFlow.getSource().getDiagramElement();

		double x=sourceShape.getBounds().getX()+sourceShape.getBounds().getWidth()+100;
		double y=sourceShape.getBounds().getY()+sourceShape.getBounds().getHeight()/2-18; // 18 = Event Height (36)/2
		addEventShape(throwEvent, x, y);
		adaptSequenceFlowWayPoint(sequenceFlow,x-100,x,y+18,y+18);
		addMessageFlow(throwEvent, eventName);
		
		this.microserviceElements.add(throwEvent);
		return throwEvent;
	}
	
	private String getTargetMicroseriveName(SequenceFlow flow){
		if(flow.getTarget()==null) return "all";
		for(LaneSet ls:fragmentProcess.getLaneSets()){
			for(Lane lane:ls.getLanes()){
				for(ModelElementInstance element:lane.getChildElementsByType(FlowNodeRef.class)){
					if(element.getRawTextContent().equals(flow.getTarget().getId())) return lane.getName();
				}
			}
		}
		return "";
	}
	
	private void addMessageFlow(Event event, String messageName){
		MessageFlow message=fragment.newInstance(MessageFlow.class);
		BpmnEdge messageEdge=fragment.newInstance(BpmnEdge.class);
		message.setName(messageName);
		
		if(event instanceof CatchEvent || event instanceof StartEvent){
			message.setSource(this.physicalParticipant);
			message.setTarget(event);
		}else{
			message.setSource(event);
			message.setTarget(this.physicalParticipant);
		}

		collaboration.addChildElement(message);
		messageEdge.setBpmnElement(message);
		diagram.getBpmnPlane().addChildElement(messageEdge);
		
		//Edge WayPoints and MessageDefinitions are added when adapted Pool Bounds
	}
	
	private String getEventName(SequenceFlow sequenceFlow){
		String eventName=sequenceFlow.getSource().getName();
		if(eventName==null){
			eventName=sequenceFlow.getTarget().getName();
			if(eventName==null){
				FlowNode previous=sequenceFlow.getSource();
				while(eventName==null){
					previous=previous.getPreviousNodes().list().get(0);
					eventName=previous.getName();
				}
				eventName=eventName+" Executed";
			}else{
				eventName="Execute "+eventName;
			}
		}else{ 
			eventName=eventName+" Executed";
		}
		eventName.replaceAll("\\?", "");
		return eventName;
	}
	
	private void addEventShape(Event event, double x, double y){
		BpmnShape shape=fragment.newInstance(BpmnShape.class);
		Bounds bounds=fragment.newInstance(Bounds.class);
		bounds.setX(x);
		bounds.setY(y);
		bounds.setWidth(36);
		bounds.setHeight(36);
		shape.setBpmnElement(event);
		shape.addChildElement(bounds);
		diagram.getBpmnPlane().addChildElement(shape);
	}
	
	private void adaptSequenceFlowWayPoint(SequenceFlow sequenceFlow, double x1, double x2, double y1, double y2){
		sequenceFlow.getDiagramElement().getChildElementsByType(Waypoint.class).forEach(wayPoint->{
			sequenceFlow.getDiagramElement().removeChildElement(wayPoint);
		});
		
		Waypoint wayPoint1=fragment.newInstance(Waypoint.class);
		Waypoint wayPoint2=fragment.newInstance(Waypoint.class);
		wayPoint1.setX(x1);
		wayPoint1.setY(y1);
		wayPoint2.setX(x2);
		wayPoint2.setY(y2);
		
		sequenceFlow.getDiagramElement().addChildElement(wayPoint1);
		sequenceFlow.getDiagramElement().addChildElement(wayPoint2);
	}
	
/**************************************************/	
	

/************** DELETE BPMN ELEMENTS *************/
	private void deleteBpmnElements(){
		//Delete flow elements that are not included in the lane of the Fragment Process
		for(FlowElement el:modelProcess.getChildElementsByType(FlowElement.class)){
			ModelElementInstance element=fragment.getModelElementById(el.getId());
			if(element!=null && !(element instanceof SequenceFlow) && !microserviceElements.contains(element)){
				deleteElement(element);
			}
		}
		
		// Delete Lanes and LaneSets
		fragmentProcess.getLaneSets().forEach(ls->{
			ls.getLanes().forEach(lane->{
				diagram.getBpmnPlane().removeChildElement(lane.getDiagramElement());
			});
		});
		fragmentProcess.removeChildElement(fragment.getModelElementById(lane.getParentElement().getAttributeValue("id")));
	}
/**************************************************/		
	

/***************** ADAPT POOL BOUNDS AND ADD MESSAGES***************/
	private void adaptPoolBoundsAndAddMessage(){
		BpmnShape microserviceParticipant=(BpmnShape)this.microserviceParticipant.getDiagramElement();
		BpmnShape physicalParticipant=(BpmnShape)this.physicalParticipant.getDiagramElement();
		ModelElementInstance laneBounds=lane.getDiagramElement().getUniqueChildElementByType(Bounds.class);
		
		double oldY=Double.parseDouble(laneBounds.getAttributeValue("y"));
		double newY=Double.parseDouble(physicalParticipant.getBounds().getAttributeValue("y"))+200;
		
		Bounds bounds=fragment.newInstance(Bounds.class);
		bounds.setX(Double.parseDouble(microserviceParticipant.getBounds().getAttributeValue("x")));
		bounds.setY(newY);
		bounds.setHeight(Double.parseDouble(laneBounds.getAttributeValue("height")));
		bounds.setWidth(Double.parseDouble(laneBounds.getAttributeValue("width")));
		microserviceParticipant.setBounds(bounds);
		
		physicalParticipant.getBounds().setAttributeValue("x", microserviceParticipant.getBounds().getAttributeValue("x"));
		physicalParticipant.getBounds().setAttributeValue("width", laneBounds.getAttributeValue("width"));

		correctPoolElementPositions(oldY-newY, physicalParticipant.getId(), microserviceParticipant.getId());
		
	}
	
	private void correctPoolElementPositions(double distance, String microserviceId, String physicalId){
		diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class).forEach(shape->{
			if(!shape.getId().equals(microserviceId) && !shape.getId().equals(physicalId))
				shape.getBounds().setY(shape.getBounds().getY()-distance);
		});
		
		diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class).forEach(edge->{
				if(edge.getBpmnElement() instanceof MessageFlow){
					MessageFlow messageFlow=(MessageFlow)edge.getBpmnElement();
					if(edge.getWaypoints().size()>0) edge.getWaypoints().clear();
					addMessageEdgeWayPoints(edge);
					
					if(messageFlow.getTarget() instanceof CatchEvent)
						addMessageDefinition((MessageFlow)edge.getBpmnElement());
					
				}else{
					edge.getWaypoints().forEach(wayPoint->{
						wayPoint.setY(wayPoint.getY()-distance);
					});
				}
		});
	}
	
	private void addMessageDefinition(MessageFlow messageFlow){
			Message message=fragment.newInstance(Message.class);
			String messageName=messageFlow.getName().replaceAll(" ", "").replaceAll("\\?", "")+"Message";
			message.setId(messageName);
			message.setAttributeValue("name", this.compositionName+"_"+messageName);
			fragment.getDefinitions().insertElementAfter(message, collaboration);
			
			MessageEventDefinition messageDefinition=(MessageEventDefinition)messageFlow.getTarget().getUniqueChildElementByType(MessageEventDefinition.class);
			if(messageDefinition==null){
				messageDefinition=fragment.newInstance(MessageEventDefinition.class);
				messageFlow.getTarget().addChildElement(messageDefinition);
			}
			messageDefinition.setMessage(message);
	}
	
	private void addMessageEdgeWayPoints(BpmnEdge messageEdge){
		MessageFlow message=((MessageFlow)messageEdge.getBpmnElement());
		boolean isCatchEvent=message.getSource().getId().equals(this.physicalParticipant.getId());
		
		Bounds physicalBounds=((BpmnShape)this.physicalParticipant.getDiagramElement()).getBounds();
		Bounds eventBounds;

		Waypoint wayPoint1=fragment.newInstance(Waypoint.class);		
		Waypoint wayPoint2=fragment.newInstance(Waypoint.class);
	
		if(isCatchEvent){
			eventBounds=((BpmnShape)((Event)fragment.getModelElementById(message.getTarget().getId())).getDiagramElement()).getBounds();
			wayPoint1.setY(physicalBounds.getY()+physicalBounds.getHeight());
			wayPoint2.setY(eventBounds.getY());
		}else{
			eventBounds=((BpmnShape)((Event)fragment.getModelElementById(message.getSource().getId())).getDiagramElement()).getBounds();
			wayPoint1.setY(eventBounds.getY());
			wayPoint2.setY(physicalBounds.getY()+physicalBounds.getHeight());
		}

		wayPoint1.setX(eventBounds.getX()+eventBounds.getWidth()/2);
		wayPoint2.setX(eventBounds.getX()+eventBounds.getWidth()/2);
		
		messageEdge.addChildElement(wayPoint1);
		messageEdge.addChildElement(wayPoint2);
	
	}
	
/**************************************************/		
	

/***************** ADD CONNECTING SEQUENCE FLOWS ******************/	
	private void addConnectingSequenceFlows(){
		for(FlowNode element:fragmentProcess.getChildElementsByType(FlowNode.class)){
			if(!(element instanceof EndEvent)){
				List<SequenceFlow> outgoingFlow=getFlowsWithSourceIn(element, fragmentProcess);
				if(outgoingFlow.size()==0){
					FlowNode counterPart=model.getModelElementById(element.getId());
					if(counterPart==null){
						List<SequenceFlow> incomingFlow=getFlowsWithTargetIn(element, fragmentProcess);
						//counterPart=model.getModelElementById(incomingFlow.get(0).getSource().getId());
					
						System.out.println("START");
						FlowElement nextElement=nextSequenceFlowInFragment(model.getModelElementById(incomingFlow.get(0).getId()));
						System.out.println("END");
						
						if(nextElement instanceof EndEvent){
							EndEvent endMessageEvent=fragment.newInstance(EndEvent.class);
							MessageEventDefinition message=(MessageEventDefinition)element.getUniqueChildElementByType(MessageEventDefinition.class);
							endMessageEvent.addChildElement(message);
							fragmentProcess.addChildElement(endMessageEvent);
							this.collaboration.getChildElementsByType(MessageFlow.class).forEach(messageFlow->{
								if(messageFlow.getSource().equals(element)) messageFlow.setSource(endMessageEvent);
							});
							incomingFlow.forEach(sequenceFlow->{
								sequenceFlow.setTarget(endMessageEvent);
							});
							element.getDiagramElement().setAttributeValue("bpmnElement", endMessageEvent.getId());
							fragmentProcess.removeChildElement(element);
						}else{
							SequenceFlow nextFlow=(SequenceFlow)nextElement;
							//FlowNode nextElementInFragment=fragment.getModelElementById(nextFlow.getTarget().getId());

							fragmentProcess.getChildElementsByType(IntermediateCatchEvent.class).forEach(catchEvent->{
								MessageEventDefinition message=(MessageEventDefinition)catchEvent.getUniqueChildElementByType(MessageEventDefinition.class);
								System.out.println(message.getMessage().getId()+"=="+this.getEventName(nextFlow)+"Message");
								if(message.getMessage().getId().equals(this.getEventName(nextFlow).replaceAll(" ", "")+"Message")){
									this.createSequenceFlow(element, catchEvent);
								};
							});
						}
					}
				}
			}
		}
		
		
		for(IntermediateCatchEvent catchEvent:fragmentProcess.getChildElementsByType(IntermediateCatchEvent.class)){
			List<SequenceFlow> ingoingFlows=getFlowsWithTargetIn(catchEvent, fragmentProcess);
			if(ingoingFlows.size()==0){
				StartEvent startMessageEvent=fragment.newInstance(StartEvent.class);
				MessageEventDefinition message=(MessageEventDefinition)catchEvent.getUniqueChildElementByType(MessageEventDefinition.class);
				startMessageEvent.addChildElement(message);
				fragmentProcess.addChildElement(startMessageEvent);
				this.collaboration.getChildElementsByType(MessageFlow.class).forEach(messageFlow->{
					if(messageFlow.getTarget().equals(catchEvent)) messageFlow.setTarget(startMessageEvent);
				});
				List<SequenceFlow> outgoingFlows=getFlowsWithSourceIn(catchEvent, fragmentProcess);
				outgoingFlows.forEach(sequenceFlow->{
					sequenceFlow.setSource(startMessageEvent);
				});
				catchEvent.getDiagramElement().setAttributeValue("bpmnElement", startMessageEvent.getId());
				fragmentProcess.removeChildElement(catchEvent);
			}
		}
	}
	
	private FlowElement nextSequenceFlowInFragment(SequenceFlow flow){
			System.out.println(flow.getId());
			if(flow.getTarget() instanceof EndEvent) return flow.getTarget();
			else if(this.microserviceElements.contains(fragment.getModelElementById(flow.getTarget().getId()))) return flow;
			else return nextSequenceFlowInFragment(flow.getTarget().getOutgoing().iterator().next());

	}
	
	private FlowNode getTarget(FlowNode element){
		FlowNode target=null;
		List<SequenceFlow> counterPartOutgoingFlow=getFlowsWithSourceIn(element, modelProcess);
		for(SequenceFlow flow:counterPartOutgoingFlow){
			if(target==null){
				ModelElementInstance outgoingTarget=fragment.getModelElementById(flow.getTarget().getId());
				if(this.microserviceElements.contains(outgoingTarget)) target=(FlowNode)outgoingTarget;
				else target=getTarget(flow.getTarget());
			}
		}
		return target;
	}
	
	private List<SequenceFlow> getFlowsWithTargetIn(FlowElement  target, Process process){
		List<SequenceFlow> outgoingFlows=new ArrayList<SequenceFlow>();
		for(SequenceFlow flow:process.getChildElementsByType(SequenceFlow.class)){
			if(flow.getTarget()!=null && flow.getTarget().equals(target)) outgoingFlows.add(flow);
		}
		return outgoingFlows;
	}
	
	private List<SequenceFlow> getFlowsWithSourceIn(FlowNode source, Process process){
		List<SequenceFlow> incomingFlows=new ArrayList<SequenceFlow>();
		for(SequenceFlow flow:process.getChildElementsByType(SequenceFlow.class)){
			if(flow.getSource()!=null && flow.getSource().equals(source)) incomingFlows.add(flow);
		}
		return incomingFlows;
	}
	

/***************** ADD SERVICE TASKS DELEGATE EXPRESSIONS ******************/	

	private void addServiceTasksDelegateExpressions(){
		fragmentProcess.getChildElementsByType(ServiceTask.class).forEach(task->{
			task.setAttributeValueNs("http://camunda.org/schema/1.0/bpmn", "delegateExpression", "${serviceClass}");
		});
		
		
		fragmentProcess.getChildElementsByType(ExclusiveGateway.class).forEach(gateWay->{
			if(gateWay.getName()!=null){
				String property=gateWay.getName().substring(0, 1).toLowerCase()+gateWay.getName().substring(1).replaceAll(" ", "").replaceAll("\\?", "");
				
				ExtensionElements extensions=fragment.newInstance(ExtensionElements.class);
				CamundaExecutionListener listener=fragment.newInstance(CamundaExecutionListener.class);
				listener.setAttributeValueNs("http://camunda.org/schema/1.0/bpmn", "delegateExpression", "${conditionEvaluator}");
				listener.setAttributeValueNs("http://camunda.org/schema/1.0/bpmn", "event", "start");
				extensions.addChildElement(listener);
				gateWay.addChildElement(extensions);
				
				gateWay.getOutgoing().forEach(outFlow->{
					ConditionExpression condition=fragment.newInstance(ConditionExpression.class);
					condition.setType("bpmn:tFormalExpression");
					condition.setTextContent("#{"+property+"==\""+outFlow.getName()+"\"}");
					outFlow.addChildElement(condition);
				});
			}
		});
	}
	
/*************************************************************/		
	
/********************** OTHERS ******************/	
	private BpmnDiagram getDiagram(){
		for(BpmnDiagram diagram:fragment.getDefinitions().getBpmDiagrams()){
			if(diagram.getBpmnPlane().getAttributeValue("bpmnElement").equals(collaboration.getAttributeValue("id"))){
				return diagram;
			}
		}
		return null;
	}
	
	private void deleteElement(ModelElementInstance element){
		if(fragment.getModelElementById(element.getAttributeValue("id"))!=null){
			deleteShape(element.getAttributeValue("id"));
			fragmentProcess.removeChildElement(fragment.getModelElementById(element.getAttributeValue("id")));
		}
	}
	
	private void deleteFlow(ModelElementInstance element){
		if(fragment.getModelElementById(element.getAttributeValue("id"))!=null){
			deleteEdge(element.getAttributeValue("id"));
			fragmentProcess.removeChildElement(fragment.getModelElementById(element.getAttributeValue("id")));
		}
	}
	
	private void deleteShape(String bpmnElement){
		for(BpmnShape shape:diagram.getBpmnPlane().getChildElementsByType(BpmnShape.class)){
			if(shape.getAttributeValue("bpmnElement").equals(bpmnElement)) diagram.getBpmnPlane().removeChildElement(shape);
		}
	}
	
	private void deleteEdge(String bpmnElement){
		for(BpmnEdge edge:diagram.getBpmnPlane().getChildElementsByType(BpmnEdge.class)){
			if(edge.getBpmnElement()!=null && edge.getBpmnElement().getId().equals(bpmnElement)) diagram.getBpmnPlane().removeChildElement(edge);
		}
	}
	
	private SequenceFlow createSequenceFlow(FlowNode source, FlowNode target){
		SequenceFlow sequenceFlow=fragment.newInstance(SequenceFlow.class);
		sequenceFlow.setSource(source);
		sequenceFlow.setTarget(target);
		this.fragmentProcess.addChildElement(sequenceFlow);
		
		BpmnShape targetShape=(BpmnShape)target.getDiagramElement();
		BpmnShape sourceShape=(BpmnShape)source.getDiagramElement();
		
		BpmnEdge edge=fragment.newInstance(BpmnEdge.class);
		edge.setBpmnElement(sequenceFlow);
		Waypoint wayPoint1=fragment.newInstance(Waypoint.class);
		Waypoint wayPoint2=fragment.newInstance(Waypoint.class);
		wayPoint1.setX(sourceShape.getBounds().getX()+sourceShape.getBounds().getWidth());
		wayPoint1.setY(sourceShape.getBounds().getY()+sourceShape.getBounds().getHeight()/2);
		wayPoint2.setX(targetShape.getBounds().getX());
		wayPoint2.setY(targetShape.getBounds().getY()+targetShape.getBounds().getHeight()/2);
		edge.addChildElement(wayPoint1);
		edge.addChildElement(wayPoint2);
		
		diagram.getBpmnPlane().addChildElement(edge);
		
		return sequenceFlow;
	}
/**************************************************/		
	

	
/*****************BPMN FRAGMENT BUILDER INTERFACE *********************/	
	public String asXML(){
		/*fragment.getDefinitions().getChildElementsByType(Process.class).forEach(p->{
			p.getChildElementsByType(SequenceFlow.class).forEach(flow->{
				System.out.println(flow.getId()+":"+flow.getSource()+"->"+flow.getTarget());
			});
		});*/
	
		return Bpmn.convertToString(fragment);
	}
	
	public String getMicroserviceId(){
		return lane.getAttributeValue("name");
	}
	
	public String getFragmentId(){
		return compositionName.replaceAll(" ", "")+"_"+lane.getAttributeValue("name").replaceAll(" ", "")+"_fragment";
	}
/**************************************************/		
}
