package life.genny.util;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import life.genny.qwanda.converter.PropertiesJsonDeserializer;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.QwandaUtils;

public class MergeHelper {
	
	/*private static final Gson gson = new GsonBuilder()
	        .registerTypeAdapter(Properties.class, PropertiesJsonDeserializer.getPropertiesJsonDeserializer())
	        .create();*/
	
	
	
	
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
					if(obj.get("linkValue") != null){
						entityTemplateContextMap.put(obj.get("linkValue").toString(),getBaseEntityForAttr(baseEntAttributeCode, token));
					}
					//BaseEntity be = getBaseEntityForAttr("PER_USER2", token); //this is for testing 
				});
						
			}		

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		System.out.println("base entity context map ::"+entityTemplateContextMap);
		return entityTemplateContextMap;

	}

	public static BaseEntity getBaseEntityForAttr(String baseEntAttributeCode, String token) {
		
		String qwandaServiceUrl = System.getenv("REACT_APP_QWANDA_API_URL");
		String attributeString;
		BaseEntity be = null;
		try {
			attributeString = QwandaUtils
					.apiGet(qwandaServiceUrl + "/qwanda/baseentitys/" +baseEntAttributeCode, token);
			
			System.out.println("attribute string ::"+attributeString);
			
			final Gson gson = new GsonBuilder()
			        .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
			          @Override
			          public LocalDateTime deserialize(final JsonElement json, final Type type,
			              final JsonDeserializationContext jsonDeserializationContext)
			              throws JsonParseException {
			            return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()).toLocalDateTime();
			          }

			          public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
			              final JsonSerializationContext context) {
			            return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // "yyyy-mm-dd"
			          }
			        }).create();
					
			//not deserializing 
			be = gson.fromJson(attributeString, BaseEntity.class);
			
			System.out.println("baseentity ::"+be);
				
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		return be;
	}
	
	public static String getBaseEntityAttrValue(BaseEntity be, String attributeCode) {
		return be.findEntityAttribute(attributeCode).get().getValueString();	
	}

}
