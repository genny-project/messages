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

	/*
	 * public static final String ACCOUNT_SID =
	 * "AC6bd0bd2ecad9cc99d01a14942e0095a4"; public static final String
	 * AUTH_TOKEN = "f5d7bc286e850b927876fa288a0b7be1";
	 * 
	 * public static void sendSMS(String message, String fromNumber, String
	 * toNumber) {
	 * 
	 * Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
	 * 
	 * //PhoneNumber numberTo = PhoneNumber.fetcher(new
	 * com.twilio.type.PhoneNumber("+61468497227")).fetch();
	 * 
	 * Message msg = Message.creator(new PhoneNumber(toNumber), new
	 * PhoneNumber(fromNumber), message).create();
	 * 
	 * System.out.println("message status:"+msg.getStatus()+", message SID:"+msg
	 * .getSid()); }
	 */

}
