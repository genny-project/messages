package life.genny.message;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import life.genny.qwanda.message.QBaseMSGMessage;

public class QSMSMessageManager implements QMessageProvider {
	
	@Override
	public void sendMessage(QBaseMSGMessage message) {

		//target is toPhoneNumber, Source is the fromPhoneNumber,
		Twilio.init(System.getenv("TWILIO_ACCOUNT_SID"), System.getenv("TWILIO_AUTH_TOKEN"));
		Message msg = Message.creator(new PhoneNumber(message.getTarget()), new PhoneNumber(message.getSource()), message.getMsgMessageData()).create();
		System.out.println("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
		
	}

}
