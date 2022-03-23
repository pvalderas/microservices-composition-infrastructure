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
class DAOCompositionImpl implements DAOComposition{
	
	 @Autowired
	 JdbcTemplate jdbcTemplate;
	 
	private final String COMPOSITIONS_TABLE="compositions";

	public void createTables(){
		jdbcTemplate.execute("DROP TABLE "+COMPOSITIONS_TABLE+" IF EXISTS");
        jdbcTemplate.execute("CREATE TABLE "+COMPOSITIONS_TABLE+"(id VARCHAR(255) PRIMARY KEY, name VARCHAR(255), file VARCHAR(1024))");
	}
	
	public void save(String id, String name, String file){
		jdbcTemplate.update("DELETE FROM "+COMPOSITIONS_TABLE+" WHERE id=?",id);
		jdbcTemplate.update("INSERT INTO "+COMPOSITIONS_TABLE+"(id, name, file) VALUES (?,?, ?)", id, name, file);
	}
	
	public String getFile(String composition){
		 Map<String,Object> row=jdbcTemplate.queryForList("SELECT file FROM "+COMPOSITIONS_TABLE+" WHERE id=?",composition).get(0);
		 return (String)row.get("file");
	}
	
	public Object get(String id){
		return jdbcTemplate.queryForList("SELECT * FROM "+COMPOSITIONS_TABLE+" WHERE id=?",id).get(0);
	}
	
	public List<Map<String, Object>> getAll(){
		return jdbcTemplate.queryForList("SELECT * FROM "+COMPOSITIONS_TABLE);
	}
	
}
