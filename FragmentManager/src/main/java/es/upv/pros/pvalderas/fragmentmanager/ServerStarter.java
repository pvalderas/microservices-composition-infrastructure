package es.upv.pros.pvalderas.fragmentmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


@Component
public class ServerStarter implements ApplicationRunner {
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
    	
    	 System.out.print("Setting up the Fragment Manager.....");
   
         jdbcTemplate.execute("DROP TABLE microservices IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE microservices(id VARCHAR(255) PRIMARY KEY, url VARCHAR(1024))");
                 
         System.out.println("OK");
         
    }
    
}
