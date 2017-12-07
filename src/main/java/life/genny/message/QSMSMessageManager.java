package life.genny.message;

import java.util.Map;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;

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
			baseMessage = new QBaseMSGMessage();
			// working on the message template
			String messageData = MergeUtil.merge(message.getTemplate_code(), entityTemplateMap);
			
			baseMessage.setMsgMessageData(messageData);
			baseMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));
			baseMessage.setAttachments(message.getAttachments());

			baseMessage.setTarget(MergeUtil.getBaseEntityAttrValue(be, "PRI_MOBILE"));
		}

		System.out.println("base message model for email ::"+baseMessage);
		return baseMessage;
	}

}
