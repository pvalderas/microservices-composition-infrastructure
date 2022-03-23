package es.upv.pros.pvalderas.compositioncoordinator.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.BPMNFragment;
import es.upv.pros.pvalderas.composition.bpmn.domain.LocalChange;
import es.upv.pros.pvalderas.composition.bpmn.domain.ParticipantChange;

@Component
public class DAOLocalChanges {
	
	protected static final String LOCAL_CHANGES_TABLE="local_changes";

	 @Autowired
	 private JdbcTemplate jdbcTemplate;
	 
	
	 public void createLocalChangesTable(){
		 jdbcTemplate.execute("DROP TABLE "+LOCAL_CHANGES_TABLE+" IF EXISTS");
         jdbcTemplate.execute("CREATE TABLE "+LOCAL_CHANGES_TABLE+"(id INT AUTO_INCREMENT, fragment VARCHAR(255), composition VARCHAR(255), xml CLOB, dirtyxml CLOB, description VARCHAR(1024), date VARCHAR(64), affected TINYINT, responses TINYINT, accepted TINYINT)");
	 }
	 
	 public Integer saveLocalChanges(String fragment, String composition, String xml, String dirtyxml, String description, String date){
         //jdbcTemplate.execute("INSERT INTO changes(changes, date, accepted) VALUES('"+changes+"','"+date+"',0)");
         
         KeyHolder keyHolder = new GeneratedKeyHolder();
         
         jdbcTemplate.update(connection -> {
		        PreparedStatement ps = connection
		          .prepareStatement("INSERT INTO "+LOCAL_CHANGES_TABLE+"(fragment, composition, xml, dirtyxml, description, date, affected, responses, accepted) VALUES(?,?,?,?,?,?,0,0,NULL)", Statement.RETURN_GENERATED_KEYS);
		          ps.setString(1, fragment);
		          ps.setString(2, composition);
		          ps.setString(3, xml);
		          ps.setString(4, dirtyxml);
		          ps.setString(5, description);
		          ps.setString(6, date);
		          return ps;
		        }, keyHolder);
		 
		 int id=keyHolder.getKey().intValue();
		 return id;
	 }
	 
	 public Integer getLocalChangeTrueResponses(Integer id){
		 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT responses FROM "+LOCAL_CHANGES_TABLE+" WHERE id="+id);
         return new Integer(result.get("responses").toString());
	 }

	 public Integer getLocalChangeTrueResponsesByComposition(String composition){
		 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT responses FROM "+LOCAL_CHANGES_TABLE+" WHERE composition=?",composition);
         return new Integer(result.get("responses").toString());
	 }
	 
	 public void updatedAffectedByLocalChanges(Integer id, Integer affected){
         jdbcTemplate.update("UPDATE "+LOCAL_CHANGES_TABLE+" SET affected=? WHERE id=?",affected, id);
	 }
	 
	 
	 public Integer getAffectedParticipantsByLocalChange(Integer id){
		 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT affected FROM "+LOCAL_CHANGES_TABLE+" WHERE id="+id);
         return new Integer(result.get("affected").toString());
	 }
	 
	 public Integer getAffectedParticipantsByComposition(String composition){
		 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT affected FROM "+LOCAL_CHANGES_TABLE+" WHERE composition=?",composition);
         return new Integer(result.get("affected").toString());
	 }
	 
	
	 
	 public Integer addTrueResponseToLocalChanges(Integer id){
         jdbcTemplate.execute("UPDATE "+LOCAL_CHANGES_TABLE+" SET responses=responses+1 WHERE id="+id);
         return this.getLocalChangeTrueResponses(id);
	 }
	 
	 public Integer addTrueResponseToLocalChangeByComposition(String composition){
         jdbcTemplate.update("UPDATE "+LOCAL_CHANGES_TABLE+" SET responses=responses+1 WHERE composition=?",composition);
         return this.getLocalChangeTrueResponsesByComposition(composition);
	 }
	 
	 public void acceptGlobalLocalChanges(Integer id){
         jdbcTemplate.execute("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=1 WHERE id="+id);
	 }	 
	 public void acceptGlobalAndParticipantsLocalChanges(Integer id){
         jdbcTemplate.execute("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=2 WHERE id="+id);
	 } 
	 public void rejectLocalChanges(Integer id){
         jdbcTemplate.execute("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=0 WHERE id="+id);
	 }
	 
	 public void acceptGlobalLocalChangesByComposition(String composition){
         jdbcTemplate.update("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=1 WHERE composition=?",composition);
	 }	 
	 public void acceptGlobalAndParticipantsLocalChangesByComposition(String composition){
		 jdbcTemplate.update("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=2 WHERE composition=?",composition);
	 } 
	 public void rejectLocalChangesByComposition(String composition){
		 jdbcTemplate.update("UPDATE "+LOCAL_CHANGES_TABLE+" SET accepted=0 WHERE composition=?",composition);
	 }
	 
	 public LocalChange getLocalChanges(Integer id){
         try{
        	 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT * FROM "+LOCAL_CHANGES_TABLE+" WHERE id="+id);
        	 return getLocalChange(result);
         }catch(EmptyResultDataAccessException e){
        	return null;
         }
	 }
	 
	 public LocalChange getOpenChangeByComposition(String composition){
		 try{
			 Map<String,Object> result=jdbcTemplate.queryForMap("SELECT * FROM "+LOCAL_CHANGES_TABLE+" WHERE composition='"+composition+"'");
	         return getLocalChange(result);
		 }catch(EmptyResultDataAccessException e){
	        	return null;
	     }
	 }
	 
	 public List<LocalChange> getAllLocalChanges(){
         List<Map<String,Object>> result=jdbcTemplate.queryForList("SELECT * FROM "+LOCAL_CHANGES_TABLE);
         
         List<LocalChange> changes=new ArrayList<LocalChange>();
         for(Map<String,Object> row:result){
        	 changes.add(getLocalChange(row));
         }
         
         return changes;
	 }
	 
	 
	 public void deleteChange(Integer id){
		 jdbcTemplate.execute("DELETE FROM "+LOCAL_CHANGES_TABLE+" WHERE id="+id);
	 }
	 
	 public void deleteChangeByComposition(String composition){
		 jdbcTemplate.update("DELETE FROM "+LOCAL_CHANGES_TABLE+" WHERE composition=?",composition);
	 }
	 
	 public void deleteAllChanges(){
		 jdbcTemplate.execute("DELETE FROM "+LOCAL_CHANGES_TABLE);
	 }
	 
	 private LocalChange getLocalChange(Map<String,Object> row){
		 LocalChange change= new LocalChange();
    	 change.setChangeId(new Integer(row.get("id").toString()));
    	 change.setDate(row.get("date").toString());
    	 change.setDescription(row.get("description").toString());
    	 if(row.get("accepted")!=null) change.setAccepted(new Integer(row.get("accepted").toString()));
    	 change.setFragment(row.get("fragment").toString());
    	 change.setComposition(row.get("composition").toString());
    	 change.setXml(row.get("xml").toString());
    	 change.setDirtyXml(row.get("dirtyxml").toString());

    	 return change;
	 }
	 
	
}
