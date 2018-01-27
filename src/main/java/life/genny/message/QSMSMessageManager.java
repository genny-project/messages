package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMSGMessage;
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
	public void sendMessage(QBaseMSGMessage message) {
		logger.info(ANSI_GREEN+">>>>>>>>>>>About to trigger SMS<<<<<<<<<<<<<<"+ANSI_RESET);
		//target is toPhoneNumber, Source is the fromPhoneNumber,
		Twilio.init(System.getenv("TWILIO_ACCOUNT_SID"), System.getenv("TWILIO_AUTH_TOKEN"));
		
		if (message.getTarget() != null && !message.getTarget().isEmpty()) {
			
			//target is a string array of multiple target phone numbers
			String[] messageTargetArr = StringUtils.split(message.getTarget(), ",");
			
			for(String targetMobile : messageTargetArr) {
				Message msg = Message.creator(new PhoneNumber(targetMobile), new PhoneNumber(message.getSource()), message.getMsgMessageData()).create();
				System.out.println("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
				logger.info(ANSI_GREEN+" SMS Sent to "+targetMobile +ANSI_RESET);
			}
			
		}
		
		
	}


	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap,
			String recipient, String token) {

		BaseEntity be = null;
		if(recipient != MESSAGE_BOTH_DRIVER_OWNER){
			be = entityTemplateMap.get(recipient);
		}
		QBaseMSGMessage baseMessage = null;
		
		// Fetching Message template from sheets
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);

		if (be == null && recipient.equals(MESSAGE_BOTH_DRIVER_OWNER)) {
			
			logger.info("Message to BOTH driver and owner");
			
			if(template != null) {
				String smsMesssage = template.getSms_template();
				logger.info(ANSI_GREEN+"sms template from google sheet ::"+smsMesssage+ANSI_RESET);
				baseMessage = new QBaseMSGMessage();
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(smsMesssage.toString(), entityTemplateMap);

				baseMessage.setMsgMessageData(messageData);
				baseMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));

				// Fetching Phone number attribute from BaseEntity for recipients
				Set<String> targetlist = new HashSet<>();
				entityTemplateMap.entrySet().forEach(baseEntityMap -> {
					String targetMobile = MergeUtil.getBaseEntityAttrValueAsString(baseEntityMap.getValue(), "PRI_MOBILE");
					if(targetMobile != null){
						targetlist.add(targetMobile);
					}else {
						//This condition is for the test sms service
						String testEmail = MergeUtil.getBaseEntityAttrValueAsString(baseEntityMap.getValue(), "TST_SMS");
						if(testEmail != null) {
							targetlist.add(testEmail);
						}
					}			
				});
				
				System.out.println("targetlist string ::"+targetlist.toString());
				baseMessage.setTarget(targetlist.toString());
				logger.info(ANSI_GREEN+"Target mobile number is set"+ANSI_RESET);
			}
			
		} else if (be != null) {
			
			logger.info("Message to "+recipient);
			
			String smsMesssage = template.getSms_template();
			logger.info(ANSI_GREEN+"sms template from google sheet ::"+smsMesssage+ANSI_RESET);
			baseMessage = new QBaseMSGMessage();
			
			// Merging SMS template message with BaseEntity values
			String messageData = MergeUtil.merge(smsMesssage.toString(), entityTemplateMap);

			baseMessage.setMsgMessageData(messageData);
			baseMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));

			// Fetching Phone number attribute from BaseEntity for recipients
			Set<String> targetlist = new HashSet<>();
			String targetMobile = MergeUtil.getBaseEntityAttrValueAsString(be, "PRI_MOBILE");
			if(targetMobile != null){
				targetlist.add(targetMobile);
			}else {
				//This condition is for the test sms service
				String testEmail = MergeUtil.getBaseEntityAttrValueAsString(be, "TST_SMS");
				if(testEmail != null) {
					targetlist.add(testEmail);
				}
			}
			
			System.out.println("targetlist string ::"+targetlist.toString());
			baseMessage.setTarget(targetlist.toString());
			logger.info(ANSI_GREEN+"Target mobile number is set"+ANSI_RESET);
			
		}

		System.out.println("base message model for email ::"+baseMessage);
		return baseMessage;
	}


	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, BaseEntity> entityTemplateMap,
			String token) {
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = entityTemplateMap.get("RECIPIENT");
		
		if(recipientBe != null) {
			if (template != null) {
				
				String smsMesssage = template.getSms_template();
				logger.info(ANSI_GREEN+"sms template from google sheet ::"+smsMesssage+ANSI_RESET);
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(smsMesssage.toString(), entityTemplateMap);
				
				baseMessage = new QBaseMSGMessage();
				baseMessage.setSubject(template.getSubject());
				baseMessage.setMsgMessageData(messageData);
				baseMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));
				baseMessage.setTarget(MergeUtil.getBaseEntityAttrValueAsString(recipientBe, "PRI_MOBILE"));
				logger.info("------->SMS DETAILS ::"+baseMessage+"<---------");
								
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		
		
		return baseMessage;
	}

}
