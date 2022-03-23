package es.upv.pros.pvalderas.compositioncoordinator.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DAO {

	@Autowired
	private DAOFragments fragments;
	
	@Autowired
	private DAOMicroservices microservices;
	
	@Autowired
	private DAOLocalChanges localChanges;
	
	@Autowired
	private DAOParticipantChanges participantChanges;

	public DAOFragments getFragments() {
		return fragments;
	}

	public DAOMicroservices getMicroservices() {
		return microservices;
	}

	public DAOLocalChanges getLocalChanges() {
		return localChanges;
	}

	public DAOParticipantChanges getParticipantChanges() {
		return participantChanges;
	}
	
	
}
