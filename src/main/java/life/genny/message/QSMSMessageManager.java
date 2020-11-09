package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class QSMSMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    
    public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, Object> contextMap) {
		logger.info(ANSI_GREEN+">>>>>>>>>>>About to trigger SMS<<<<<<<<<<<<<<"+ANSI_RESET);
		
		BaseEntity projectBe = (BaseEntity)contextMap.get("PROJECT");
		
		if(projectBe != null) {
			//target is toPhoneNumber, Source is the fromPhoneNumber
			String accountSID = projectBe.getValue("ENV_TWILIO_ACCOUNT_SID", null);
			String sourcePhone = projectBe.getValue("ENV_TWILIO_SOURCE_PHONE", null);
			String twilioAuthToken = projectBe.getValue("ENV_TWILIO_AUTH_TOKEN", null);

			
			if(accountSID != null && sourcePhone != null && twilioAuthToken != null) {
				Twilio.init(accountSID, twilioAuthToken);
				message.setSource(sourcePhone);
								
				if (message.getTarget() != null && !message.getTarget().isEmpty()) {
					
					//target is a string array of multiple target phone numbers
					String[] messageTargetArr = StringUtils.split(message.getTarget(), ",");
					
					for(String targetMobile : messageTargetArr) {
						Message msg = Message.creator(new PhoneNumber(targetMobile), new PhoneNumber(message.getSource()), message.getMsgMessageData()).create();
						logger.info("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
						logger.info(ANSI_GREEN+" SMS Sent to "+targetMobile +ANSI_RESET);
					}
					
				} else {
					logger.error("SMS not sent since target phone number is empty or NULL");
				}
			} else {
				logger.error("Twilio credentials not loaded into cache");
			}
			
		} else {
			logger.error("SMS not sent since Project Baseentity is NULL");
		}
			
		
	}


	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, Object> entityTemplateMap,
			String token) {
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
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


	@Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String token, String to) {
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		
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
