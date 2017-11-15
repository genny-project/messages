package life.genny.message;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import life.genny.qwanda.message.QBaseMSGMessage;

public class QSMSMessageManager implements QMessageProvider {
	
	@Override
	public void sendMessage(QBaseMSGMessage message) {
		
		Properties properties = getProperties();

		//target is toPhoneNumber, Source is the fromPhoneNumber,
		Twilio.init(properties.getProperty("ACCOUNT_SID"), properties.getProperty("AUTH_TOKEN"));
		Message msg = Message.creator(new PhoneNumber(message.getTarget()), new PhoneNumber(message.getSource()), message.getMsgMessageData()).create();
		System.out.println("message status:" + msg.getStatus() + ", message SID:" + msg.getSid());
		
	}

	private Properties getProperties() {
		Properties properties = new Properties();
		
		try {
			properties.load(new FileInputStream(System.getProperty("user.dir") + "/credentials.properties"));

		} catch (IOException e) {
			e.printStackTrace();
		}

		return properties;
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
