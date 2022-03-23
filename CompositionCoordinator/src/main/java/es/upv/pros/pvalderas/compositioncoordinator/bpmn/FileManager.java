package es.upv.pros.pvalderas.compositioncoordinator.bpmn;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class FileManager {
	
    @Autowired
    private ResourcePatternResolver resourceLoader;

	public String getFragmentDir(String compo){
		File dir=new File("fragments");
    	String compositions[]=dir.list();
    	for(String compoDir:compositions){
    		if(compoDir.charAt(0)!='.' && compoDir.indexOf(compo)>=0){
	    		return "fragments/"+compoDir;
    		}
    	}
    	return null;
	}
	
	public String getBPMNFileContent(String compo) throws IOException{
		String fragmentDir=getFragmentDir(compo);
	    if(fragmentDir!=null){
	    	final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/"+fragmentDir+"/*.bpmn");
	    	return new String(Files.readAllBytes(Paths.get(resources[0].getURI())));
	    }else{
	    	return "";
	    }
	}
	
	public File getBPMNFile(String compo) throws IOException{
		String fragmentDir=getFragmentDir(compo);
	    if(fragmentDir!=null){
	    	final Resource[] resources = this.resourceLoader.getResources("file:" + System.getProperty("user.dir") + "/"+fragmentDir+"/*.bpmn");
	    	return resources[0].getFile();
	    }else{
	    	return null;
	    }
	}
	
}
