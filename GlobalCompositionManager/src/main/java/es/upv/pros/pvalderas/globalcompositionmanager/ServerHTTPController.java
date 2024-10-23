package es.upv.pros.pvalderas.globalcompositionmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dom4j.DocumentException;
import org.jaxen.JaxenException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptationResponse;
import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedModel;
import es.upv.pros.pvalderas.composition.bpmn.domain.AffectedParticipant;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNComposition;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.ChangeConfirmation;
import es.upv.pros.pvalderas.composition.bpmn.domain.EvolutionProcess;
import es.upv.pros.pvalderas.composition.bpmn.domain.LocalChange;
import es.upv.pros.pvalderas.composition.http.HTTPClient;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.AdaptationConfirmer;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.AdaptationProvider;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution.FileManager;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.joiner.Joiner;
import es.upv.pros.pvalderas.globalcompositionmanager.dao.DAO;
import es.upv.pros.pvalderas.globalcompositionmanager.events.EventSender;

@RestController
@CrossOrigin
public class ServerHTTPController {
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;
	 
	 @Autowired
	 private ResourcePatternResolver resourceLoader;
	 
	 @Autowired
	 private AdaptationProvider adaptationProvider;
	 
	 @Autowired
	 private FileManager fileManager;
	 
	 @Autowired
	 private DAO dao;
	 
	 @Autowired
	 private EventSender eventSender;
	 
	 @Autowired
	 private AdaptationConfirmer adaptationConfirmer;
	 
	 @RequestMapping(
			  value = "/dirty/compos", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getDirtyCompos() throws JSONException {
		 List<Map<String,Object>> compos=dao.getDirtyComposition().getAll();
		 JSONArray list=new JSONArray();
		 for(Map<String,Object> compo:compos){
			 JSONObject c=new JSONObject();
			 c.put("name", (String)compo.get("name"));
			 c.put("bpmn", compo.get("xml"));
			 c.put("affectedParticipants", compo.get("affectedParticipants"));
			 list.put(c);
		 }
		 return list.toString();
	 }
		
	 
	 @RequestMapping(
			  value = "/evolution", 
			  method = RequestMethod.POST,
			  consumes = "application/json",
			  produces = "application/json")
	 @Transactional
	 public String processChanges(@RequestBody LocalChange change) throws JSONException, IOException, DocumentException, JaxenException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		
		System.out.println("Local Change Received");
		
		AdaptedModel adaptedModel=adaptationProvider.getAdaptedModel(change);
		
		boolean allAutomatic=true;
		for(AffectedParticipant p:adaptedModel.getAffectedParticipants()){
			if(p.getAdaptationType()!=EvolutionProcess.AUTOMATIC) allAutomatic=false;
		}
		
		if(allAutomatic){
			
			String adaptedCompositionBPMN=adaptedModel.getBpmnToConfirm();
			String modifiedFragmentBPMN=change.getXml();
			Joiner joinerToConfirm=new Joiner(adaptedCompositionBPMN,modifiedFragmentBPMN);
			String newCompositionToConfirm=joinerToConfirm.join();
			
			saveComposition(change.getComposition(), change.getComposition(), newCompositionToConfirm);
			
			JSONObject response=new JSONObject();
		    response.put("participants",-1);
		    response.put("responses", -1);
		    
		    return response.toString();
			
		}else{
			
			String compositionBPMN=adaptedModel.getBpmn();
			String fragmentBPMN=change.getDirtyXml();
			Joiner joiner=new Joiner(compositionBPMN,fragmentBPMN);
			String newCompositionWithColors=joiner.join();
			
			String compositionBPMNToConfirm=adaptedModel.getBpmnToConfirm();
			String fragmentBPMNToConfirm=change.getXml();

			Joiner joinerToConfirm=new Joiner(compositionBPMNToConfirm,fragmentBPMNToConfirm);
			String newCompositionToConfirm=joinerToConfirm.join();
			
			dao.getDirtyComposition().save(change.getComposition(), newCompositionWithColors, newCompositionToConfirm, adaptedModel.getAffectedParticipantsJSON(), adaptedModel.getTotalParticipants(), adaptedModel.getModifiedParticipant());
			
			JSONObject response=new JSONObject();
		    response.put("participants", adaptedModel.getAffectedParticipants().size());
		    response.put("responses", 0);
			
			return response.toString();
		}

	 }
	 
