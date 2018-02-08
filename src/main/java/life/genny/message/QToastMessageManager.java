package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.channels.EBProducers;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwanda.message.QDataToastMessage;
import life.genny.qwanda.message.QMSGMessage;
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
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus) {
		
		System.out.println("About to send toast message");
		
		QDataToastMessage toastMsg = new QDataToastMessage("info", message.getMsgMessageData());
		toastMsg.setToken(message.getToken());
		
		String[] recipientArr = {message.getTarget()};
		
		toastMsg.setRecipientCodeArray(recipientArr);
		
		String toastJson = JsonUtils.toJson(toastMsg);
		JsonObject toastJsonObj = new JsonObject(toastJson);
		
		eventBus.publish("cmds", toastJsonObj);
		/*QCmdMessage cmdView = new QCmdMessage("CMD_NOTIFICATION", "toast");
		JsonObject jsonObj = JsonObject.mapFrom(cmdView);
		jsonObj.put("style", "info");
		jsonObj.put("token", message.getToken());
		jsonObj.put("text", message.getMsgMessageData());
		
		String[] recipientArr = {message.getTarget()};
		
		jsonObj.put("recipientCodeArray", recipientArr);
		
		eventBus.publish("data", jsonObj);*/
		
		
		
		/*public void publishData(final BaseEntity be, final String aliasCode, final String[] recipientsCode) {
			
			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(be, aliasCode);
			msg.setToken(getToken());
			if (recipientsCode != null) {
				msg.setRecipientCodeArray(recipientsCode);
			}
			publish("cmds",  RulesUtils.toJsonObject(msg));
		}*/
			
	}

	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap,
			String recipient, String token) {
		return null;
	}

	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, BaseEntity> entityTemplateMap,
			String token) {
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = entityTemplateMap.get("RECIPIENT");
		
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
