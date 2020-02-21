package es.upv.pros.pvalderas.fragmentmanager.http;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPClient {
	

	public static String get(String url) throws IOException{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		
		BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		con.disconnect();
		return response.toString();
	}
	
	public static String post(String url, String data, boolean responseProcess, String dataType) throws IOException{
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", dataType);
		con.setDoOutput(true);
		
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(wr, "UTF-8"));
		writer.write(data);
		writer.flush();
		writer.close();
		wr.close();
		
		con.getResponseCode();
		
		if(responseProcess){
			BufferedReader in = new BufferedReader( new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			
			while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			}
			in.close();
			
			return response.toString();
		}
		
		con.disconnect();

		return null;
	}
	
}
