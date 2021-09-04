package life.genny.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.json.JSONObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.ANSIColour;
import life.genny.utils.VertxUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;

public class MessageProcessHelper {

	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static QMessageFactory messageFactory = new QMessageFactory();

	/**
	 * 
	 * @param message
	 * @param token
	 * @param eventbus
	 *            <p>
	 *            Based on the message provider (SMS/Email/Voice), information for
	 *            message will be set and message will be triggered
	 *            </p>
	 */
	public static void processGenericMessage(QMessageGennyMSG message, String token) {

		if (message == null) {
			log.error(ANSIColour.RED + "GENNY COM MESSAGE IS NULL" + ANSIColour.RESET);
		}

		log.info("message model ::" + message.toString());

		GennyToken userToken = new GennyToken(token);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		String realm = beUtils.getGennyToken().getRealm();
		log.info("Realm is " + realm);

		BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_"+realm.toUpperCase());

		// Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(beUtils, message);
		baseEntityContextMap.put("PROJECT", projectBe);

		String[] recipientArr = message.getRecipientArr();
		List<BaseEntity> recipientBeList = new ArrayList<BaseEntity>();

		BaseEntity templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());

		if (templateBe == null) {
			log.warn(ANSIColour.YELLOW + "No Template found for " + message.getTemplateCode() + ANSIColour.RESET);
		} else {
			log.info("Using TemplateBE " + templateBe.getCode());
		}

		List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());
		if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
			// Use default if told to do so
			List<String> typeList = beUtils.getBaseEntityCodeArrayFromLNKAttr(templateBe, "PRI_DEFAULT_MSG_TYPE");
			messageTypeList = typeList.stream().map(item -> QBaseMSGMessageType.valueOf(item)).collect(Collectors.toList());
		}

		Attribute emailAttr = RulesUtils.getAttribute("PRI_EMAIL", token);
		Attribute mobileAttr = RulesUtils.getAttribute("PRI_MOBILE", token);

		for (String recipient : recipientArr) {

			BaseEntity recipientBe = null;

			if (recipient.contains("[\"") && recipient.contains("\"]")) {
				// This is a BE Code
				String code = beUtils.cleanUpAttributeValue(recipient);
				recipientBe = beUtils.getBaseEntityByCode(code);
			} else {
				// Probably an actual email
				String code = "RCP_" + UUID.randomUUID().toString().toUpperCase();
				recipientBe = new BaseEntity(code, recipient);

				try {
					EntityAttribute email = new EntityAttribute(recipientBe, emailAttr, 1.0, recipient);
					recipientBe.addAttribute(email);
					EntityAttribute mobile = new EntityAttribute(recipientBe, mobileAttr, 1.0, recipient);
					recipientBe.addAttribute(mobile);
				} catch (Exception e) {
					log.error(e);
				}
			}

			if (recipientBe != null) {
				recipientBeList.add(recipientBe);
			} else {
				log.error(ANSIColour.RED + "Could not process recipient " + recipient + ANSIColour.RESET);
			}
		}

		BaseEntity unsubscriptionBe = beUtils.getBaseEntityByCode("COM_EMAIL_UNSUBSCRIPTION");
		log.info("unsubscribe be :: " + unsubscriptionBe);
		String templateCode = message.getTemplateCode() + "_UNSUBSCRIBE";


		/* Iterating and triggering email to each recipient individually */
		for (BaseEntity recipientBe : recipientBeList) {

			// Set our current recipient
			baseEntityContextMap.put("RECIPIENT", recipientBe);

			// Iterate our array of send types
			for (QBaseMSGMessageType msgType : messageTypeList) {

				/* Get Message Provider */
				QMessageProvider provider = messageFactory.getMessageProvider(msgType);

				/* check if unsubscription list for the template code has the userCode */
				Boolean isUserUnsubscribed = VertxUtils.checkIfAttributeValueContainsString(unsubscriptionBe,
						templateCode, recipientBe.getCode());

				if (provider != null) {
					/*
					 * if user is unsubscribed, then dont send emails. But toast and sms are still
					 * applicable
					 */
					if (isUserUnsubscribed && !msgType.equals(QBaseMSGMessageType.EMAIL)) {
						log.info("unsubscribed");
						provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
					}

					/* if subscribed, allow messages */
					if (!isUserUnsubscribed) {
						log.info("subscribed");
						provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
					}
				} else {
					log.error(ANSIColour.RED + ">>>>>> Provider is NULL for entity: " + ", msgType: " + msgType.toString() + " <<<<<<<<<" + ANSIColour.RESET);
				}
			}
		}
	}

	private static Map<String, Object> createBaseEntityContextMap(BaseEntityUtils beUtils, QMessageGennyMSG message) {
		
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		JSONObject decodedToken = KeycloakUtils.getDecodedToken(beUtils.getGennyToken().getToken());
		String realm = decodedToken.getString("aud");
		
		for (Map.Entry<String, String> entry : message.getMessageContextMap().entrySet())
		{
			String key = entry.getKey();
		    String value = entry.getValue();

			String logStr = "key: " + key + ", value: " + (key.toUpperCase().equals("PASSWORD") ? "REDACTED" : value);
			log.info(logStr);
		    
		    if ((value != null) && (value.length() > 4)) {

				// MUST CONTAIN A BE CODE
		    	if (value.matches("[A-Z]{3}\\_.*")) {
					// Create Array of Codes
					String[] codeArr = beUtils.cleanUpAttributeValue(value).split(",");

					// Convert to BEs
					BaseEntity[] beArray = Arrays.stream(codeArr)
						.map(item -> (BaseEntity) beUtils.getBaseEntityByCode(item))
						.toArray(BaseEntity[]::new);

					if (beArray.length == 0) {
						baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray[0]);
					} else {
						baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray);
					}

					continue;

				}
		    }
		    
			// By Default, add it as is
			baseEntityContextMap.put(entry.getKey().toUpperCase(), value);
		}
		
		return baseEntityContextMap;
	}
	
}
