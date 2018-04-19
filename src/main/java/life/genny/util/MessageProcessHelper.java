package life.genny.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.verticle.utils.VertxUtils;
import io.vertx.rxjava.core.eventbus.EventBus;

public class MessageProcessHelper {

	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_RED = "\u001B[31m";

	static QMessageFactory messageFactory = new QMessageFactory();

	public static void processTestMessage(QMSGMessage message, String token, EventBus eventBus) {

		BaseEntity be = getBaseEntityForCode(message, token); 
		
		if(be != null) {
			System.out.println("got the deserialized baseentity");
			//set message template with template (which was recieved as an answer in the Message-layout) from the TST_COMM template class
			//message.setTemplate_code(be.findEntityAttribute("TST_TEMPLATE_CODE").get().getValueString());
			message.setTemplate_code(MergeUtil.getBaseEntityAttrValueAsString(be, "TST_TEMPLATE_CODE"));
			
			Map<String, Object> templateBaseEntityMap = getBaseEntityLinks(be, message, token);

			System.out.println("template baseEntity map ::" + templateBaseEntityMap);

			QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
			QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntityMap, "OWNER", token);

			if (msgMessage != null) {
				logger.info(ANSI_BLUE + ">>>>>>>>>>Message info is set<<<<<<<<<<<<" + ANSI_RESET);
				provider.sendMessage(msgMessage, eventBus, templateBaseEntityMap);
			} else {
				System.out.println(
						ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
			}
		} else {
			System.out.println("base entity is null");
		}
		
		

	}

	private static BaseEntity getBaseEntityForCode(QMSGMessage message, String token) {
		
		//Code is the Test-Message BaseEntity code
		BaseEntity be = MergeUtil.getBaseEntityForAttr(message.getCode(), token);
		return be;
	}

	private static Map<String, Object> getBaseEntityLinks(BaseEntity be, QMSGMessage message, String token) {
		
		Map<String, Object> templateBaseEntityMap = new HashMap<>();

		be.getBaseEntityAttributes().forEach(attribute -> {
			switch (attribute.getAttributeCode()) {
			case "TST_ALIAS1_ALIAS":
				String value = MergeUtil.getBaseEntityAttrValueAsString(be, "TST_ALIAS1_CODE");
				BaseEntity aliasEntity = MergeUtil.getBaseEntityForAttr(value, token);
				templateBaseEntityMap.put(attribute.getValueString(), aliasEntity);
				/*templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS1_CODE").get().getValueString(), token));*/
				break;
			case "TST_ALIAS2_ALIAS":
				templateBaseEntityMap.put(attribute.getValueString(), MergeUtil.getBaseEntityForAttr(MergeUtil.getBaseEntityAttrValueAsString(be, "TST_ALIAS2_CODE"), token));
				/*templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS2_CODE").get().getValueString(), token));*/
				break;
			case "TST_ALIAS3_ALIAS":
				templateBaseEntityMap.put(attribute.getValueString(), MergeUtil.getBaseEntityForAttr(MergeUtil.getBaseEntityAttrValueAsString(be, "TST_ALIAS3_CODE"), token));
				/*templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS3_CODE").get().getValueString(), token));*/
				break;
			}

		});
		
		templateBaseEntityMap.put(be.getName(), be);
		
		return templateBaseEntityMap;
	}

	public static void processMessage(QMSGMessage message, String token, EventBus eventBus) {

		Map<String, String> keyEntityAttrMap = MergeHelper.getKeyEntityAttrMap(message);

		if (keyEntityAttrMap.containsKey("code")) {
			
			Map<String, BaseEntity> map = QwandaUtils
					.getBaseEntWithChildrenForAttributeCode(keyEntityAttrMap.get("code"), token);
			Map<String, Object> templateBaseEntMap = new HashMap<String, Object>(map);

			if (templateBaseEntMap != null && !templateBaseEntMap.isEmpty()) {
				logger.info(ANSI_BLUE + "template base entity map ::" + templateBaseEntMap + ANSI_RESET);
				triggerMessage(message, templateBaseEntMap, keyEntityAttrMap.get("recipient").toString(), token, eventBus);
			}
		}

	}

	/**
	 * 
	 * @param message
	 * @param templateBaseEntMap
	 * @param recipient
	 * @param token
	 *            <p>
	 *            Based on the message provider (SMS/Email/Voice), information
	 *            for message will be set and message will be triggered
	 *            </p>
	 */
	private static void triggerMessage(QMSGMessage message, Map<String, Object> templateBaseEntMap,
			String recipient, String token, EventBus eventBus) {
		
		QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
		QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntMap, recipient, token);

		if (msgMessage != null) {
			logger.info(ANSI_BLUE + ">>>>>>>>>>Message info is set<<<<<<<<<<<<" + ANSI_RESET);
			provider.sendMessage(msgMessage, eventBus, templateBaseEntMap);
		} else {
			System.out.println(
					ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
		}

	}

	public static void processGenericMessage(QMessageGennyMSG message, String tokenString, EventBus eventbus) {
		
		
		System.out.println("message model ::"+message.toString());
		
		//Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(message, tokenString);
		
		//Iterate through each recipient in recipientArray, Set Message and Trigger Message
		String[] recipientArr = message.getRecipientArr();
		System.out.println("recipient array ::"+recipientArr.toString());
		QBaseMSGMessage msgMessage = null;
		Map<String, Object> newMap = null;
		
		if(recipientArr != null && recipientArr.length > 0) {
			
			logger.info("recipient array length ::"+recipientArr.length);
			
			for(String recipientCode : recipientArr) {
				
				newMap = new HashMap<>();
				newMap = baseEntityContextMap;
				
				BaseEntity recipientBeFromDDT = VertxUtils.readFromDDT(recipientCode, tokenString);
				
				//BaseEntity recipientBe = MergeUtil.getBaseEntityForAttr(recipientCode, tokenString);
				newMap.put("RECIPIENT", recipientBeFromDDT);
				logger.info("new map ::"+newMap);
				
				//Setting Message values
				msgMessage = new QBaseMSGMessage();
				
				//Get Message Provider
				QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
				
				msgMessage = provider.setGenericMessageValue(message, newMap, tokenString);
				
				//Triggering message
				if (msgMessage != null) {
					logger.info(ANSI_BLUE + ">>>>>>>>>>Message info is set<<<<<<<<<<<<" + ANSI_RESET);
					provider.sendMessage(msgMessage, eventbus, newMap);
				} else {
					logger.error(
							ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
				}
				
			} 
			
		} else {
			logger.error(ANSI_RED+"  RECIPIENT NULL OR EMPTY  "+ANSI_RESET);
		}
		
	}

	private static Map<String, Object> createBaseEntityContextMap(QMessageGennyMSG message, String tokenString) {
		
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		
		for (Map.Entry<String, String> entry : message.getMessageContextMap().entrySet())
		{
		    System.out.println(entry.getKey() + "/" + entry.getValue());
		    //baseEntityContextMap.put(entry.getKey().toUpperCase(), MergeUtil.getBaseEntityForAttr(entry.getValue(), tokenString));
		    
		    String value = entry.getValue();
		    BaseEntity be = VertxUtils.readFromDDT(value, tokenString);
		    if(be != null) {
			    baseEntityContextMap.put(entry.getKey().toUpperCase(), be);
		    }
		    else {
			    baseEntityContextMap.put(entry.getKey().toUpperCase(), value);
		    }
		}
		
		return baseEntityContextMap;
	}

}
