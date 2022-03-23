package es.upv.pros.pvalderas.compositioncoordinator.dao;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;

@Component
public class DAOFragments {
	
	protected static final String FRAGMENTS_TABLE="fragments";
	protected static final String LOCAL_CHANGES_TABLE=DAOLocalChanges.LOCAL_CHANGES_TABLE;
	

	 @Autowired
	 private JdbcTemplate jdbcTemplate;
	 

	 
	 public void createFragmentsTable(){
	    	jdbcTemplate.execute("DROP TABLE "+FRAGMENTS_TABLE+" IF EXISTS");
	    jdbcTemplate.execute("CREATE TABLE "+FRAGMENTS_TABLE+"(id VARCHAR(255) PRIMARY KEY, composition VARCHAR(255), microservice VARCHAR(255), file VARCHAR(1024), numParticipants int)");         
	 }
	 
	 public void saveFragment(BPMNFragment fragment, String fileName){
		 jdbcTemplate.update("DELETE FROM "+FRAGMENTS_TABLE+" WHERE id=?", fragment.getId().toLowerCase());
		 jdbcTemplate.update("INSERT INTO "+FRAGMENTS_TABLE+" (id, composition, microservice, file, numParticipants) VALUES (?,?,?,?,?)", 
				 				fragment.getId().toLowerCase(), fragment.getComposition(), fragment.getMicroservice(), fileName, fragment.getNumParticipants());
	 }
	
	 public List<Map<String, Object>> getFragments(){
		 return jdbcTemplate.queryForList("SELECT * FROM "+FRAGMENTS_TABLE);
	 }
	 
	 public Map<String, Object> getFragment(String id){
		 return jdbcTemplate.queryForMap("SELECT * FROM "+FRAGMENTS_TABLE+" WHERE id=?",id);
	 }
	 
	 public BPMNFragment getFragmentByComposition(String composition){
		 Map<String, Object> row= jdbcTemplate.queryForMap("SELECT * FROM "+FRAGMENTS_TABLE+" WHERE composition=?",composition);
		 
		 BPMNFragment fragment=new BPMNFragment();
		 
		 fragment.setComposition(row.get("composition").toString());
		 fragment.setId(row.get("id").toString());
		 if(row.get("microservice")!=null) fragment.setMicroservice(row.get("microservice").toString());
		 fragment.setNumParticipants(Integer.parseInt(row.get("numParticipants").toString()));
		 
		 
		 
		 return fragment;
	 }
	 
	 public Integer getCompositionParticipants(String composition){
		 Map<String, Object> map=jdbcTemplate.queryForMap("SELECT numParticipants FROM "+FRAGMENTS_TABLE+" WHERE composition=?",composition);
		 return (Integer)map.get("numParticipants");
	 }
	 
	 public Integer getCompositionParticipantsFromChangeId(Integer id){
		 Map<String, Object> map=jdbcTemplate.queryForMap("SELECT f.numParticipants FROM "+FRAGMENTS_TABLE+" f,"+LOCAL_CHANGES_TABLE+" c WHERE c.composition=f.composition AND c.id=?",id);
		 return (Integer)map.get("numParticipants");
	 }
}
