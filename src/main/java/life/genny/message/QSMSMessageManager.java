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
import life.genny.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.ANSIColour;
import life.genny.util.MergeHelper;
import life.genny.utils.BaseEntityUtils;

public class QSMSMessageManager implements QMessageProvider {
	
    
    public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		log.info(ANSIColour.GREEN+">>>>>>>>>>>About to trigger SMS<<<<<<<<<<<<<<"+ANSIColour.RESET);
		
		BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");
		
		if (target == null) {
			log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
			return;
		}
		if (projectBe == null) {
			log.error(ANSIColour.RED+"ProjectBe is NULL"+ANSIColour.RESET);
			return;
		}

		String targetMobile = target.getValue("PRI_MOBILE", null);
		if (targetMobile == null) {
			log.error(ANSIColour.RED+"TargetMobile is NULL"+ANSIColour.RESET);
			return;
		}

		String body = null;
		if (contextMap.containsKey("BODY")) {
			body = (String) contextMap.get("BODY");
		} else {
			body = templateBe.getValue("PRI_BODY", null);
		}
		if (body == null) {
			log.error(ANSIColour.RED+"Body is NULL"+ANSIColour.RESET);
			return;
		}

		// Mail Merging Data
		body = MergeUtil.merge(body, contextMap);

		//target is toPhoneNumber, Source is the fromPhoneNumber
		String accountSID = projectBe.getValue("ENV_TWILIO_ACCOUNT_SID", null);
		String sourcePhone = projectBe.getValue("ENV_TWILIO_SOURCE_PHONE", null);
		String twilioAuthToken = projectBe.getValue("ENV_TWILIO_AUTH_TOKEN", null);

		// Debug logs for devs
		log.debug("accountSID = " + accountSID);
		log.debug("sourcePhone = " + sourcePhone);
		log.debug("twilioAuthToken = " + twilioAuthToken);
		log.debug("targetMobile = " + targetMobile);
		
		if(accountSID != null && sourcePhone != null && twilioAuthToken != null) {

			// Use Try-Catch Block to ensure consumer does not die upon error.
			try {
				// Init and Send SMS
				Twilio.init(accountSID, twilioAuthToken);
				Message msg = Message.creator(new PhoneNumber(targetMobile), new PhoneNumber(sourcePhone), body).create();

				// Log response
				log.info("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
				log.info(ANSIColour.GREEN+" SMS Sent to "+targetMobile +ANSIColour.RESET);

			} catch (Exception e) {
				log.error(ANSIColour.RED+"Could Not Send SMS!!! Exception:"+e+ANSIColour.RESET);
			}
				
		} else {
			log.error(ANSIColour.RED+"Twilio credentials not loaded into cache"+ANSIColour.RESET);
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
				log.info(ANSIColour.GREEN+"sms template from google sheet ::"+smsMesssage+ANSIColour.RESET);
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(smsMesssage, entityTemplateMap);
				
				baseMessage = new QBaseMSGMessage();
				baseMessage.setSubject(template.getSubject());
				baseMessage.setMsgMessageData(messageData);
				
				String targetPhone = recipientBe.getValue("PRI_MOBILE", null);
				log.info("target phone ::"+targetPhone);
				
				baseMessage.setTarget(targetPhone);
				log.info("------->SMS DETAILS ::"+baseMessage+"<---------");
								
			} else {
				log.error("NO TEMPLATE FOUND");
			}
		} else {
			log.error("Recipient BaseEntity is NULL");
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
			log.info(ANSIColour.GREEN+"sms template from google sheet ::"+smsMesssage+ANSIColour.RESET);
			
			// Merging SMS template message with BaseEntity values
			String messageData = MergeUtil.merge(smsMesssage, entityTemplateMap);
			
			baseMessage = new QBaseMSGMessage();
			baseMessage.setSubject(template.getSubject());
			baseMessage.setMsgMessageData(messageData);
			baseMessage.setTarget(to);
			log.info("------->SMS DETAILS ::"+baseMessage+"<---------");
							
		} else {
			log.error("NO TEMPLATE FOUND");
		}
		
		
		return baseMessage;
	}

}
