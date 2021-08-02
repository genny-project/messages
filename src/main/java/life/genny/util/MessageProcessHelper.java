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

	private static final Logger logger = LoggerFactory
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
			logger.error(ANSIColour.RED + "GENNY COM MESSAGE IS NULL" + ANSIColour.RESET);
		}

		logger.info("message model ::" + message.toString());

		GennyToken userToken = new GennyToken(token);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		String realm = beUtils.getGennyToken().getRealm();
		logger.info("Realm is " + realm);

		BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_"+realm.toUpperCase());

		// Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(beUtils, message);
		baseEntityContextMap.put("PROJECT", projectBe);

		String[] recipientArr = message.getRecipientArr();
		List<BaseEntity> recipientBeList = new ArrayList<BaseEntity>();

		BaseEntity templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());

		if (templateBe == null) {
			logger.error(ANSIColour.RED + "No Template found for " + message.getTemplateCode() + ANSIColour.RESET);
		}

		List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());
		if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
			// Use default if told to do so
			List<String> typeList = beUtils.getBaseEntityCodeArrayFromLNKAttr(templateBe, "PRI_DEFAULT_MSG_TYPE");
			messageTypeList = typeList.stream().map(item -> QBaseMSGMessageType.valueOf(item)).collect(Collectors.toList());
		}

		logger.info("Using TemplateBE " + templateBe.getCode());

		Attribute emailAttr = RulesUtils.getAttribute("PRI_EMAIL", token);

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
				} catch (Exception e) {
					logger.error(e);
				}
			}

			if (recipientBe != null) {
				recipientBeList.add(recipientBe);
			} else {
				logger.error(ANSIColour.RED + "Could not process recipient " + recipient + ANSIColour.RESET);
			}
		}

		BaseEntity unsubscriptionBe = beUtils.getBaseEntityByCode("COM_EMAIL_UNSUBSCRIPTION");
		logger.info("unsubscribe be :: " + unsubscriptionBe);
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
						logger.info("unsubscribed");
						provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
					}

					/* if subscribed, allow messages */
					if (!isUserUnsubscribed) {
						logger.info("subscribed");
						provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
					}
				} else {
					logger.error(ANSIColour.RED + ">>>>>> Provider is NULL for entity: " + ", msgType: " + msgType.toString() + " <<<<<<<<<" + ANSIColour.RESET);
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
			logger.info(entry.getKey() + "/" + entry.getValue());
		    
		    String value = entry.getValue();
		    BaseEntity be = null;
		    if ((value != null) && (value.length()>4))
		    {
		    	if (value.matches("[A-Z]{3}\\_.*")) { // MUST BE A BE CODE
		    		be = VertxUtils.readFromDDT(realm,value, beUtils.getGennyToken().getToken());
		    	}
		    }
		    
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
