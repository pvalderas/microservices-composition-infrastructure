package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import java.io.IOException;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class CamundaEngine {
	@Autowired
	private SpringProcessEngineConfiguration config;
	
    @Autowired
    private ResourcePatternResolver resourceLoader;
	
	public void run() throws IOException{
		 final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/fragments/*/*.bpmn");
	     System.out.println("Loaded Fragments: "+resources.length);
	     config.setDeploymentResources(resources);
	     config.buildProcessEngine();
	}
}


/*repositoryService.deleteDeployment(fragment.getId());
DeploymentBuilder deploymentBuilder= repositoryService.createDeployment();
deploymentBuilder.addString(fragment.getId(),fragment.getXml());
Deployment deployment= deploymentBuilder.deploy();
System.out.println(deployment.getId());*/