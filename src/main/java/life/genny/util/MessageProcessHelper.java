package life.genny.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
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
import life.genny.utils.VertxUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;

public class MessageProcessHelper {

	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public static final String ANSI_RESET = "\u001B[0m";
	public static final String ANSI_BLUE = "\u001B[34m";
	public static final String ANSI_RED = "\u001B[31m";

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
			logger.error(ANSI_RED + "GENNY COM MESSAGE IS NULL" + ANSI_RESET);
		}

		logger.info("message model ::" + message.toString());

		GennyToken userToken = new GennyToken(token);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		String realm = beUtils.getGennyToken().getRealm();

		BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_"+beUtils.getGennyToken().getRealm().toUpperCase());

		// Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(beUtils, message);
		baseEntityContextMap.put("PROJECT", projectBe);

		String[] recipientArr = message.getRecipientArr();
		List<BaseEntity> recipientBeList = new ArrayList<BaseEntity>();

		BaseEntity templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());

		if (templateBe == null) {
			logger.error(ANSI_RED + "No Template found for " + message.getTemplateCode() + ANSI_RESET);
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
				logger.error(ANSI_RED + "Could not process recipient " + recipient + ANSI_RESET);
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
			for (QBaseMSGMessageType msgType : message.getMessageTypeArr()) {

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
					logger.error(ANSI_RED + ">>>>>> Provider is NULL for entity: " + ", msgType: " + msgType.toString() + " <<<<<<<<<" + ANSI_RESET);
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
