package es.upv.pros.pvalderas.globalcompositionmanager.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DAO {
	
	@Autowired
	private DAOComposition composition;
	
	@Autowired
	private DAODirtyComposition dirtyComposition;

	public DAOComposition getComposition() {
		return composition;
	}

	public void setComposition(DAOCompositionImpl composition) {
		this.composition = composition;
	}

	public DAODirtyComposition getDirtyComposition() {
		return dirtyComposition;
	}

	public void setDirtyComposition(DAODirtyCompositionImpl dirtyComposition) {
		this.dirtyComposition = dirtyComposition;
	}
	 
	 
}
