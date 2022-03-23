package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.jaxen.JaxenException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNElement;
import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.composition.bpmn.utils.XMLQuery;
import es.upv.pros.pvalderas.composition.http.HTTPClient;
import es.upv.pros.pvalderas.compositioncoordinator.dao.DAOFragments;
import es.upv.pros.pvalderas.compositioncoordinator.events.EventSender;

/*@Component
public class BPMNManager {
	private final String CHANGE_NAME="changeName";
	private final String DELETE_ELEMENT="deleteElement";
	private final String ADD_ELEMENT="addElement";
	private final String CHANGE_TYPE="changeType";
	
	private Document bpmnDoc;
	private XMLQuery query;
	
	private String compoDir;
	private String compoFile;
	private String composition;
	
	@Autowired
	private CamundaEngine camunda;
	
    @Autowired
    private EventSender eventSender;
    
    @Autowired
    private DAO dao;
	
	public void loadComposition(String composition){
		try {
			this.composition=composition;
			this.bpmnDoc=DocumentHelper.parseText(this.getCompoXML(composition));
			query=new XMLQuery(bpmnDoc);
		} catch (DocumentException e) {
			e.printStackTrace();
		}
	}
	
	public void manageChanges(JSONObject message){
		ArrayList<Object[]> affectingChanges=new ArrayList<Object[]>();
		
		try {
			JSONArray changes=message.getJSONArray("changes");
			
			//Getting the changes that affect this microservice
			for(BPMNElement e:this.getCoordinationElements()){
				for(int i=0;i<changes.length();i++){
					if(changes.getJSONObject(i).getString("name").equals(e.getName())){
						Object change[]={changes.getJSONObject(i),e};
						affectingChanges.add(change);
					}
				}
			}
			
			Properties props=getProps();
			String microservice=props.getProperty("spring.application.name");
			
			if(affectingChanges.size()>0){
				String globalcompositionManagerURL=props.getProperty("composition.globalcompositionmanager.url");
				
				JSONArray affectingChangesJSON=new JSONArray();
				for(Object[] c: affectingChanges){
					JSONObject change=(JSONObject)c[0];
					affectingChangesJSON.put(change);
				}

				JSONObject adaptationRequest=new JSONObject();
				adaptationRequest.put("changeId", message.getInt("changeId"));
				adaptationRequest.put("changes", affectingChangesJSON);
				adaptationRequest.put("composition", message.getString("composition"));
				adaptationRequest.put("modifiedMicroservice", message.getString("microservice"));
				adaptationRequest.put("toAdaptMicroservice", microservice);
				
				String adaptation=HTTPClient.post(globalcompositionManagerURL+"/evolution", adaptationRequest.toString(), true, "application/json");
				JSONObject adaptationJSON=new JSONObject(adaptation);
				switch(adaptationJSON.getInt("type")){
					case EvolutionProcess.GLOBAL_MODIFICATION:  sendReponse(message.getInt("changeId"), microservice, false); 
																break
																;
					case EvolutionProcess.AUTOMATIC: sendReponse(message.getInt("changeId"), microservice, true); 
													 dao.saveParticipantChange(message.getString("microservice"), 
															message.getInt("changeId"), 
															composition,
															
															affectingChangesJSON.toString(), 
															adaptationJSON.getString("adaptedFragment"),
															adaptationJSON.getInt("type"));
													 break;
													 
					case EvolutionProcess.AUTOMATIC_WITH_ACCEPTANCE: dao.saveParticipantChange(message.getString("microservice"), 
																								message.getInt("changeId"), 
																								composition, 
																								affectingChangesJSON.toString(), 
																								adaptationJSON.getString("adaptedFragment"),
																								adaptationJSON.getInt("type"));
																	  break;
				}
			}else{
				sendReponse(message.getInt("changeId"), microservice, true);
			}
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void sendReponse(Integer changeId, String microservice, boolean response) throws JSONException{
		JSONObject responseMessage=new JSONObject();
		responseMessage.put("changeId", changeId);
		responseMessage.put("microservice", microservice);
		responseMessage.put("composition", composition);
		responseMessage.put("response", response);
		//eventSender.responseToChange(responseMessage.toString(), composition);
	}
	
	private void updateFragment(String adaptedFragment) throws JaxenException, JSONException, IOException{

		this.saveCompoXML(adaptedFragment);
		
	}
	
	private ArrayList<BPMNElement> getCoordinationElements(){
		ArrayList<BPMNElement> coordinationElements=new ArrayList<BPMNElement>();
		
		try {
			List<Node> elements = query.selectNodes("(//bpmn:startEvent | //bpmn:endEvent | //bpmn:intermediateThrowEvent |bpmn:intermediateCatchEvent)");
			
			for(Node element:elements){
				BPMNElement e=new BPMNElement();
				e.setId(element.valueOf("@id"));
				e.setName(element.valueOf("@name"));
				e.setType(element.getName());
				coordinationElements.add(e);
			}
		} catch (JaxenException e1) {
			e1.printStackTrace();
		}
		
		return coordinationElements;
	}

	private String getCompoXML(String composition){
	    
	    File dir=new File("fragments");
	    String compositions[]=dir.list();
	    for(String compo:compositions){
	    	if(compo.indexOf(composition)>=0){
	    		compoDir=compo;
	    		File subDir=new File("fragments/"+compoDir);
	        	String files[]=subDir.list();	
	    		if(files!=null){
		        	for(String file:files){
			    		 if(file.indexOf(".")>0) compoFile=file;
		        	}
	    		}
		    }
	    }
	    String bpmn=null;
		try {
			bpmn = new String(Files.readAllBytes(new File("fragments/"+compoDir+"/"+compoFile).toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return bpmn;
	}
	
	private void saveCompoXML(String xml) throws FileNotFoundException, UnsupportedEncodingException{
		String fileName="fragments/"+compoDir+"/"+compoFile;
		File fichero=new File(fileName);
		PrintWriter writer = new PrintWriter(fichero, "UTF-8");
		writer.print(this.bpmnDoc.asXML());
		writer.close();
	}
	
	private Properties getProps(){
		YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
		yamlFactory.setResources(new ClassPathResource("application.yml"));
		Properties props = yamlFactory.getObject();
		return props;
	}
	
}*/
