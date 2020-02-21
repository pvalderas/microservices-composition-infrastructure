package es.upv.pros.pvalderas.globalcompositionmanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNComposition;
import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.globalcompositionmanager.bpmn.joiner.Joiner;
import es.upv.pros.pvalderas.globalcompositionmanager.http.HTTPClient;

@RestController
@CrossOrigin
public class ServerHTTPController {
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;
	 
	
	 @RequestMapping(
			  value = "/compositions", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public void saveProcess(@RequestBody BPMNComposition composition) throws IOException, JSONException {
		 
		 System.out.println("BPMN Composition received!");
		 
		 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
         yamlFactory.setResources(new ClassPathResource("application.yml"));
         Properties props = yamlFactory.getObject();
         
         String fragmentManagerURL=props.getProperty("composition.fragmentmanager.url");

         JSONObject compositionJSON=new JSONObject();
         compositionJSON.put("id", composition.getId());
         compositionJSON.put("name", composition.getName());
         compositionJSON.put("xml", composition.getXml());
		 
         try{
        	 HTTPClient.post(fragmentManagerURL, compositionJSON.toString(), false, "application/json");
        	 System.out.println("Sent to the Fragment Manager");
         }catch(Exception e){
        	 System.out.println("The Fragment Manager is not available");
         }
			 
         String fileName=this.saveCompositionFile(composition.getId(), composition.getXml());
         
         jdbcTemplate.update("DELETE FROM compositions WHERE id=?",composition.getId());
		 jdbcTemplate.update("INSERT INTO compositions(id, name, file) VALUES (?,?, ?)", composition.getId(), composition.getName(), fileName);
		 
         
		 System.out.println("Stored in the Composition Repository");
	 }
	 
	 @RequestMapping(
			  value = "/compositions", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getProcessess() {
		 return jdbcTemplate.queryForList("SELECT * FROM compositions");
	 }
	 

	 @RequestMapping(
			  value = "/compositions/{id}", 
			  method = RequestMethod.GET)
	 public Object getProcess(@PathVariable(value="id") String id) {
		 return jdbcTemplate.queryForList("SELECT * FROM compositions WHERE id=?",id).get(0);
	 }
	 

	 @RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 @Transactional
	 public void joinFragment(@RequestBody BPMNFragment fragment) throws DocumentException, JaxenException, IOException {
		 
		 System.out.println("BPMN Fragment received!");
		
		 Map<String,Object> row=jdbcTemplate.queryForList("SELECT file FROM compositions WHERE id=?",fragment.getComposition()).get(0);
		 
		 String compositionBPMN=getCompositionFromFile((String)row.get("file"));
		 String fragmentBPMN=fragment.getXml();
		 
		 Joiner joiner=new Joiner(compositionBPMN,fragmentBPMN);
		 String newComposition=joiner.join();
		
		 saveCompositionFile(fragment.getComposition(), newComposition);
		 
		 System.out.println("Composition updated.");
		 
	 }
	 
	 @RequestMapping(
			  value = "/serviceregistry", 
			  method = RequestMethod.GET)
	 public String getServiceRegistry(){
		 
		 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
         yamlFactory.setResources(new ClassPathResource("application.yml"));
         Properties props = yamlFactory.getObject();
         
         String serviceRegistryType=props.getProperty("composition.serviceregistry.type");
         String serviceRegistryURL=props.getProperty("composition.serviceregistry.url");
         
		 return "{\"type\":\""+serviceRegistryType+"\",\"url\":\""+serviceRegistryURL+"\"}";
	 }
	
	 private String saveCompositionFile(String id, String xml) throws FileNotFoundException, UnsupportedEncodingException{
		 String fileName="compositions/"+id+".bpmn";
         File fichero=new File(fileName);
		 PrintWriter writer = new PrintWriter(fichero, "UTF-8");
		 writer.print(xml);
		 writer.close();
		 return fileName;
	 }
	 
	 private String getCompositionFromFile(String fileName) throws IOException{
		 String xml=new String(Files.readAllBytes(Paths.get(fileName))); 
		 return xml;
	 }

}
