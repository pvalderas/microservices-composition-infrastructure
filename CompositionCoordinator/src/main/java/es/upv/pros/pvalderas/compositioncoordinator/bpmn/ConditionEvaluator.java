package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import java.util.Scanner;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class ConditionEvaluator implements ExecutionListener {
	
	public void notify(DelegateExecution execution) throws Exception {
	  
		Scanner teclado=new Scanner(System.in);
		
        System.out.println(execution.getCurrentActivityName());
        String value=teclado.nextLine();
        
        String property=execution.getCurrentActivityName();
        	property=property.substring(0, 1).toLowerCase()+property.substring(1).replaceAll(" ", "").replaceAll("\\?", "");   
		
		execution.setVariable(property, value);
		
    }
	
 }