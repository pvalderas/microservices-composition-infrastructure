package es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter;

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jaxen.JaxenException;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.utils.XMLQuery;


public class Splitter {
	private Document bpmn;	
	private XMLQuery query;
	
	public Splitter(String bpmn) throws DocumentException{
		this.bpmn = DocumentHelper.parseText(bpmn);
		query=new XMLQuery(this.bpmn);
	}

	public List<BPMNFragment> split() throws DocumentException, JaxenException{
		List<BPMNFragment> fragments=new ArrayList<BPMNFragment>();
		
		Element root=this.bpmn.getRootElement();
		String composition=root.valueOf("@id");
		
		List<Node> pools = this.selectPools(); 
		for(Node poolParticipant:pools){
			BPMNFragmentBuilder fragmentBd=new BPMNFragmentBuilder(composition, poolParticipant, query);
			BPMNFragment fragment=createBPMNFragment(composition,fragmentBd);
			fragments.add(fragment);
		}
	
		return fragments;
	}
	
	private List<Node> selectPools() throws JaxenException{
		List<Node> pools = query.selectNodes("//bpmn:participant"); 
		return pools;
	}
	
	private BPMNFragment createBPMNFragment(String composition, BPMNFragmentBuilder fragmentBd){
		BPMNFragment fragment=new BPMNFragment();
		fragment.setMicroservice(fragmentBd.getMicroserviceId());
		fragment.setXml(fragmentBd.asXML().toString());
		fragment.setComposition(composition);
		fragment.setId(fragmentBd.getFragmentId());
		return fragment;
	}
}
