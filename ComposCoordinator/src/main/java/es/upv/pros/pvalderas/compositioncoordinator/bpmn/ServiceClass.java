package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import java.io.IOException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.el.FixedValue;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.springframework.stereotype.Component;

import es.upv.pros.pvalderas.compositioncoordinator.http.HTTPClient;

@Component
public class ServiceClass implements JavaDelegate
{
	FixedValue url;
	FixedValue method;
    
    public void execute(final DelegateExecution execution) throws IOException {
    	String results="";
    	switch(method.getExpressionText()){
	    	case "GET": results=HTTPClient.get(url.getExpressionText()); break;
	    	case "POST": results=HTTPClient.post(url.getExpressionText(),"",true,"text"); break;
    	}
    		
        System.out.println("Execution of "+execution.getCurrentActivityName()+": "+results);
    }
}