package es.upv.pros.pvalderas.composition.bpmn.utils;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.dom4j.Dom4jXPath;

public class XMLQuery {
	
	private NameSpaceManager nsm;
	private Document bpmn;
	
	public XMLQuery(Document bpmn){
		this.bpmn=bpmn;
		nsm=NameSpaceManager.getCurrentInstance();
	}
	

	public List<Node> selectNodes(String xpath, Object element) throws JaxenException{
		Dom4jXPath query = new Dom4jXPath(xpath);
		query.setNamespaceContext( new SimpleNamespaceContext(nsm.getAllUris()));
		List<Node> nodes = query.selectNodes(element); 
		return nodes;
	}
	
	public List<Node> selectNodes(String xpath) throws JaxenException{
		return selectNodes(xpath, bpmn);
	}
	
	public Node selectSingleNode(String xpath, Object element) throws JaxenException{
		Dom4jXPath query = new Dom4jXPath(xpath);
		query.setNamespaceContext( new SimpleNamespaceContext(nsm.getAllUris()));
		Node node = (Node)query.selectSingleNode(element); 
		return node;
	}
	
	public Node selectSingleNode(String xpath) throws JaxenException{
		return selectSingleNode(xpath, bpmn);
	}
	
}