	 @RequestMapping(
			  value = "/evolution/adaptation/{composition}", 
			  method = RequestMethod.POST,
			  consumes = "text/plain",
			  produces = "text/plain")
	 @Transactional
	 public String acceptAdaptation(@RequestBody String bpmn, @PathVariable(value="composition") String composition) throws JSONException, IOException, DocumentException, JaxenException {		 

		 AdaptedModel adaptedModel=dao.getDirtyComposition().getAdaptedModel(composition);
		 adaptedModel.setBpmn(bpmn);
		 
		 String confirmedBpmn=adaptationConfirmer.adapt(adaptedModel.getBpmn());
		 adaptedModel.setBpmnToConfirm(confirmedBpmn);
		 dao.getDirtyComposition().updateBPMNAndAccept(composition, bpmn, confirmedBpmn);
		 
		 Properties props=getProps();
		 String fragmentManagerURL=props.getProperty("composition.fragmentmanager.url");
		 HTTPClient.post(fragmentManagerURL+"/adaptation", adaptedModel.toJSON(), false, "application/json");
		 
		 
	     AdaptationResponse response=new AdaptationResponse();
	     response.setComposition(adaptedModel.getComposition());
	     response.setFrom("Global");
	     response.setTo(adaptedModel.getModifiedParticipant());
	     response.setResponse(true);
	     
	     eventSender.adaptationResponse(response);
		
	     return String.valueOf(adaptedModel.getAffectedParticipants().size());
	 }
	 
	 @RequestMapping(
			  value = "/evolution/adaptation/{composition}", 
			  method = RequestMethod.DELETE)
	 @Transactional
	 public void rejectAdaptation(@PathVariable(value="composition") String composition) throws JSONException, IOException, DocumentException, JaxenException {		 
		 AdaptedModel adaptedModel=dao.getDirtyComposition().getAdaptedModelWithouBPMN(composition);
		 dao.getDirtyComposition().delete(composition);
		 
	     AdaptationResponse response=new AdaptationResponse();
	     response.setComposition(composition);
	     response.setFrom("Global");
	     response.setTo(adaptedModel.getModifiedParticipant());
	     response.setResponse(false);
	     
	     eventSender.adaptationResponse(response);
			
	 }
	 
	 @RequestMapping(
			  value = "/evolution/adaptation/{composition}", 
			  method = RequestMethod.PUT,
			  produces = "text/plain" )
	 @Transactional
	 public String confirmAdaptation(@PathVariable(value="composition") String composition) throws JSONException, IOException, DocumentException, JaxenException {		 
		 AdaptedModel adaptedModel=dao.getDirtyComposition().getAdaptedModel(composition);
		 
		 dao.getDirtyComposition().delete(composition);
		 
		 String fileName=fileManager.saveCompositionFile(composition, adaptedModel.getBpmnToConfirm());
         dao.getComposition().save(composition, composition, fileName);
         
         ChangeConfirmation confirmation=new ChangeConfirmation();
         confirmation.setComposition(composition);
         confirmation.setModifiedMicroservice(adaptedModel.getModifiedParticipant());
         confirmation.setConfirmed(true);
         
         eventSender.confirmChange(confirmation);
	    
		 return adaptedModel.getBpmnToConfirm();
	 }
	
