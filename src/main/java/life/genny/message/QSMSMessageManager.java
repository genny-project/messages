package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;
import life.genny.utils.BaseEntityUtils;

public class QSMSMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	public static final String ANSI_RED = "\u001B[31m";
    
    public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		logger.info(ANSI_GREEN+">>>>>>>>>>>About to trigger SMS<<<<<<<<<<<<<<"+ANSI_RESET);
		
		BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");
		
		if (target == null) {
			logger.error(ANSI_RED+"Target is NULL"+ANSI_RESET);
			return;
		}
		if (projectBe == null) {
			logger.error(ANSI_RED+"ProjectBe is NULL"+ANSI_RESET);
			return;
		}

		String targetMobile = target.getValue("PRI_MOBILE", null);
		String body = templateBe.getValue("PRI_BODY", null);

		if (targetMobile == null) {
			logger.error(ANSI_RED+"TargetMobile is NULL"+ANSI_RESET);
			return;
		}
		if (body == null) {
			logger.error(ANSI_RED+"Body is NULL"+ANSI_RESET);
			return;
		}

		// Mail Merging Data
		body = MergeUtil.merge(body, contextMap);

		//target is toPhoneNumber, Source is the fromPhoneNumber
		String accountSID = projectBe.getValue("ENV_TWILIO_ACCOUNT_SID", null);
		String sourcePhone = projectBe.getValue("ENV_TWILIO_SOURCE_PHONE", null);
		String twilioAuthToken = projectBe.getValue("ENV_TWILIO_AUTH_TOKEN", null);

		// Debug logs for devs
		logger.debug("accountSID = " + accountSID);
		logger.debug("sourcePhone = " + sourcePhone);
		logger.debug("twilioAuthToken = " + twilioAuthToken);
		logger.debug("targetMobile = " + targetMobile);
		
		if(accountSID != null && sourcePhone != null && twilioAuthToken != null) {

			Twilio.init(accountSID, twilioAuthToken);
							
			Message msg = Message.creator(new PhoneNumber(targetMobile), new PhoneNumber(sourcePhone), body).create();
			logger.info("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
			logger.info(ANSI_GREEN+" SMS Sent to "+targetMobile +ANSI_RESET);
				
		} else {
			logger.error(ANSI_RED+"Twilio credentials not loaded into cache"+ANSI_RESET);
		}
			
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
				
				String smsMesssage = template.getSms_template();
				logger.info(ANSI_GREEN+"sms template from google sheet ::"+smsMesssage+ANSI_RESET);
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(smsMesssage, entityTemplateMap);
				
				baseMessage = new QBaseMSGMessage();
				baseMessage.setSubject(template.getSubject());
				baseMessage.setMsgMessageData(messageData);
				
				String targetPhone = recipientBe.getValue("PRI_MOBILE", null);
				logger.info("target phone ::"+targetPhone);
				
				baseMessage.setTarget(targetPhone);
				logger.info("------->SMS DETAILS ::"+baseMessage+"<---------");
								
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		
		return baseMessage;
	}


	// @Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String to) {

		String token = beUtils.getGennyToken().getToken();
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplateCode(), token);
		
		if (template != null) {
			
			String smsMesssage = template.getSms_template();
			logger.info(ANSI_GREEN+"sms template from google sheet ::"+smsMesssage+ANSI_RESET);
			
			// Merging SMS template message with BaseEntity values
			String messageData = MergeUtil.merge(smsMesssage, entityTemplateMap);
			
			baseMessage = new QBaseMSGMessage();
			baseMessage.setSubject(template.getSubject());
			baseMessage.setMsgMessageData(messageData);
			baseMessage.setTarget(to);
			logger.info("------->SMS DETAILS ::"+baseMessage+"<---------");
							
		} else {
			logger.error("NO TEMPLATE FOUND");
		}
		
		
		return baseMessage;
	}

}
