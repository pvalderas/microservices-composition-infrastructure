package es.upv.pros.pvalderas.compositioncoordinator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
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

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.compositioncoordinator.dao.DAO;
import es.upv.pros.pvalderas.compositioncoordinator.events.EventManager;

@RestController
@CrossOrigin
public class ClientHTTPController {
	
	@Autowired
	private DAO dao;
	
	@Autowired
	private SpringProcessEngineConfiguration config;
	
    @Autowired
    private ResourcePatternResolver resourceLoader;
    
    @Autowired
    private EventManager eventManager;
	
	@RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.POST,
			  consumes = "application/json")
	 public void saveBPMNPiece(@RequestBody BPMNFragment fragment) throws IOException, TimeoutException {
	
			 File dir=new File("fragments/"+fragment.getComposition());
			 dir.mkdir();
			 
			 String fileName="fragments/"+fragment.getComposition()+"/"+fragment.getId()+".bpmn";
			 File fichero=new File(fileName);
			 PrintWriter writer = new PrintWriter(fichero, "UTF-8");
			 writer.print(fragment.getXml());
			 writer.close();
			 
			 eventManager.registerEventListener(fragment.getMicroservice(),fragment.getComposition());
			 
			 /*repositoryService.deleteDeployment(fragment.getId());
			 DeploymentBuilder deploymentBuilder= repositoryService.createDeployment();
			 deploymentBuilder.addString(fragment.getId(),fragment.getXml());
			 Deployment deployment= deploymentBuilder.deploy();
			 System.out.println(deployment.getId());*/
			 
			 
			 final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/fragments/*/*.bpmn");
		     System.out.println("Loaded Fragments: "+resources.length);
		     config.setDeploymentResources(resources);
		     config.buildProcessEngine();
			 
			 dao.saveFragment(fragment, fileName);

	 }
	
	
	 @RequestMapping(
			  value = "/operations", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getOperations() {
		 return dao.getOperations();
	 }
	 
	 @RequestMapping(
			  value = "/microservicename", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getName() {
		 return dao.getMicroserviceName();
	 }
	 
	 @RequestMapping(
			  value = "/fragments", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public List<Map<String, Object>> getFragments() {
		 return dao.getFragments();
	 }
	 
	 @RequestMapping(
			  value = "/fragmentbpmn/{composition}", 
			  method = RequestMethod.GET,
			  produces = "application/json")
	 @Transactional
	 public String getFragmentBPMN(@PathVariable(value="composition") String composition) throws IOException {
		 final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/fragments/"+composition+"/*.bpmn");
		 if(resources.length==1){
			 return new String(Files.readAllBytes(Paths.get(resources[0].getURI())));
		 }
		 else return "";
	 }

	 
	 @RequestMapping(
			  value = "/fragmentmanagerurl", 
			  method = RequestMethod.GET)
	 public String getFragmentManagerURL() {
		 
		 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
		 yamlFactory.setResources(new ClassPathResource("application.yml"));
		 Properties props = yamlFactory.getObject();
     
		 return props.getProperty("composition.fragmentmanager.url");
		 
	 }
}
