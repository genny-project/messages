package life.genny.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.GennySheets;

public class MergeHelper {
	
	private static String messagesSheetId = System.getenv("MESSAGES_SHEETID");
	private final static String secret = System.getenv("GOOGLE_CLIENT_SECRET");
	private final static String hostingSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
	static File credentialPath = new File(System.getProperty("user.home"), ".genny/sheets.googleapis.com-java-quickstart"); 

	public static Map<String, String> getKeyEntityAttrMap(QMSGMessage message) {
		
		String[] messageDataArr = message.getMsgMessageData();
		Map<String, String> keyEntityMap = new HashMap<>();
		if (messageDataArr != null && messageDataArr.length > 0) {

			for (String data : messageDataArr) {

				String[] dataEntity = StringUtils.split(data, ":");
				if (dataEntity.length > 0) {

					keyEntityMap.put(dataEntity[0], dataEntity[1]);

				}

			}

		}
		System.out.println("key entity map ::" + keyEntityMap);
		return keyEntityMap;
	}
	
	
	public static Map getTemplate(String templateCode) {
		GennySheets sheets = new GennySheets(secret, messagesSheetId, credentialPath);
		
		Map<String, Map> templateMap = getMessageTemplates(sheets);
		System.out.println("template map ::"+templateMap);
		
		return templateMap.get(templateCode);
	}
	
	
	public static Map<String, Map> getMessageTemplates(GennySheets sheets) {
	    List<Map> obj = new ArrayList<Map>();
	    try {
	      obj = sheets.row2DoubleTuples("Messages");
	    } catch (final IOException e) {
	      e.printStackTrace();
	    }
	    return obj.stream().map(object -> {
	      final Map<String, Map> map = new HashMap<String, Map>();
	      final String code = (String) object.get("code");
	      final String description = (String) object.get("description");
	      final String subject = (String) object.get("subject");
	      final String email = (String) object.get("email");
	      final String sms = (String) object.get("sms");
	      Map<String, String> fields = new HashMap<String, String>();
	      fields.put("code", code);
	      fields.put("description", description);
	      fields.put("subject", subject);
	      fields.put("email", email);
	      fields.put("sms", sms);
	      map.put(code, fields);
	      System.out.println("**********************templates*****************************");
	      System.out.println(map);
	      return map;
	    }).reduce((ac, acc) -> {
	      ac.putAll(acc);
	      return ac;
	    }).get();
	  }

}
