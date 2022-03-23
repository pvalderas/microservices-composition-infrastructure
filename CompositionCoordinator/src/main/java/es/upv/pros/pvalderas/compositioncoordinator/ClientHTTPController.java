package es.upv.pros.pvalderas.compositioncoordinator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.LocalChange;
import es.upv.pros.pvalderas.composition.bpmn.domain.ParticipantChange;
import es.upv.pros.pvalderas.composition.http.HTTPClient;
import es.upv.pros.pvalderas.compositioncoordinator.bpmn.CamundaEngine;
import es.upv.pros.pvalderas.compositioncoordinator.bpmn.FileManager;
import es.upv.pros.pvalderas.compositioncoordinator.dao.DAO;
import es.upv.pros.pvalderas.compositioncoordinator.events.EventManager;
import es.upv.pros.pvalderas.compositioncoordinator.events.EventSender;

@RestController
@CrossOrigin
public class ClientHTTPController {
	
	@Autowired
	private DAO dao;
	
	@Autowired
	private CamundaEngine camunda;
    
    @Autowired
    private EventManager eventManager;
    
    @Autowired
    private ResourcePatternResolver resourceLoader;
    
    @Autowired
    private EventSender eventSender;
    
    @Autowired
    private FileManager fileManager;
    
    @RequestMapping(
			  value = "/local/change", 
			  method = RequestMethod.POST,
			  consumes = "application/json",
			  produces = "application/json")
	public String saveLocalChanges(@RequestBody LocalChange change) throws JSONException, IOException  {
    	
    	Properties props=getProps();
		String fragmentManagerURL=props.getProperty("composition.fragmentmanager.url");
    	String response=HTTPClient.post(fragmentManagerURL+"/evolution", change.toJSON(), true, "application/json");
    	    	
		JSONObject responseJSON=new JSONObject(response);
		if(responseJSON.getInt("participants")!=-1 && responseJSON.getInt("responses")!=-1){
			Integer id=dao.getLocalChanges().saveLocalChanges(change.getFragment(), change.getComposition(), change.getXml(), change.getDirtyXml(), change.getDescription(), new Date().toString());
			change.setChangeId(id);
			dao.getLocalChanges().updatedAffectedByLocalChanges(id, responseJSON.getInt("participants"));
		}
		return response;
    }
    
    @RequestMapping(
			  value = "/local/change/{id}", 
			  method = RequestMethod.PUT)
	public String confirmLocalChanges(@PathVariable(value="id") Integer id) throws JSONException, IOException  {
			LocalChange change=dao.getLocalChanges().getLocalChanges(id);
			
			BPMNFragment fragment=new BPMNFragment();
			fragment.setComposition(change.getComposition());
			fragment.setId(change.getFragment());
			fragment.setMicroservice(change.getMicroservice());
			fragment.setXml(change.getXml());
			fragment.setNumParticipants(dao.getFragments().getCompositionParticipantsFromChangeId(change.getChangeId()));
			
			this.saveFragment(fragment);
			dao.getLocalChanges().deleteChange(change.getChangeId());
			//eventSender.confirmChange(change.getComposition(), true);
			
			return change.getXml();
  }
    
    @RequestMapping(
			  value = "/local/change/{id}", 
			  method = RequestMethod.DELETE)
	public String rejectLocalChanges(@PathVariable(value="id") Integer id) throws JSONException, IOException  {
			LocalChange change=dao.getLocalChanges().getLocalChanges(id);
			dao.getLocalChanges().deleteChange(change.getChangeId());
			//eventSender.confirmChange(change.getComposition(), false);
			
			
			return fileManager.getBPMNFileContent(change.getComposition());
			/*String fragmentDir=fileManager.getFragmentDir(change.getComposition());
		    if(fragmentDir!=null){
		    	final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/"+fragmentDir+"/*.bpmn");
		    	return new String(Files.readAllBytes(Paths.get(resources[0].getURI())));
		    }else{
		    	return "";
		    }*/
    }
    
    @RequestMapping(
			  value = "/local/change/debug/{id}", 
			  method = RequestMethod.DELETE)
	public void deleteLocalChanges(@PathVariable(value="id") Integer id) throws JSONException  {
	  	dao.getLocalChanges().deleteChange(id);
    }
    
    @RequestMapping(
			  value = "/local/changes", 
			  method = RequestMethod.DELETE)
	public void deleteAllLocalChanges() throws JSONException  {
    	dao.getLocalChanges().deleteAllChanges();
    } 
    
    @RequestMapping(
			  value = "/local/change/{id}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	public LocalChange getLocalChanges(@PathVariable(value="id") Integer id)  {
    	return dao.getLocalChanges().getLocalChanges(id);
    }
    
