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
import life.genny.qwandautils.MergeUtil;

public class MessageProcessHelper {

	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_RED = "\u001B[31m";

	static QMessageFactory messageFactory = new QMessageFactory();

	public static void processTestMessage(QMSGMessage message, String token) {

		BaseEntity be = getBaseEntityForCode(message, token);
		
		if(be != null) {
			
			//set message template with template (which was recieved as an answer in the Message-layout) from the TST_COMM template class
			message.setTemplate_code(be.findEntityAttribute("TST_TEMPLATE_CODE").get().getValueString());
			
			
			Map<String, BaseEntity> templateBaseEntityMap = getBaseEntityLinks(be, message, token);

			System.out.println("template baseEntity map ::" + templateBaseEntityMap);

			QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
			QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntityMap, "OWNER", token);

			if (msgMessage != null) {
				logger.info(ANSI_BLUE + ">>>>>>>>>>Message info is set<<<<<<<<<<<<" + ANSI_RESET);
				provider.sendMessage(msgMessage);
			} else {
				System.out.println(
						ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
			}
		}
		
		

	}

	private static BaseEntity getBaseEntityForCode(QMSGMessage message, String token) {
		BaseEntity be = MergeUtil.getBaseEntityForAttr(message.getCode(), token);
		return be;
	}

	private static Map<String, BaseEntity> getBaseEntityLinks(BaseEntity be, QMSGMessage message, String token) {
		
		Map<String, BaseEntity> templateBaseEntityMap = new HashMap<>();

		be.getBaseEntityAttributes().forEach(attribute -> {
			switch (attribute.getAttributeCode()) {
			case "TST_ALIAS1_ALIAS":
				templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS1_CODE").get().getValueString(), token));
				break;
			case "TST_ALIAS2_ALIAS":
				templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS2_CODE").get().getValueString(), token));
				break;
			case "TST_ALIAS3_ALIAS":
				templateBaseEntityMap.put(attribute.getValueString(), MergeUtil
						.getBaseEntityForAttr(be.findEntityAttribute("TST_ALIAS3_CODE").get().getValueString(), token));
				break;
			}

		});
		return templateBaseEntityMap;
	}

	public static void processMessage(QMSGMessage message, String token) {

		Map<String, String> keyEntityAttrMap = MergeHelper.getKeyEntityAttrMap(message);

		if (keyEntityAttrMap.containsKey("code")) {
			Map<String, BaseEntity> templateBaseEntMap = MergeUtil
					.getBaseEntWithChildrenForAttributeCode(keyEntityAttrMap.get("code"), token);

			if (templateBaseEntMap != null && !templateBaseEntMap.isEmpty()) {
				logger.info(ANSI_BLUE + "template base entity map ::" + templateBaseEntMap + ANSI_RESET);
				triggerMessage(message, templateBaseEntMap, keyEntityAttrMap.get("recipient").toString(), token);
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
	private static void triggerMessage(QMSGMessage message, Map<String, BaseEntity> templateBaseEntMap,
			String recipient, String token) {
		QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
		QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntMap, recipient, token);

		if (msgMessage != null) {
			logger.info(ANSI_BLUE + ">>>>>>>>>>Message info is set<<<<<<<<<<<<" + ANSI_RESET);
			provider.sendMessage(msgMessage);
		} else {
			System.out.println(
					ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
		}

	}

}