	 @RequestMapping(
			  value = "/compositions", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public void saveProcess(@RequestBody BPMNComposition composition) throws IOException, JSONException {
		 
		 System.out.println("BPMN Composition received!");
		 
		 saveComposition(composition.getId(), composition.getName(), composition.getXml());
         
		 System.out.println("Stored in the Composition Repository");
	 }
	 
	 @RequestMapping(
			  value = "/compositions", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getProcessess() {
		 return dao.getComposition().getAll();
	 }
	 
	 @RequestMapping(
			  value = "/compositionbpmn/{id}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getCompositionBPMN(@PathVariable(value="id") String composition) throws IOException, JSONException {
		 AdaptedModel dirtyCompo=dao.getDirtyComposition().getAdaptedModel(composition);
		 
		 final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + File.separator+"compositions"+File.separator+composition+".bpmn");
		 String bpmn;
		 try{
			 bpmn=new String(Files.readAllBytes(Paths.get(resources[0].getURI())));
		 }catch(Exception e){
			 bpmn=new String(Files.readAllBytes(new File(System.getProperty("user.dir") + File.separator+"compositions"+File.separator+composition+".bpmn").toPath()));
		 }

		 JSONObject compositionJSON=new JSONObject();
		 if(dirtyCompo!=null){
			 compositionJSON.put("confirmed", dirtyCompo.getConfirmed());
			 compositionJSON.put("dirty", true);
			 if(dirtyCompo.getConfirmed()<0 || dirtyCompo.getConfirmed()==1){
				 compositionJSON.put("dirtyBpmn", dirtyCompo.getBpmn());
				 compositionJSON.put("bpmn", bpmn);
				 if(dirtyCompo.getConfirmed()==1){
					 compositionJSON.put("participants", dirtyCompo.getAffectedParticipants().size());
					 compositionJSON.put("responses", dirtyCompo.getResponses());
				}/*else if(dirtyCompo.getConfirmed()==-2){
						 compositionJSON.put("participants", dirtyCompo.getAffectedParticipants().size());
						 compositionJSON.put("responses", -2);
				 }*/
			 }else{
				 if(dirtyCompo.getConfirmed()==2){
					 compositionJSON.put("dirtyBpmn", dirtyCompo.getBpmn());
					 compositionJSON.put("bpmn", dirtyCompo.getBpmnToConfirm());
					 dao.getDirtyComposition().delete(composition);
				 }else{
					compositionJSON.put("bpmn", bpmn);
				 }
			 }
		 }else{
			compositionJSON.put("dirty", false);
			compositionJSON.put("bpmn", bpmn);
		 }
		 return compositionJSON.toString();
	 }
	 

	 @RequestMapping(
			  value = "/compositions/{id}", 
			  method = RequestMethod.GET)
	 public Object getProcess(@PathVariable(value="id") String id) {
		 return dao.getComposition().get(id);
	 }
	 

	 @RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public String joinFragment(@RequestBody BPMNFragment fragment) throws DocumentException, JaxenException, IOException {
		 
		 try{
			 System.out.println("BPMN Fragment received!");
			
			 String compositionBPMN=fileManager.getCompositionFromFile(dao.getComposition().getFile(fragment.getComposition()));
			 String fragmentBPMN=fragment.getXml();
			 
			 Joiner joiner=new Joiner(compositionBPMN,fragmentBPMN);
			 String newComposition=joiner.join();
			
			 fileManager.saveCompositionFile(fragment.getComposition(), newComposition);
			 
			 System.out.println("Composition updated.");
			 return "1";
		 }catch(Exception e){
			 return "0";
		 }
	 }
	 
	 @RequestMapping(
			  value = "/serviceregistry", 
			  method = RequestMethod.GET)
	 public String getServiceRegistry(){
		 
         Properties props = this.getProps();
         
         String serviceRegistryType=props.getProperty("composition.serviceregistry.type");
         String serviceRegistryURL=props.getProperty("composition.serviceregistry.url");
         
		 return "{\"type\":\""+serviceRegistryType+"\",\"url\":\""+serviceRegistryURL+"\"}";
	 }
	 
	 @RequestMapping(
			  value = "/microservices", 
			  method = RequestMethod.GET,
			  produces = "applciation/json")
	 public String getMicroservices() throws IOException{
		 
        Properties props = this.getProps();
        
        String serviceRegistryType=props.getProperty("composition.serviceregistry.type");
        String serviceRegistryURL=props.getProperty("composition.serviceregistry.url");
        
        switch(serviceRegistryType){
        	case "eureka": return  HTTPClient.get(serviceRegistryURL+"/eureka/apps", "application/json");
        }
        
		return "";
	 }
	 
	 
	 private void saveComposition(String id, String name, String bpmn) throws FileNotFoundException, UnsupportedEncodingException, JSONException{
		 Properties props=getProps();
         String fragmentManagerURL=props.getProperty("composition.fragmentmanager.url");

         JSONObject compositionJSON=new JSONObject();
         compositionJSON.put("id", id);
         compositionJSON.put("name", name);
         compositionJSON.put("xml", bpmn);
		 
         try{
        	 HTTPClient.post(fragmentManagerURL+"/compositions", compositionJSON.toString(), false, "application/json");
        	 System.out.println("Sent to the Fragment Manager");
         }catch(Exception e){
        	 System.out.println("The Fragment Manager is not available");
         }
			 
         String fileName=fileManager.saveCompositionFile(id, bpmn);
         
        dao.getComposition().save(id, name, fileName);
	 }
	 
	 
	 private Properties props;
	 private Properties getProps(){
		 if(this.props==null){
			 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
			 yamlFactory.setResources(new ClassPathResource("application.yml"));
			 this.props = yamlFactory.getObject();
		 }
         return this.props;
	 }
	 
}
