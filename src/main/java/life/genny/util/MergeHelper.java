package life.genny.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.QwandaUtils;

public class MergeHelper {
	
	
	/*public Map<String, BaseEntity> mergeHelper(QMSGMessage message) {

		Map<String, BaseEntity> entityTemplateMap = new HashMap<String, BaseEntity>();
		Map<String, String> keyEntityMap = getKeyEntityAttrMap(message);

		keyEntityMap.forEach((k, v) -> {
			System.out.println("k and v" + k + "," + v);
			//service.findBaseEntityById(k);
			//entityTemplateMap.put(k, service.findBaseEntityByCode(v));
		});

		return entityTemplateMap;

	}*/

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

	@SuppressWarnings("unchecked")
	public static Map<String, BaseEntity> getBaseEntWithChildrenForAttributeCode(String attributeCode, String token) {

		String qwandaServiceUrl = System.getenv("REACT_APP_QWANDA_API_URL");
		System.out.println("url ::" + qwandaServiceUrl);
		Map<String, BaseEntity> entityTemplateContextMap = new HashMap<String, BaseEntity>();


		try {
			String attributeString = QwandaUtils
					.apiGet(qwandaServiceUrl + "/qwanda/entityentitys/" + attributeCode + "/linkcodes/LNK_BEG/children", token);
			System.out.println("json for the baseEntity_group code ::" + attributeString);

			JSONParser parser = new JSONParser();
			JSONArray jsonarr;	

			if(attributeString != null && !attributeString.isEmpty()) {
				jsonarr = (JSONArray) parser.parse(attributeString);
				System.out.println("items >>>>>" + jsonarr);

				jsonarr.forEach(item -> {
					JSONObject obj = (JSONObject) item;
					String baseEntAttributeCode = (String) obj.get("targetCode");
					System.out.println("obj code >>>>" +baseEntAttributeCode);
					//Use this
					/*if(obj.get("linkValue") != null){
						entityTemplateContextMap.put(obj.get("linkValue").toString(),getBaseEntityForAttr(baseEntAttributeCode, token));
					}*/
					BaseEntity be = getBaseEntityForAttr("PER_USER2", token); //this is for testing 
				});
						
			}		

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return entityTemplateContextMap;

	}

	public static BaseEntity getBaseEntityForAttr(String baseEntAttributeCode, String token) {
		
		String qwandaServiceUrl = System.getenv("REACT_APP_QWANDA_API_URL");
		String attributeString;
		BaseEntity be = null;
		try {
			attributeString = QwandaUtils
					.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/" +baseEntAttributeCode, token);
						
			System.out.println("json for the baseEntity_group code ::" + attributeString);			
				
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return be;
	}
	
	public static String getBaseEntityAttrValue(BaseEntity be, String attributeCode) {
		return be.findEntityAttribute(attributeCode).get().getValueString();	
	}

}
