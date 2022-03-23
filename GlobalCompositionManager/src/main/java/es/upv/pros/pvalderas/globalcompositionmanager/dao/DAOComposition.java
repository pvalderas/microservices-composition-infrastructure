package es.upv.pros.pvalderas.globalcompositionmanager.dao;

import java.util.List;
import java.util.Map;

public interface DAOComposition {
	public void createTables();
	
	public void save(String id, String name, String file);
	
	public String getFile(String composition);
	
	public Object get(String id);
	
	public List<Map<String, Object>> getAll();
}
