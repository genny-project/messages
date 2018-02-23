package life.genny.util;

import java.io.IOException;

import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Export;

import life.genny.qwandautils.GoogleDocs;

public class GoogleDocHelper {
	
	public static String getGoogleDocString(String docId) {
		// Build a new authorized API client service.
        Drive service;
        String htmlString = null;
        try {
			service = GoogleDocs.getDriveService();
				
			Export export = service.files().export(docId, "text/html");
	        HttpResponse response = export.executeMedia();
	         
	        htmlString = response.parseAsString();
	        			
		} catch (IOException e) {
			e.printStackTrace();
		}

       return htmlString;
	
	}
	
	
	public static String getGoogleDocPlainString(String docId) {
		// Build a new authorized API client service.
        Drive service;
        String htmlString = null;
		try {
			service = GoogleDocs.getDriveService();
				
			Export export = service.files().export(docId, "text/plain");
	        HttpResponse response = export.executeMedia();
	         
	        htmlString = response.parseAsString();
	        			
		} catch (IOException e) {
			e.printStackTrace();
		}

       return htmlString;
       
       
       
		
	}

}
