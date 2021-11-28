package life.genny.process;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.io.IOException;
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
import life.genny.message.QMessageGennyMSG;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.ANSIColour;
import life.genny.utils.VertxUtils;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.RulesUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import life.genny.util.MsgUtils;
import life.genny.qwandautils.GennySettings;

public class MessageProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static QMessageFactory messageFactory = new QMessageFactory();

	/**
	 * Generic Message Handling method.
	 * 
	 * @param message
	 * @param token
	 */
	public static void processGenericMessage(QMessageGennyMSG message, String token) {

		// Begin recording duration
		long start = System.currentTimeMillis();

		if (message == null) {
			log.error(ANSIColour.RED + "GENNY COM MESSAGE IS NULL" + ANSIColour.RESET);
		}

		log.debug("Incoming Message ::" + message.toString());

		// Init utility objects
		GennyToken userToken = new GennyToken(token);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);
		String realm = beUtils.getGennyToken().getRealm();
		log.info("Realm is " + realm);

		BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_"+realm.toUpperCase());

		// Create context map with BaseEntities
		HashMap<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(beUtils, message);
		baseEntityContextMap.put("PROJECT", projectBe);

		List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());

		String[] recipientArr = message.getRecipientArr();
		List<BaseEntity> recipientBeList = new ArrayList<BaseEntity>();

		BaseEntity templateBe = null;
		if (message.getTemplateCode() != null) {
			templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());
		}

		if (templateBe == null) {
			log.warn(ANSIColour.YELLOW + "No Template found for " + message.getTemplateCode() + ANSIColour.RESET);
		} else {
			log.info("Using TemplateBE " + templateBe.getCode());

			// Handle any default context associations
			String contextAssociations = templateBe.getValue("PRI_CONTEXT_ASSOCIATIONS", null);
			if (contextAssociations != null) {
				MergeUtil.addAssociatedContexts(beUtils, baseEntityContextMap, contextAssociations, false);
			}

			// Check for Default Message
			if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
				// Use default if told to do so
				List<String> typeList = beUtils.getBaseEntityCodeArrayFromLNKAttr(templateBe, "PRI_DEFAULT_MSG_TYPE");
				messageTypeList = typeList.stream().map(item -> QBaseMSGMessageType.valueOf(item)).collect(Collectors.toList());
			}

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

			// Process any URL contexts for this recipient
			if (baseEntityContextMap.containsKey("URL:ENCODE")) {
				// Fetch form contexts
				log.info("Handling Encoding...");
				String[] componentArray = ((String) baseEntityContextMap.get("URL:ENCODE")).split("/");
				// Init and grab url structure
				String parentCode = null;
				String code = null;
				String targetCode = null;
				if (componentArray.length > 0) {
					log.info("length 1");
					log.info(componentArray[0]);
					parentCode = componentArray[0];
				}
				if (componentArray.length > 1) {
					log.info("length 2");
					log.info(componentArray[1]);
					code = componentArray[1];
				}
				if (componentArray.length > 2) {
					log.info("length 3");
					log.info(componentArray[2]);
					targetCode = componentArray[2];
				}
				// Fetch access token
				String accessToken = null;
				try {
					log.info("Fetching Token for " + recipientBe.getCode());
					accessToken = KeycloakUtils.getImpersonatedToken(userToken.getKeycloakUrl(), userToken.getRealm(), projectBe, recipientBe, userToken.getToken());
				} catch (IOException e) {
					log.error("Could not fetch Token: " + e.getStackTrace());
				}
				// Encode URL and put back in the map
				String url = MsgUtils.encodeUrl(GennySettings.projectUrl+"/home", parentCode, code, targetCode, accessToken);
				baseEntityContextMap.put("URL", url);
			}

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

		long duration = System.currentTimeMillis() - start;
		log.info("FINISHED PROCESSING MESSAGE :: time taken = " + String.valueOf(duration));
	}

	private static HashMap<String, Object> createBaseEntityContextMap(BaseEntityUtils beUtils, QMessageGennyMSG message) {
		
		HashMap<String, Object> baseEntityContextMap = new HashMap<>();
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

					if (beArray.length == 1) {
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
