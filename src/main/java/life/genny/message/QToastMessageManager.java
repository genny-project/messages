package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.channel.Producer;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QDataToastMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class QToastMessageManager implements QMessageProvider{
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, Object> contextMap) {
		
		System.out.println("About to send toast message");
		
		/* message.getPriority() returns "error" for templateCode containing "FAIL", returns "info" for other templates */ 
		QDataToastMessage toastMsg = new QDataToastMessage(message.getPriority(), message.getMsgMessageData());
		toastMsg.setToken(message.getToken());
		
		String[] recipientArr = {message.getTarget()};
		
		toastMsg.setRecipientCodeArray(recipientArr);
		
		String toastJson = JsonUtils.toJson(toastMsg);
		JsonObject toastJsonObj = new JsonObject(toastJson);
		
		/*eventBus.publish("data", toastJsonObj);*/
		//System.out.println(Producer.getToData().write(toastJsonObj));
		//Producer.getToData().write(toastJsonObj);
		Producer.getToWebData().write(toastJsonObj);
	}


	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, Object> entityTemplateMap,
			String token) {
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = (BaseEntity)(entityTemplateMap.get("RECIPIENT"));
		
		if(recipientBe != null) {
			if (template != null) {
				
				String toastMessage = template.getToast_template();
				logger.info(ANSI_GREEN+"toast template from google sheet ::"+toastMessage+ANSI_RESET);
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(toastMessage, entityTemplateMap);
				
				baseMessage = new QBaseMSGMessage();
				baseMessage.setMsgMessageData(messageData);
				baseMessage.setToken(token);
				baseMessage.setTarget(recipientBe.getCode());
				
				if(message.getTemplate_code().contains("FAIL")) {
					baseMessage.setPriority("error");
				} else {
					baseMessage.setPriority("info");
				}
				
				logger.info("------->TOAST DETAILS ::"+baseMessage+"<---------");
								
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		return baseMessage;
	}

}
