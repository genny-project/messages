package life.genny.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.models.GennyToken;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.utils.VertxUtils;
import life.genny.utils.BaseEntityUtils;

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
	 * @param tokenString
	 * @param eventbus
	 *            <p>
	 *            Based on the message provider (SMS/Email/Voice), information for
	 *            message will be set and message will be triggered
	 *            </p>
	 */
	public static void processGenericMessage(QMessageGennyMSG message, String tokenString) {

		logger.info("message model ::" + message.toString());

		GennyToken userToken = new GennyToken(tokenString);
		BaseEntityUtils beUtils = new BaseEntityUtils(userToken);

		// Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(beUtils, message);

		String[] recipientArr = message.getRecipientArr();
		String[] to = message.getTo();

		/* iterating and triggering email to each recipient individually */
		if (recipientArr != null && recipientArr.length > 0) {

			logger.info("recipient array length ::" + recipientArr.length);
			messageProcessorForBaseEntityRecipientArray(beUtils, message, baseEntityContextMap);

		} else if(to != null && to.length > 0) {
			
			logger.info("to array length ::" + to.length);
			messageProcessorForDirectRecipientArray(beUtils, message, baseEntityContextMap);
		}
		else {
			logger.error(ANSI_RED + "  RECIPIENT NULL OR EMPTY  " + ANSI_RESET);
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
	
	/* When recipientArray is an array of BaseEntityCodeArray, we use this method to send message */
	private static void messageProcessorForBaseEntityRecipientArray(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> baseEntityContextMap) {
		
		String token = beUtils.getGennyToken().getToken();
		String realm = beUtils.getGennyToken().getRealm();

		for (String recipientCode : message.getRecipientArr()) {

			// Setting Message values
			QBaseMSGMessage msgMessage = new QBaseMSGMessage();
			BaseEntity recipientBe = beUtils.getBaseEntityByCoe(recipientCode);
			if (recipientBe == null) {
				
				logger.error(ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned for "+recipientCode+" is null<<<<<<<<<"
						+ ANSI_RESET);
			} else {
				Map<String, Object> newMap = new HashMap<>();
				newMap = baseEntityContextMap;
				newMap.put("RECIPIENT", recipientBe);
				logger.info("new map ::" + newMap);

				/* Get Message Provider */
				QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());

				/* set values for sending message */
				msgMessage = provider.setGenericMessageValue(beUtils, message, newMap);

				if (msgMessage != null) {

					// setting attachments
					if (message.getAttachmentList() != null) {
						logger.info("mail has attachments");
						msgMessage.setAttachmentList(message.getAttachmentList());
					}

					BaseEntity unsubscriptionBe = beUtils.getBaseEntityByCode("COM_EMAIL_UNSUBSCRIPTION");
					logger.info("unsubscribe be :: " + unsubscriptionBe);
					String templateCode = message.getTemplateCode() + "_UNSUBSCRIBE";

					/* check if unsubscription list for the template code has the userCode */
					Boolean isUserUnsubscribed = VertxUtils.checkIfAttributeValueContainsString(unsubscriptionBe,
							templateCode, recipientBe.getCode());

					/*
					 * if user is unsubscribed, then dont send emails. But toast and sms are still
					 * applicable
					 */
					if (isUserUnsubscribed && !message.getMsgMessageType().equals(QBaseMSGMessageType.EMAIL)) {
						logger.info("unsubscribed");
						provider.sendMessage(beUtils, msgMessage, newMap);
					}

					/* if subscribed, allow messages */
					if (!isUserUnsubscribed) {
						logger.info("subscribed");
						provider.sendMessage(beUtils, msgMessage, newMap);
					}

				} else {
					logger.error(ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<"
							+ ANSI_RESET);
				}
			}
		}
	}
	
	/* When recipientArray is an array of emailIds OR array of phone-numbers, we use this method to send message */
	private static void messageProcessorForDirectRecipientArray(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> baseEntityContextMap) {
		
		String token = beUtils.getGennyToken().getToken();

		// Setting Message values		
		/* directToValue -> actual emailId or phoneNumber */
		for(String directToValue : message.getTo()) {
			
			// Get Message Provider
			QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
			
			/* set values for sending message */
			QBaseMSGMessage msgMessage = provider.setGenericMessageValueForDirectRecipient(beUtils, message, baseEntityContextMap, directToValue);
			
			if (msgMessage != null) {
				
				/* setting attachments */
				if (message.getAttachmentList() != null) {
					logger.info("mail has attachments");
					msgMessage.setAttachmentList(message.getAttachmentList());
				}
				
				//TODO Need to implement unsubscription for direct email list
				
				provider.sendMessage(beUtils, msgMessage, baseEntityContextMap);
						
				
			} else {
				logger.error(ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
			}
		}
	}

}
