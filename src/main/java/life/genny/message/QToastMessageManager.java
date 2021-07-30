package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.channels.Producer;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QDataToastMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;
import life.genny.utils.BaseEntityUtils;

@ApplicationScoped
public class QToastMessageManager implements QMessageProvider{
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	Producer producer;

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		
		logger.info("About to send toast message");
		
		/* message.getPriority() returns "error" for templateCode containing "FAIL", returns "info" for other templates */ 
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");
		String body = templateBe.getValue("PRI_BODY", null);
		String style = templateBe.getValue("PRI_STYLE", "info");

		if (target == null) {
			logger.error("Target is NULL");
			return;
		}

		if (body == null) {
			logger.error("body is NULL");
			return;
		}

		// Mail Merging Data
		body = MergeUtil.merge(body, contextMap);

		QDataToastMessage toastMsg = new QDataToastMessage(style, body);
		toastMsg.setToken(beUtils.getGennyToken().getToken());
		
		String[] recipientArr = { target.getCode() };
		
		toastMsg.setRecipientCodeArray(recipientArr);
		
		String toastJson = JsonUtils.toJson(toastMsg);
		JsonObject toastJsonObj = new JsonObject(toastJson);
		
		producer.getToWebData().send(toastJsonObj.toString());
	}


	// @Override
	public QBaseMSGMessage setGenericMessageValue(BaseEntityUtils beUtils, QMessageGennyMSG message, 
			Map<String, Object> entityTemplateMap) {

		String token = beUtils.getGennyToken().getToken();
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplateCode(), token);
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
				
				if(message.getTemplateCode().contains("FAIL")) {
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


	/* refrain from using this method, instead pass the recipient array itself */
	// @Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String to) {

		String token = beUtils.getGennyToken().getToken();
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplateCode(), token);
		
		BaseEntityUtils baseEntity = new BaseEntityUtils(GennySettings.qwandaServiceUrl, token, null, null);
		BaseEntity userBe = baseEntity.getBaseEntityByAttributeAndValue("PRI_EMAIL", to);
		logger.info("direct user recipient for toast ::"+userBe.getCode());
			
		if (template != null) {
				
			String toastMessage = template.getToast_template();
			logger.info(ANSI_GREEN+"toast template from google sheet ::"+toastMessage+ANSI_RESET);
			
			// Merging SMS template message with BaseEntity values
			String messageData = MergeUtil.merge(toastMessage, entityTemplateMap);
			
			baseMessage = new QBaseMSGMessage();
			baseMessage.setMsgMessageData(messageData);
			baseMessage.setToken(token);
			
			if(userBe != null) {
				baseMessage.setTarget(userBe.getCode());
			}
			
			if(message.getTemplateCode().contains("FAIL")) {
				baseMessage.setPriority("error");
			} else {
				baseMessage.setPriority("info");
			}
			
			logger.info("------->TOAST DETAILS ::"+baseMessage+"<---------");
								
		} else {
			logger.error("NO TEMPLATE FOUND");
		}
		
		return baseMessage;
	}

}
