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
import life.genny.qwanda.message.QCmdMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.ANSIColour;
import life.genny.util.MergeHelper;
import life.genny.utils.BaseEntityUtils;
import life.genny.utils.VertxUtils;

@ApplicationScoped
public class QToastMessageManager implements QMessageProvider{
	

	
	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	Producer producer;

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		
		log.info("About to send toast message");
		
		/* message.getPriority() returns "error" for templateCode containing "FAIL", returns "info" for other templates */ 
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

		if (target == null) {
			log.error("Target is NULL");
			return;
		}

		// Check for Toast Body
		String body = null;
		if (contextMap.containsKey("BODY")) {
			body = (String) contextMap.get("BODY");
		} else {
			body = templateBe.getValue("PRI_BODY", null);
		}
		if (body == null) {
			log.error("body is NULL");
			return;
		}

		// Check for Toast Style
		String style = null;
		if (contextMap.containsKey("STYLE")) {
			style = (String) contextMap.get("STYLE");
		} else {
			style = templateBe.getValue("PRI_STYLE", "INFO");
		}
		if (style == null) {
			log.error("style is NULL");
			return;
		}

		// Mail Merging Data
		body = MergeUtil.merge(body, contextMap);

		QCmdMessage msg = new QCmdMessage("TOAST", style);
		msg.setMessage(body);
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setSend(true);
		VertxUtils.writeMsg("webcmds", msg);
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
				log.info(ANSIColour.GREEN+"toast template from google sheet ::"+toastMessage+ANSIColour.RESET);
				
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
				
				log.info("------->TOAST DETAILS ::"+baseMessage+"<---------");
								
			} else {
				log.error("NO TEMPLATE FOUND");
			}
		} else {
			log.error("Recipient BaseEntity is NULL");
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
		log.info("direct user recipient for toast ::"+userBe.getCode());
			
		if (template != null) {
				
			String toastMessage = template.getToast_template();
			log.info(ANSIColour.GREEN+"toast template from google sheet ::"+toastMessage+ANSIColour.RESET);
			
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
			
			log.info("------->TOAST DETAILS ::"+baseMessage+"<---------");
								
		} else {
			log.error("NO TEMPLATE FOUND");
		}
		
		return baseMessage;
	}

}
