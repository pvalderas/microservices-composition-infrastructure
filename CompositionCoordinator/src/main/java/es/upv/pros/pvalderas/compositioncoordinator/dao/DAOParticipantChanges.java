package es.upv.pros.pvalderas.compositioncoordinator.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.ParticipantChange;

@Component
public class DAOParticipantChanges {
	
	protected static final String PARTICIPANT_CHANGES_TABLE="participant_changes";

	 @Autowired
	 private JdbcTemplate jdbcTemplate;
	 
	 public void createTable(){
		jdbcTemplate.execute("DROP TABLE "+PARTICIPANT_CHANGES_TABLE+" IF EXISTS");
        jdbcTemplate.execute("CREATE TABLE "+PARTICIPANT_CHANGES_TABLE+"(microservice VARCHAR(255), id INT, composition VARCHAR(255), changes VARCHAR(1024), dirtyxml CLOB, xmlToConfirm CLOB, date VARCHAR(64), type TINYINT, accepted TINYINT, PRIMARY KEY(microservice, id))");
	 }
	 
	 
	 public void save(String microservice, Integer changeId, String composition, String changes, String dirtyXML, String xmlToConfirm, Integer type){

		 jdbcTemplate.execute("DELETE FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE id="+changeId+" AND microservice='"+microservice+"'");
		 jdbcTemplate.update("INSERT INTO "+PARTICIPANT_CHANGES_TABLE+" (microservice, id, composition, changes, dirtyxml, xmlToConfirm, date, type, accepted) VALUES (?,?,?,?,?,?,?,?,NULL)", microservice, changeId, composition, changes, dirtyXML, xmlToConfirm, new Date().toString(), type);
	 }
	 
	 public List<ParticipantChange> getNonAccepted(String composition){
		 List<Map<String,Object>> result=jdbcTemplate.queryForList("SELECT * FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE accepted is NULL AND composition='"+composition+"'");
         
         List<ParticipantChange> changes=new ArrayList<ParticipantChange>();
         for(Map<String,Object> row:result){
        	 changes.add(getParticipantChange(row));
         }
         
         return changes;
	 }
	 
	 public JSONArray getNonAcceptedJSON(String composition) throws JSONException{
		 List<Map<String,Object>> result=jdbcTemplate.queryForList("SELECT * FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE accepted is NULL AND composition='"+composition+"'");
         
         JSONArray changes=new JSONArray();
         for(Map<String,Object> row:result){
        	 changes.put(getParticipantChangeJSON(row));
         }
         
         return changes;
	 }
	 
	 public JSONArray getAllJSONByComposition(String composition) throws JSONException{
		 List<Map<String,Object>> result=jdbcTemplate.queryForList("SELECT * FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE composition='"+composition+"'");
         
         JSONArray changes=new JSONArray();
         for(Map<String,Object> row:result){
        	 changes.put(getParticipantChangeJSON(row));
         }
         
         return changes;
	 }
	 
	 public JSONObject getJSON(String microservice, Integer id) throws JSONException{
		 Map<String,Object> row=jdbcTemplate.queryForMap("SELECT * FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE microservice=? AND id=?",microservice, id);
         return getParticipantChangeJSON(row);
	 }
	 
	 
	 
	 public ParticipantChange get(String microservice, Integer id) throws JSONException{
		 Map<String,Object> row=jdbcTemplate.queryForMap("SELECT * FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE microservice=? AND id=?",microservice, id);
         return getParticipantChange(row);
	 }
	 
	 
	 public void delete(String microservice, Integer id){
		 jdbcTemplate.execute("DELETE FROM "+PARTICIPANT_CHANGES_TABLE+" WHERE microservice='"+microservice+"' AND id="+id);
	 }
	 

	 
	 
	 
	 private ParticipantChange getParticipantChange(Map<String,Object> row){
		 ParticipantChange change= new ParticipantChange();
		 change.setMicroservice(row.get("microservice").toString());
    	 change.setId(new Integer(row.get("id").toString()));
    	 change.setComposition(row.get("composition").toString());
    	 change.setChanges(row.get("changes").toString());
    	 change.setDirtyXml(row.get("dirtyxml").toString());
    	 change.setXmlToConfirm(row.get("xmlToConfirm").toString());
    	 change.setDate(row.get("date").toString());
    	 if(row.get("accepted")!=null) change.setAccepted(new Integer(row.get("accepted").toString()));
    	 return change;
	 }
	 
	 private JSONObject getParticipantChangeJSON(Map<String,Object> row) throws JSONException{
		 JSONObject change= new JSONObject();
		 change.put("microservice",row.get("microservice").toString());
    	 change.put("id",new Integer(row.get("id").toString()));
    	 change.put("composition",row.get("composition").toString());
    	 change.put("changes",row.get("changes").toString());
    	 change.put("dirtyXml",row.get("dirtyxml").toString());
    	 change.put("xmlToConfirm",row.get("xmlToConfirm").toString());
    	 change.put("date",row.get("date").toString());
    	 if(row.get("accepted")!=null) change.put("accepted",Integer.parseInt(row.get("accepted").toString()));
    	 return change;
	 }
	 
	 public void accept(String microservice, Integer changeId){
		 jdbcTemplate.update("UPDATE "+PARTICIPANT_CHANGES_TABLE+" SET accepted=1 WHERE microservice=? AND id=?",microservice, changeId);
	 }
	 
	 public void reject(String microservice, Integer changeId){
		 jdbcTemplate.update("UPDATE "+PARTICIPANT_CHANGES_TABLE+" SET accepted=0 WHERE microservice=? AND id=?",microservice, changeId);
	 }
	 
	 public void confirmByComposition(String composition){
		 jdbcTemplate.update("UPDATE "+PARTICIPANT_CHANGES_TABLE+" SET accepted=2 WHERE composition=?",composition);
	 } 
	 
	 public void rejectByComposition(String composition){
		 jdbcTemplate.update("UPDATE "+PARTICIPANT_CHANGES_TABLE+" SET accepted=0 WHERE composition=?",composition);
	 } 
}