    @RequestMapping(
			  value = "/local/changes", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	public List<LocalChange> getAllLocalChanges()  {
    	return dao.getLocalChanges().getAllLocalChanges();
    }
    
    @RequestMapping(
			  value = "/local/change/{id}/responses", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	public String getLocalChangesResponses(@PathVariable(value="id") Integer id) throws JSONException  {
		  Integer responses=dao.getLocalChanges().getLocalChangeTrueResponses(id);
		  Integer participants=dao.getFragments().getCompositionParticipantsFromChangeId(id);
		  
		  JSONObject responsesJSON=new JSONObject();
		  responsesJSON.put("responses", responses);
		  responsesJSON.put("participants", participants);
		  
		  return responsesJSON.toString();
    }
  
    
    @RequestMapping(
			  value = "/participant/changes/unconfirmed/{composition}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	public List<ParticipantChange> getUnconfirmedParticipantChanges(@PathVariable(value="composition") String composition)  {
    	return dao.getParticipantChanges().getNonAccepted(composition);
    }
    
    @RequestMapping(
			  value = "/participant/changes/all/{composition}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	public String getAllParticipantChanges(@PathVariable(value="composition") String composition) throws JSONException  {
    	return dao.getParticipantChanges().getAllJSONByComposition(composition).toString();
  }
    
    @RequestMapping(
			  value = "/participant/change/reponse", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	public String responseParticipantChange(@RequestBody AdaptationResponse response) throws JSONException  {
    	eventSender.responseToChange(response);
    	if(response.getResponse()){ 
    		dao.getParticipantChanges().accept(response.getTo(), response.getChangeId()); 
    		return ""; // If accepted return nothing
    	}else{
    		dao.getParticipantChanges().reject(response.getTo(), response.getChangeId());
    		return dao.getFragments().getFragmentByComposition(response.getComposition()).getXml(); //If not accepted return non-modified bpmn
    	}
	 }
    
    
    @RequestMapping(
			  value = "/participant/change/{microservice}/{id}", 
			  method = RequestMethod.PUT)
	public String confirmParticipantChange(@PathVariable(value="microservice") String microservice, @PathVariable(value="id") Integer id) throws JSONException, IOException  {
			ParticipantChange change=dao.getParticipantChanges().get(microservice, id);
			
			BPMNFragment fragment=dao.getFragments().getFragmentByComposition(change.getComposition());
			fragment.setXml(change.getXmlToConfirm());
			
			this.saveFragment(fragment);
			dao.getParticipantChanges().delete(change.getMicroservice(), change.getId());
			
			return change.getXmlToConfirm();
}
    
  @RequestMapping(
			  value = "/participant/change/adapted/fragment/{microservice}/{changeId}", 
			  method = RequestMethod.GET)
    public String getAdaptedFragment(@PathVariable(value="microservice") String microservice, @PathVariable(value="changeId") Integer changeId) throws JSONException{
    	return dao.getParticipantChanges().getJSON(microservice, changeId).getString("dirtyXml").replaceAll("\\\"", "\"").replace("\\/","/");
    }
    
	@RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 public String saveBPMNFragment(@RequestBody BPMNFragment fragment) {
				
		try{
			saveFragment(fragment);
			eventManager.registerEventListener(fragment.getMicroservice(),fragment.getComposition());
			return "1";
		}catch(Exception e){
			return "0";
		}
	 }
	
	 @RequestMapping(
			  value = "/operations", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getOperations() {
		 return dao.getMicroservices().getOperations();
	 }
	 
	 @RequestMapping(
			  value = "/microservicename", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getName() {
		 return dao.getMicroservices().getMicroserviceName();
	 }
	 
	 @RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getFragments() {
		 return dao.getFragments().getFragments();
	 }
	 
	 @RequestMapping(
			  value = "/fragmentbpmn/{composition}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getFragmentBPMN(@PathVariable(value="composition") String composition) throws IOException, JSONException {
		 LocalChange change=dao.getLocalChanges().getOpenChangeByComposition(composition);
		 JSONObject fragment=new JSONObject();
		 if(change!=null){ // The fragment has dirty changes
			 fragment.put("changes", new JSONArray()); // changes = []
			 fragment.put("responses", dao.getLocalChanges().getLocalChangeTrueResponses(change.getChangeId()));
			 if(change.getAccepted()!=null && change.getAccepted()==1) fragment.put("globalAccepted", true);
			 fragment.put("participants", dao.getLocalChanges().getAffectedParticipantsByLocalChange(change.getChangeId()));
			 fragment.put("xml",change.getDirtyXml());
			 fragment.put("changeId",change.getChangeId());
			 fragment.put("accepted", change.getAccepted());
			 return fragment.toString();
		 }else{ 
			JSONArray participantChanges=dao.getParticipantChanges().getAllJSONByComposition(composition);
			fragment.put("changes", participantChanges); //If there are no unconfirmed changes of other participants -> participantChanges=[]
			fragment.put("xml",fileManager.getBPMNFileContent(composition));
		    /*String fragmentDir=fileManager.getFragmentDir(composition);
		    if(fragmentDir!=null){
		    	final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/"+fragmentDir+"/*.bpmn");
				fragment.put("xml", new String(Files.readAllBytes(Paths.get(resources[0].getURI()))));
			} else fragment.put("xml","");*/
			return fragment.toString();
		 }
	 }

	 
	 @RequestMapping(
			  value = "/fragmentmanagerurl", 
			  method = RequestMethod.GET)
	 public String getFragmentManagerURL() {
		 
		 Properties props=getProps();
		 return props.getProperty("composition.fragmentmanager.url");
		 
	 }
	 
	 
	 
	 
	 private void saveFragment(BPMNFragment fragment) throws IOException{
			String dirName=fileManager.getFragmentDir(fragment.getComposition());
			if(dirName==null) dirName="fragments/"+fragment.getComposition()+"-"+fragment.getNumParticipants();
			
			 File dir=new File(dirName);
			 dir.mkdir();
			 
			 String fileName=dirName+"/"+fragment.getId().toLowerCase()+".bpmn";
			 File fichero=new File(fileName);
			 PrintWriter writer = new PrintWriter(fichero, "UTF-8");
			 writer.print(fragment.getXml());
			 writer.close();
		
			 camunda.run();
			 
			 dao.getFragments().saveFragment(fragment, fileName);
	 }
	 
	 
	 private Properties getProps(){
		 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
		 yamlFactory.setResources(new ClassPathResource("application.yml"));
		 Properties props = yamlFactory.getObject();
		 return props;
	 }
}
