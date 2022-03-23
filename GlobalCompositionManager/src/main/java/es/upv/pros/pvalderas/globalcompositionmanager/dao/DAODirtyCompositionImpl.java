package es.upv.pros.pvalderas.globalcompositionmanager.dao;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedModel;

@Component
class DAODirtyCompositionImpl implements DAODirtyComposition{
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	 
	private final String DIRTY_COMPOSITIONS_TABLE="dirty_compositions";

	public void createTables(){
        jdbcTemplate.execute("DROP TABLE "+DIRTY_COMPOSITIONS_TABLE+" IF EXISTS");
        jdbcTemplate.execute("CREATE TABLE "+DIRTY_COMPOSITIONS_TABLE+"(id VARCHAR(255) PRIMARY KEY, xml CLOB, xmlToConfirm CLOB, affectedParticipants VARCHAR(1024), totalParticipants TINYINT, modifiedParticipant VARCHAR(128), responses TINYINT, confirmed TINYINT)");
	}
	
	public void save(String composition, String xml, String xmlToConfirm, String affectedParticipants, Integer totalParticipants, String modifiedParticipant){
		jdbcTemplate.update("DELETE FROM "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
		jdbcTemplate.update("INSERT INTO "+DIRTY_COMPOSITIONS_TABLE+"(id, xml, xmlToConfirm, affectedParticipants,  totalParticipants, modifiedParticipant, responses, confirmed) VALUES (?,?,?,?,?,?,0,-1)", composition, xml, xmlToConfirm, affectedParticipants,  totalParticipants, modifiedParticipant);
	}
	
	public void updateBPMNAndAccept(String composition, String xml, String xmlToConfirm){
		jdbcTemplate.update("UPDATE "+DIRTY_COMPOSITIONS_TABLE+" SET xml=?, xmlToConfirm=?, confirmed=1 WHERE id=?",xml, xmlToConfirm, composition);
	}
	
	public void unAccept(String composition){
		jdbcTemplate.update("UPDATE "+DIRTY_COMPOSITIONS_TABLE+" SET confirmed=-2 WHERE id=?", composition);
	}
	
	public void addResponse(String composition){
		jdbcTemplate.update("UPDATE "+DIRTY_COMPOSITIONS_TABLE+" SET responses=responses+1 WHERE id=?", composition);
	}
	
	public void confirm(String composition){
		jdbcTemplate.update("UPDATE "+DIRTY_COMPOSITIONS_TABLE+" SET confirmed=? WHERE id=?",2,composition);
	}
	
	public void reject(String composition){
		jdbcTemplate.update("UPDATE "+DIRTY_COMPOSITIONS_TABLE+" SET confirmed=? WHERE id=?",0,composition);
	}
	
	public void delete(String composition){
		jdbcTemplate.update("DELETE "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
	}
	
	public String getBPMN(String composition){
		 try{
			Map<String,Object> row = jdbcTemplate.queryForMap("SELECT xml FROM "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
			return (String)row.get("xml");
		 }catch(EmptyResultDataAccessException e){
	        return null;
	     }
	}
	
	public String getToConfirmBPMN(String composition){
		 try{
			Map<String,Object> row = jdbcTemplate.queryForMap("SELECT xmlToConfirm FROM "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
			return (String)row.get("xmlToConfirm");
		 }catch(EmptyResultDataAccessException e){
	        return null;
	     }
	}
	
	public AdaptedModel getAdaptedModelWithouBPMN(String composition) {
		 try{
			Map<String,Object> row = jdbcTemplate.queryForMap("SELECT id, affectedParticipants, totalParticipants, modifiedParticipant, responses, confirmed FROM "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
			return getModelWithOutBPMN(row);
		 }catch(EmptyResultDataAccessException | JSONException e){
	        return null;
	     }
	}
	
	
	public AdaptedModel getAdaptedModel(String composition) {
		 try{
			Map<String,Object> row = jdbcTemplate.queryForMap("SELECT * FROM "+DIRTY_COMPOSITIONS_TABLE+" WHERE id=?",composition);
			AdaptedModel adaptedModel=getModelWithOutBPMN(row);
			
			adaptedModel.setBpmn((String)row.get("xml"));
			adaptedModel.setBpmnToConfirm((String)row.get("xmlToConfirm"));
			
			return adaptedModel;
		 }catch(EmptyResultDataAccessException | JSONException e){
	        return null;
	     }
	}
	
	private AdaptedModel getModelWithOutBPMN(Map<String,Object> row) throws JSONException{
		AdaptedModel adaptedModel=new AdaptedModel();
		
		adaptedModel.setAffectedParticipantsJSON((String)row.get("affectedParticipants"));
		adaptedModel.setComposition((String)row.get("id"));
		adaptedModel.setTotalParticipants(Integer.parseInt(row.get("totalParticipants").toString()));
		adaptedModel.setModifiedParticipant((String)row.get("modifiedParticipant"));
		adaptedModel.setConfirmed(Integer.parseInt(row.get("confirmed").toString())); 
		adaptedModel.setResponses(Integer.parseInt(row.get("responses").toString()));
		
		return adaptedModel;
	}
	
	public List<Map<String,Object>> getAll(){
		 try{
			List<Map<String,Object>> rows = jdbcTemplate.queryForList("SELECT * FROM "+DIRTY_COMPOSITIONS_TABLE);
			return rows;
		 }catch(EmptyResultDataAccessException e){
	        return null;
	     }
	}
}
