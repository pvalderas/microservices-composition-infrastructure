package es.upv.pros.pvalderas.globalcompositionmanager;

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
    	
    	 System.out.print("Setting up the Global Composition Manager.....");
   
         jdbcTemplate.execute("DROP TABLE compositions IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE compositions(id VARCHAR(255) PRIMARY KEY, name VARCHAR(255), file VARCHAR(1024))");
                 
         File dir=new File("compositions");
         dir.mkdirs();
         
         checkExistingCompositions(dir);
         
         System.out.println("OK");
         
    }
    
    private void checkExistingCompositions(File dir) throws IOException{
    	String files[]=dir.list();
    	for(String file:files){
    		 String id= file.substring(0,file.indexOf("."));
    		 jdbcTemplate.update("INSERT INTO compositions(id, name, file) VALUES (?,?, ?)", id, "", "compositions/"+file);
    	}
    }
    
}
