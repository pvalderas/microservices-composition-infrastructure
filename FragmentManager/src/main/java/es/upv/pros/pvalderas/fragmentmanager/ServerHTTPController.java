package es.upv.pros.pvalderas.fragmentmanager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.dom4j.DocumentException;
import org.jaxen.JaxenException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedModel;
import es.upv.pros.pvalderas.composition.bpmn.domain.AffectedParticipant;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNComposition;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.LocalChange;
import es.upv.pros.pvalderas.composition.bpmn.domain.MicroService;
import es.upv.pros.pvalderas.composition.http.HTTPClient;
import es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter.SplittedSimplifier;
import es.upv.pros.pvalderas.fragmentmanager.bpmn.splitter.Splitter;
import es.upv.pros.pvalderas.fragmentmanager.events.EventSender;

@RestController
@CrossOrigin
public class ServerHTTPController {
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;
	 
	 @Autowired
	 EventSender eventSender;
	 
	
	 @RequestMapping(
			  value = "/compositions", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public void splitComposition(@RequestBody BPMNComposition composition) throws IOException, JSONException, DocumentException, JaxenException {
		 
		 System.out.println("BPMN Composition received!");
		 
		 Splitter splitter=new Splitter(composition.getXml());
		 List<BPMNFragment> fragments=splitter.split();
		
		 Integer numParticipants=fragments.size();
			 
         System.out.println("Composition Split into "+numParticipants+" Fragments");
         
         for(BPMNFragment fragment: fragments){
        	 
        	 String url="";
        	 List<Map<String, Object>> row=jdbcTemplate.queryForList("SELECT url FROM microservices WHERE lower(id)=?", fragment.getMicroservice().toLowerCase());
        	 if(!row.isEmpty()){
        		 url=(String)row.get(0).get("url");
        	 }
        	
        	 
        	 JSONObject fragmentJSON=new JSONObject();
             fragmentJSON.put("id", fragment.getId());
             fragmentJSON.put("composition", fragment.getComposition());
             fragmentJSON.put("xml", fragment.getXml());
             fragmentJSON.put("microservice",fragment.getMicroservice());
             fragmentJSON.put("numParticipants",numParticipants);
        	 
        	try{
        		System.out.println(url);
        		HTTPClient.post(url, fragmentJSON.toString(), false, "application/json");
	            System.out.println("Fragment sent to "+fragment.getMicroservice());
	         }catch(Exception e){
	        	 System.out.println("ERROR: "+ fragment.getMicroservice() +" is not available. Fragment not sent.");
	         }
         }
	 }
	 
	 
	 @RequestMapping(
			  value = "/microservices", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public void saveMicroservice(@RequestBody MicroService microservice) {
		 jdbcTemplate.update("DELETE FROM microservices WHERE id=?", microservice.getId().toLowerCase());
		 jdbcTemplate.update("INSERT INTO microservices(id,url) VALUES(?,?)", microservice.getId().toLowerCase(), microservice.getUrl());
	 }
	 
	 @RequestMapping(
			  value = "/microservices", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getMicroservices() {
		 return jdbcTemplate.queryForList("SELECT * FROM microservices");
	 }
	 

	 @RequestMapping(
			  value = "/microservices/{id}", 
			  method = RequestMethod.GET)
	 public Object getMicroservice(@PathVariable(value="id") String id) {
		 return jdbcTemplate.queryForList("SELECT * FROM microservices WHERE id=?",id).get(0);
	 }
	 

	 @RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public String resendFragment(@RequestBody BPMNFragment fragment) throws DocumentException, JaxenException, IOException, JSONException {
		 
		 System.out.println("BPMN Fragment received!");
		
		 Properties props = this.getProps();
         
         String GlobalCompositionManagerURL=props.getProperty("composition.globalcompositionmanager.url");

         JSONObject fragmentJSON=new JSONObject();
         fragmentJSON.put("id", fragment.getId());
         fragmentJSON.put("composition", fragment.getComposition());
         
         SplittedSimplifier simplifierToSend=new SplittedSimplifier(fragment.getComposition(),fragment.getXml()); 
         fragmentJSON.put("xml", simplifierToSend.simplify());
		 
         try{
        	 System.out.println("Sent to the Global Composition Manager");
        	 return HTTPClient.post(GlobalCompositionManagerURL+"/fragments", fragmentJSON.toString(), true, "application/json");	 
         }catch(Exception e){
        	 System.out.println("ERROR: Global Manager is not available");
        	 return "0";
         }
		 
	 }

	 
	 @RequestMapping(
			  value = "/evolution", 
			  method = RequestMethod.POST,
			  consumes = "application/json",
			  produces = "application/json")
	 @Transactional
	 public String resendCoordinationChange(@RequestBody LocalChange change) throws DocumentException, JaxenException, IOException, JSONException {
		 
		System.out.println("Local Change received!");
		
		Properties props = this.getProps();
        String GlobalCompositionManagerURL=props.getProperty("composition.globalcompositionmanager.url");
        
        SplittedSimplifier dirtyWithColorsSimplifierToSend=new SplittedSimplifier(change.getComposition(),change.getDirtyXml()); 
        SplittedSimplifier dirtySimplifierToSend=new SplittedSimplifier(change.getComposition(),change.getXml()); 
        change.setDirtyXml(dirtyWithColorsSimplifierToSend.simplify());
        change.setXml(dirtySimplifierToSend.simplify());
		 
        String adaptation="";
        try{
	       	adaptation=HTTPClient.post(GlobalCompositionManagerURL+"/evolution", change.toJSON(), true, "application/json");
	       	System.out.println("Sent to the Global Composition Manager");
        }catch(Exception e){
        	System.out.println("ERROR: Global Manager is not available");
        }
        
        return adaptation;
	 }
	 
	 @RequestMapping(
			  value = "/adaptation", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 public void evolveComposition(@RequestBody String adaptation) throws JSONException, DocumentException, JaxenException{
		
		System.out.println("BPMN Adapted Composition received!");

		AdaptedModel adaptedModel=AdaptedModel.parseJSON(adaptation);
			 
		 
		 Splitter splitter=new Splitter(adaptedModel.getBpmn());
		 Splitter splitter2=new Splitter(adaptedModel.getBpmnToConfirm());
		 for(AffectedParticipant a:adaptedModel.getAffectedParticipants()){
			 String adaptedFragmentXML=splitter.split(a.getMicroservice());
			 String adaptedFragmentXML2=splitter2.split(a.getMicroservice());
			 AdaptedFragment adaptedFragment=new AdaptedFragment();
			 adaptedFragment.setBpmn(adaptedFragmentXML);
			 adaptedFragment.setBpmnToConfirm(adaptedFragmentXML2);
			 adaptedFragment.setComposition(adaptedModel.getComposition());
			 adaptedFragment.setMicroservice(a.getMicroservice());
			 adaptedFragment.setModifiedMicroservice(adaptedModel.getModifiedParticipant());
			 adaptedFragment.setType(a.getAdaptationType());
			 adaptedFragment.setChangesJSON(a.getChangesJSON());
			 eventSender.evolutionChange(adaptedFragment);
		 }

	 }
	 
	 private Properties getProps(){
		 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
	     yamlFactory.setResources(new ClassPathResource("application.yml"));
	     Properties props = yamlFactory.getObject();
	     return props;
	 }
}
