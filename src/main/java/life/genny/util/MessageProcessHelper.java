package life.genny.util;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.utils.VertxUtils;

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
	public static void processGenericMessage(QMessageGennyMSG message, String tokenString, EventBus eventbus) {

		System.out.println("message model ::" + message.toString());

		// Create context map with BaseEntities
		Map<String, Object> baseEntityContextMap = new HashMap<>();
		baseEntityContextMap = createBaseEntityContextMap(message, tokenString);

		String[] recipientArr = message.getRecipientArr();
		System.out.println("recipient array ::" + recipientArr.toString());
		QBaseMSGMessage msgMessage = null;
		Map<String, Object> newMap = null;

		/* iterating and triggering email to each recipient individually */
		if (recipientArr != null && recipientArr.length > 0) {

			logger.info("recipient array length ::" + recipientArr.length);

			for (String recipientCode : recipientArr) {

				// Setting Message values
				msgMessage = new QBaseMSGMessage();
				BaseEntity recipientBeFromDDT = VertxUtils.readFromDDT(recipientCode, tokenString);
				newMap = new HashMap<>();
				newMap = baseEntityContextMap;
				newMap.put("RECIPIENT", recipientBeFromDDT);
				logger.info("new map ::" + newMap);

				// Get Message Provider
				QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
				msgMessage = provider.setGenericMessageValue(message, newMap, tokenString);

				if (msgMessage != null) {
					
					// setting attachments
					if (message.getAttachmentList() != null) {
						System.out.println("mail has attachments");
						msgMessage.setAttachmentList(message.getAttachmentList());
					}
					
					BaseEntity unsubscriptionBe = VertxUtils.readFromDDT("COM_EMAIL_UNSUBSCRIPTION", tokenString);
					System.out.println("unsubscribe be :: "+unsubscriptionBe);
					String templateCode = message.getTemplate_code() + "_UNSUBSCRIBE";
					System.out.println("template code in message processor :: "+message.getTemplate_code());
					System.out.println("template code ::"+templateCode);
					
					/* check if unsubscription list for the template code has the userCode */
					Boolean isUserUnsubscribed = VertxUtils.checkIfAttributeValueContainsString(unsubscriptionBe, templateCode, recipientBeFromDDT.getCode());
					
					/* if user is unsubscribed, then dont send emails. But toast and sms are still applicable */
					if(isUserUnsubscribed && !message.getMsgMessageType().equals(QBaseMSGMessageType.EMAIL)) {
						System.out.println("unsubscribed");
						provider.sendMessage(msgMessage, eventbus, newMap);
					}
					
					/* if subscribed, allow messages */
					if(!isUserUnsubscribed) {
						System.out.println("subscribed");
						provider.sendMessage(msgMessage, eventbus, newMap);
					} 
					
				} else {
					logger.error(ANSI_RED + ">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<" + ANSI_RESET);
				}

			}

		} else {
			logger.error(ANSI_RED + "  RECIPIENT NULL OR EMPTY  " + ANSI_RESET);
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
