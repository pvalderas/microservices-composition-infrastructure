package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Association;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.TextAnnotation;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.di.DiagramElement;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Component;

@Component
public class AdaptationConfirmer {

	private BpmnModelInstance model;
	
	public String adapt (String adaptedModelBPMN) throws DocumentException{
		model=Bpmn.readModelFromStream(new ByteArrayInputStream(adaptedModelBPMN.getBytes()));
		
		for(BpmnDiagram diagram:model.getDefinitions().getBpmDiagrams()){
			List<DiagramElement> toRemoveElements=new ArrayList<DiagramElement>();
			
			for(DiagramElement element:diagram.getBpmnPlane().getDiagramElements()){
				String color=element.getAttributeValueNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke");
			
				if(color!=null){
					if(color.equalsIgnoreCase(BPMNColors.RED)){
						toRemoveElements.add(element);
					}else{
						if(element instanceof BpmnEdge && ((BpmnEdge) element).getBpmnElement() instanceof MessageFlow){
								MessageFlow flow=(MessageFlow)((BpmnEdge) element).getBpmnElement();
	
								if((flow.getSource() instanceof StartEvent || flow.getSource() instanceof IntermediateCatchEvent)
										&&  (flow.getTarget() instanceof StartEvent || flow.getTarget() instanceof IntermediateCatchEvent))
												toRemoveElements.add(element);
						}
						element.removeAttributeNs("http://bpmn.io/schema/bpmn/biocolor/1.0","stroke");
					}
				}
	
				
				if(element instanceof BpmnShape && ((BpmnShape)element).getBpmnElement() instanceof TextAnnotation){
					TextAnnotation annotation=(TextAnnotation)((BpmnShape)element).getBpmnElement();
					if(annotation.getTextContent().equals(AdaptationProvider.ADAPTED_ANNOTATION)){
						toRemoveElements.add(element);
						annotation.getParentElement().getChildElementsByType(Association.class).forEach(asoc->{
							if(asoc.getTarget().getId().equals(annotation.getId())) toRemoveElements.add(asoc.getDiagramElement());
						});
					}
				}
			}
		
			for(DiagramElement element: toRemoveElements){
				
				ModelElementInstance bpmnElement=model.getModelElementById(element.getAttributeValue("bpmnElement"));
				
				bpmnElement.getParentElement().removeChildElement(bpmnElement);
				diagram.getBpmnPlane().removeChildElement(element);

			}
		}

		
		return Bpmn.convertToString(model);
		
	}
	

}
