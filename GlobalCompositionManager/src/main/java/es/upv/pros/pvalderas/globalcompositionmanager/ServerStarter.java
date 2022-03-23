package es.upv.pros.pvalderas.globalcompositionmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.globalcompositionmanager.dao.DAO;
import es.upv.pros.pvalderas.globalcompositionmanager.events.EventManager;


@Component
public class ServerStarter implements ApplicationRunner {
	
	@Autowired
	private DAO dao;
	
	@Autowired
	private EventManager eventManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
    	 System.out.print("Setting up the Global Composition Manager.....");
   
    	 dao.getComposition().createTables();
    	 dao.getDirtyComposition().createTables();
            
         File dir=new File("compositions");
         dir.mkdirs();
         
         List<String> compositions=checkExistingCompositions(dir);
         
         BrokerConfig.configMessageBroker(getProperties());
         
         for(String composition:compositions){
        	 eventManager.registerEventListener(composition);
         }

         System.out.println("OK.");
         
    }
    
    private List<String> checkExistingCompositions(File dir) throws IOException{
    	List<String> compositionNames=new ArrayList<String>();
    	String files[]=dir.list();
    	for(String file:files){
    		 String id= file.substring(0,file.indexOf("."));
    		 dao.getComposition().save(id, "", "compositions/"+file);
    		 compositionNames.add(id);
    	}
    	
    	return compositionNames;
    }
    
    private static Properties getProperties(){
   	 	YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application.yml"));
        return yamlFactory.getObject();
   }
}
