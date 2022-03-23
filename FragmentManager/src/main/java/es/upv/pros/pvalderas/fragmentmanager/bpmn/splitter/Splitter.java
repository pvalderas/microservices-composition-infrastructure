package es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Lane;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jaxen.JaxenException;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.utils.CollapsedPools;
import es.upv.pros.pvalderas.composition.bpmn.utils.XMLQuery;


public class Splitter {
	private Document bpmn;	
	private XMLQuery query;
	private BpmnModelInstance model;
	
	public Splitter(String bpmn) throws DocumentException{
		this.bpmn = DocumentHelper.parseText(bpmn);
		query=new XMLQuery(this.bpmn);
	}

	public List<BPMNFragment> split() throws DocumentException, JaxenException{
		List<BPMNFragment> fragments=new ArrayList<BPMNFragment>();
		
		Element root=this.bpmn.getRootElement();
		String composition=root.valueOf("@id");
		
		if(isIoT()){
			model=Bpmn.readModelFromStream(new ByteArrayInputStream(bpmn.asXML().getBytes()));
			
			for(Process p:model.getModelElementsByType(Process.class)){
				p.getLaneSets().forEach(ls->{
					ls.getLanes().forEach(lane->{
						BPMNFragmentBuilderFromLane fragmentBd=new BPMNFragmentBuilderFromLane(composition, model, lane);
						BPMNFragment fragment=createBPMNFragment(composition,fragmentBd);
						fragments.add(fragment);
					});
				});
			}
			
		}else{
		
			List<Node> pools = this.selectPools(); 
			for(Node poolParticipant:pools){
				BPMNFragmentBuilderFromPools fragmentBd=new BPMNFragmentBuilderFromPools(composition, poolParticipant, query);
				BPMNFragment fragment=createBPMNFragment(composition,fragmentBd);
				fragments.add(fragment);
			}
			
		}
	
		return fragments;
	}
	
	public String split(String microservice) throws DocumentException, JaxenException{
		
		Element root=this.bpmn.getRootElement();
		String composition=root.valueOf("@id");
		
		List<Node> pools = this.selectPools(); 
		for(Node poolParticipant:pools){
			String name=poolParticipant.valueOf("@name");
			if(name.equalsIgnoreCase(microservice)){
				BPMNFragmentBuilderFromPools fragmentBd=new BPMNFragmentBuilderFromPools(composition, poolParticipant, query);
				return fragmentBd.asXML();
			}
		}
		
		return null;
	}
	
	private boolean isIoT() throws JaxenException{
		Node physical=query.selectSingleNode("//bpmn:participant[@name='PHYSICAL WORLD']"); 
		return physical!=null;
	}
	
	private List<Node> selectPools() throws JaxenException{
		List<Node> pools = query.selectNodes("//bpmn:participant"); 
		return pools;
	}
	
	private BPMNFragment createBPMNFragment(String composition, BPMNFragmentBuilder fragmentBd){
		BPMNFragment fragment=new BPMNFragment();
		fragment.setMicroservice(fragmentBd.getMicroserviceId());
		fragment.setXml(fragmentBd.asXML());
		fragment.setComposition(composition);
		fragment.setId(fragmentBd.getFragmentId());
		return fragment;
	}
}
