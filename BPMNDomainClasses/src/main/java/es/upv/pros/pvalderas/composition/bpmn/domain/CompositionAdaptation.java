package es.upv.pros.pvalderas.composition.bpmn.domain;

public class CompositionAdaptation {
		private String composition;
		private Integer changeId;
		private Integer type;
		private String microservice;
		private String adaptedModel;
		
		public String getComposition() {
			return composition;
		}
		public void setComposition(String composition) {
			this.composition = composition;
		}
		public Integer getChangeId() {
			return changeId;
		}
		public void setChangeId(Integer changeId) {
			this.changeId = changeId;
		}
		public Integer getType() {
			return type;
		}
		public void setType(Integer type) {
			this.type = type;
		}
		public String getMicroservice() {
			return microservice;
		}
		public void setMicroservice(String microservice) {
			this.microservice = microservice;
		}
		public String getAdaptedModel() {
			return adaptedModel;
		}
		public void setAdaptedModel(String adaptedModel) {
			this.adaptedModel = adaptedModel;
		}
		
		
}
