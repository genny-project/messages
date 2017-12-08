package life.genny.message;

import java.util.Map;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class QSMSMessageManager implements QMessageProvider {
	
	@Override
	public void sendMessage(QBaseMSGMessage message) {
		System.out.println("its an sms");
		//target is toPhoneNumber, Source is the fromPhoneNumber,
		Twilio.init(System.getenv("TWILIO_ACCOUNT_SID"), System.getenv("TWILIO_AUTH_TOKEN"));
		
		if (message.getTarget() != null && !message.getTarget().isEmpty()) {
			Message msg = Message.creator(new PhoneNumber(message.getTarget()), new PhoneNumber(message.getSource()), message.getMsgMessageData()).create();
			System.out.println("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
		}
		
		
	}


	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap,
			String recipient) {

		BaseEntity be = entityTemplateMap.get(recipient);
		QBaseMSGMessage baseMessage = null;

		if (be != null) {	

			// Fetching Message template from sheets
			Map templateMap = MergeHelper.getTemplate(message.getTemplate_code());
			
			
			if(templateMap != null) {
				String smsMesssage = templateMap.get("sms").toString();
				baseMessage = new QBaseMSGMessage();
				
				// Merging SMS template message with BaseEntity values
				String messageData = MergeUtil.merge(smsMesssage.toString(), entityTemplateMap);

				baseMessage.setMsgMessageData(messageData);
				baseMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));

				// Fetching Phone number attribute from BaseEntity for recipient
				baseMessage.setTarget(MergeUtil.getBaseEntityAttrValue(be, "PRI_MOBILE"));
			}
		}

		System.out.println("base message model for email ::"+baseMessage);
		return baseMessage;
	}

}
