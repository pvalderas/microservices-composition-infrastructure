package es.upv.pros.pvalderas.fragmentmanager;

import java.io.IOException;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.eventbroker.utils.BrokerConfig;
import es.upv.pros.pvalderas.composition.http.HTTPClient;


@Component
public class ServerStarter implements ApplicationRunner {
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
    	 	System.out.print("Setting up the Fragment Manager.....");
   
        jdbcTemplate.execute("DROP TABLE microservices IF EXISTS");
        jdbcTemplate.execute("CREATE TABLE microservices(id VARCHAR(255) PRIMARY KEY, url VARCHAR(1024))");
         
        loadMicroservices();
         
        BrokerConfig.configMessageBroker(getProperties());
              
        System.out.println("OK");
         
    }
    
    private static Properties getProperties(){
   	 	YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application.yml"));
        return yamlFactory.getObject();
   }
    
    private void loadMicroservices() throws IOException, JSONException{
		 
        Properties props = getProperties();
        
        String serviceRegistryType=props.getProperty("composition.serviceregistry.type");
        String serviceRegistryURL=props.getProperty("composition.serviceregistry.url");
        
        switch(serviceRegistryType){
        		case "eureka": String response=HTTPClient.get(serviceRegistryURL+"/eureka/apps", "application/json");
        					   JSONObject system=new JSONObject(response);
        					   JSONArray microservices=system.getJSONObject("applications").getJSONArray("application");
        					   
        					   for(int i=0;i<microservices.length();i++){
        						   String microserviceId=microservices.getJSONObject(i).getString("name").toLowerCase();
        						   JSONObject instance=microservices.getJSONObject(i).getJSONArray("instance").getJSONObject(0);
        						   String microserviceUrl="http://"+instance.getString("ipAddr")+":"+instance.getJSONObject("port").getInt("$")+"/fragments";
        						   jdbcTemplate.update("DELETE FROM microservices WHERE id=?", microserviceId);
            					   jdbcTemplate.update("INSERT INTO microservices(id,url) VALUES(?,?)", microserviceId, microserviceUrl);
        					   }
        					  

        }

	 }
}
