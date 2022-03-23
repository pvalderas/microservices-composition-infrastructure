package es.upv.pros.pvalderas.globalcompositionmanager.dao;

import java.util.List;
import java.util.Map;

import es.upv.pros.pvalderas.composition.bpmn.domain.AdaptedModel;

public interface DAODirtyComposition {
	
	public void createTables();
	
	public void save(String composition, String xml, String xmlToConfirm, String affectedParticipants, Integer totalParticipants, String modifiedParticipant);
	
	public void delete(String composition);
	
	public String getBPMN(String composition);
	public String getToConfirmBPMN(String composition);
	public AdaptedModel getAdaptedModelWithouBPMN(String composition);
	public AdaptedModel getAdaptedModel(String composition);
	public List<Map<String,Object>> getAll();
	
	public void updateBPMNAndAccept(String composition, String xml, String xmlToConfirm);
	public void unAccept(String composition);
	
	public void addResponse(String composition);
	
	public void confirm(String composition);
	public void reject(String composition);
	
}
