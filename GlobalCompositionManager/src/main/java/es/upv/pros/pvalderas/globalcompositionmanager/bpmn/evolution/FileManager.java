package es.upv.pros.pvalderas.globalcompositionmanager.bpmn.evolution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class FileManager {

	public String saveCompositionFile(String id, String xml) throws FileNotFoundException, UnsupportedEncodingException{
		 String fileName="compositions"+File.separator+id+".bpmn";
        File fichero=new File(fileName);
		 PrintWriter writer = new PrintWriter(fichero, "UTF-8");
		 writer.print(xml);
		 writer.close();
		 return fileName;
	 }
	 
	public String getCompositionFromFile(String fileName) throws IOException{
		 String xml=new String(Files.readAllBytes(Paths.get(fileName))); 
		 return xml;
	 }
}
