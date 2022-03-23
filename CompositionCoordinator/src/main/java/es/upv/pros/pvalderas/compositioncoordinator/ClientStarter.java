package es.upv.pros.pvalderas.compositioncoordinator;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.MicroService;
import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.compositioncoordinator.dao.DAO;
import es.upv.pros.pvalderas.compositioncoordinator.events.EventManager;

@Component
public class ClientStarter implements ApplicationRunner {
	 
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private DAO dao;
		
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
	   return builder.build();
	}
	 
	 @Autowired
	 private ApplicationContext context;
	 
	 @Autowired
	  private EventManager eventManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
		Class mainClass=context.getBeansWithAnnotation(CompositionCoordinator.class).values().iterator().next().getClass().getSuperclass();
		 
		if(mainClass!=null){
			System.out.print("Setting up Composition Coordinator......");
			 
			//String microServiceName=mainClass.getName().substring(mainClass.getName().lastIndexOf(".")+1);
			Properties props=this.loadProperties();
			String microServiceName=props.getProperty("spring.application.name");
			if(microServiceName==null) microServiceName=mainClass.getName().substring(mainClass.getName().lastIndexOf(".")+1);
				        
			this.createFragmentsFolder();
			dao.getFragments().createFragmentsTable();      
			
			dao.getMicroservices().createMicroserviceTable();
			dao.getMicroservices().saveMicroserviceName(microServiceName);
			dao.getLocalChanges().createLocalChangesTable();
			dao.getParticipantChanges().createTable();
			
			this.configMessageBroker(props);
			String microserviceURL=this.sendURLtoFragmentManager(props, microServiceName);       
			this.registerOperations(mainClass,microserviceURL);
			
			String[] compositions=loadAllFragments();
			
			for(String composition:compositions){
				eventManager.registerEventListener(microServiceName, composition);
			}
				      
			System.out.println("OK");
		}
         
    }
    
    
    private Properties loadProperties(){
    	 YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
         yamlFactory.setResources(new ClassPathResource("application.yml"));
         return yamlFactory.getObject();
    }
    
    
    private void createFragmentsFolder() throws IOException{
    	File dir=new File("fragments");
    	dir.mkdirs();
    }
    
    private void configMessageBroker(Properties props){
        
        BrokerConfig.setBrokerType(props.getProperty("composition.messagebroker.type"));
        BrokerConfig.setHost(props.getProperty("composition.messagebroker.host"));
        BrokerConfig.setVirtualHost(props.getProperty("composition.messagebroker.virtualHost"));
        BrokerConfig.setPort(props.getProperty("composition.messagebroker.port"));
        BrokerConfig.setUser(props.getProperty("composition.messagebroker.user"));
        BrokerConfig.setPassword(props.getProperty("composition.messagebroker.password")); 
        
    }
    
    private String sendURLtoFragmentManager(Properties props, String microServiceName) throws UnknownHostException{
    	
    	String fragmentManagerURL=props.getProperty("composition.fragmentmanager.url");
    	
        String microservicePORT = props.getProperty("server.port")!=null?props.getProperty("server.port"):"8080";
        String microserviceIP = props.getProperty("server.ip")!=null?props.getProperty("server.ip"):InetAddress.getLocalHost().getHostAddress();
        
        String protocol="";
        if(props.getProperty("server.ssl.enabled")!=null && props.getProperty("server.ssl.enabled")=="true")
        	protocol="https://";
        else
        	protocol="http://";
        
        String microserviceURL=protocol+microserviceIP+":"+microservicePORT;
        
        MicroService microservice=new MicroService();
        microservice.setId(microServiceName);
        microservice.setUrl(microserviceURL+"/fragments");
        
        this.restTemplate.postForEntity(fragmentManagerURL+"/microservices", microservice, MicroService.class);
        
        return microserviceURL;
    }
    
    private void registerOperations(Class mainClass, String microserviceURL) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException{
    	 dao.getMicroservices().createOperationTable();
         
         Annotation annotation = mainClass.getDeclaredAnnotation(CompositionCoordinator.class);
         Class classAPI=(Class)annotation.annotationType().getMethod("serviceAPIClass").invoke(annotation);
         
         for(Method m:classAPI.getMethods()){
        	 Annotation request=m.getAnnotation(RequestMapping.class);
        	 if(request!=null){
        		String[] path=(String[])request.annotationType().getMethod("value").invoke(request);
        		RequestMethod[] methods=(RequestMethod[])request.annotationType().getMethod("method").invoke(request);        		
        		
        		dao.getMicroservices().saveOperation(m.getName(), microserviceURL+path[0], methods[0].name());
        	
        	 }
         }
    }
    
    private String[] loadAllFragments(){
		File dir=new File("fragments");
    	String compositions[]=dir.list();
    	ArrayList<String> compoNames=new ArrayList<String>();

    	for(String compoDir:compositions){
    		if(compoDir.charAt(0)!='.'){
	    		File subDir=new File("fragments/"+compoDir);
	        	String files[]=subDir.list();	
	    		if(files!=null){
	    			String[] composition=compoDir.split("-");
	    			compoNames.add(composition[0]);
		        	for(String file:files){
		        		if(file.indexOf(".bpmn")>0){
				    		 String id=file.substring(0,file.indexOf("."));
				    		 BPMNFragment fragment=new BPMNFragment();
				    		 fragment.setId(id);
				    		 fragment.setComposition(composition[0]);
				    		 fragment.setNumParticipants(new Integer(composition[1]));
				    		 String fileName="fragments/"+compoDir+"/"+file;
				    		 dao.getFragments().saveFragment(fragment, fileName);
		        		}
		        	}
	    		}
    		}
    	}
    	
    	return compoNames.toArray(new String[0]);
    }
    
    
}
