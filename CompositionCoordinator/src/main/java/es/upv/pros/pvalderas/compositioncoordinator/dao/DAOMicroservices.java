package es.upv.pros.pvalderas.compositioncoordinator.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DAOMicroservices {
	
	protected static final String MICROSERVICE_TABLE="microservice";
	protected static String OPERATIONS_TABLE="operations";

	 @Autowired
	 private JdbcTemplate jdbcTemplate;

	 /***************************************************************/
	 /*************************** MICROSERVICE **********************/
	 /***************************************************************/
	 
	 public void createMicroserviceTable(){
		 jdbcTemplate.execute("DROP TABLE "+MICROSERVICE_TABLE+" IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE "+MICROSERVICE_TABLE+"(id VARCHAR(255) PRIMARY KEY)");
	 }
	 
	 public void saveMicroserviceName(String name){
		 jdbcTemplate.update("DELETE FROM "+MICROSERVICE_TABLE);
		 jdbcTemplate.update("INSERT INTO "+MICROSERVICE_TABLE+" (id) VALUES (?)", name);
	 }
	 
	 public String getMicroserviceName(){
		 return (String)jdbcTemplate.queryForMap("SELECT id FROM "+MICROSERVICE_TABLE).get("id");
	 }
	 

	 /***************************************************************/
	 /**************************** OPERATIONS ***********************/
	 /***************************************************************/
	  
	 public void createOperationTable(){
		 jdbcTemplate.execute("DROP TABLE "+OPERATIONS_TABLE+" IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE "+OPERATIONS_TABLE+"(id VARCHAR(255) PRIMARY KEY, url VARCHAR(1024), method VARCHAR(10))");
	 }
	 
	 public List<Map<String, Object>> getOperations(){
		 return jdbcTemplate.queryForList("SELECT * FROM "+OPERATIONS_TABLE);
	 }
	 
	 public void saveOperation(String id, String url, String method){
		 jdbcTemplate.update("DELETE FROM "+OPERATIONS_TABLE+" WHERE id=?", id);
		 jdbcTemplate.update("INSERT INTO "+OPERATIONS_TABLE+"(id, url,method) VALUES (?,?,?)", id, url,method);
	 }
}
