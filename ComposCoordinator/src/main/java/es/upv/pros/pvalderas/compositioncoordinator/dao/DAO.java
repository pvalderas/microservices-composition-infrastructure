package es.upv.pros.pvalderas.compositioncoordinator.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;

@Component
public class DAO {

	 @Autowired
	 private JdbcTemplate jdbcTemplate;
	 
	 public void saveFragment(BPMNFragment fragment, String fileName){
		 jdbcTemplate.update("DELETE FROM fragments WHERE id=?", fragment.getId());
		 jdbcTemplate.update("INSERT INTO fragments (id, composition, microservice, file) VALUES (?,?,?,?)", 
				 				fragment.getId(), fragment.getComposition(), fragment.getMicroservice(), fileName);
	 }
	 
	 public void saveOperation(String id, String url, String method){
		 jdbcTemplate.update("DELETE FROM operations WHERE id=?", id);
		 jdbcTemplate.update("INSERT INTO operations(id, url,method) VALUES (?,?,?)", id, url,method);
	 }
	 
	 public List<Map<String, Object>> getOperations(){
		 return jdbcTemplate.queryForList("SELECT * FROM operations");
	 }
	
	 public List<Map<String, Object>> getFragments(){
		 return jdbcTemplate.queryForList("SELECT * FROM fragments");
	 }
	 
	 public Map<String, Object> getFragment(String id){
		 return jdbcTemplate.queryForMap("SELECT * FROM fragments WHERE id=?",id);
	 }
	 
	 public void createFragmentsTable(){
	    	jdbcTemplate.execute("DROP TABLE fragments IF EXISTS");
	        jdbcTemplate.execute("CREATE TABLE fragments(id VARCHAR(255) PRIMARY KEY, composition VARCHAR(255), microservice VARCHAR(255), file VARCHAR(1024))");         
	 }
	 
	 public void createOperationTable(){
		 jdbcTemplate.execute("DROP TABLE operations IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE operations(id VARCHAR(255) PRIMARY KEY, url VARCHAR(1024), method VARCHAR(10))");
	 }
	 
	 public void createMicroserviceTable(){
		 jdbcTemplate.execute("DROP TABLE microservice IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE microservice(id VARCHAR(255) PRIMARY KEY)");
	 }
	 
	 public void saveMicroserviceName(String name){
		 jdbcTemplate.update("DELETE FROM microservice");
		 jdbcTemplate.update("INSERT INTO microservice (id) VALUES (?)", name);
	 }
	 
	 public String getMicroserviceName(){
		 return (String)jdbcTemplate.queryForMap("SELECT id FROM microservice").get("id");
	 }
}
