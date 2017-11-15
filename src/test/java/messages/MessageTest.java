package messages;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

//import com.google.gson.JsonObject;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.message.QMessage;
import life.genny.qwanda.message.QMessage.MessageData;

public class MessageTest {
	
	static QMessageFactory messageFactory = new QMessageFactory();
	
	/*@Test
	public void mailTest() throws FileNotFoundException, IOException {
		String message = "Hello world";
		Properties properties = new Properties();
		properties.load(new FileInputStream(System.getProperty("user.dir")+"/credentials.properties"));
		System.out.println("props ::"+properties.entrySet().toString());
		//QEmailMessage.sendMail(message, toMail);
		
		QEventMessage eventMessage = new QEventMessage("EVENT_TYPE", "qw1");
		eventMessage.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		eventMessage.setSource(properties.getProperty("USERNAME"));
		eventMessage.setTarget(properties.getProperty("USERNAME"));
		QMessage msgmessage = new QMessage("event_msg") {
		};
		MessageData data = msgmessage.new MessageData("code");
		data.setValue(message);
		eventMessage.setData(data);
		eventMessage.setSubject("Test_subject_outcome.life");
		
		QMessageProvider provider = messageFactory.getMessageProvider(eventMessage.getMsgMessageType());
		provider.sendMessage(eventMessage);
	}
		
	//@Test
	public void messageFactorySMSTesting() throws FileNotFoundException, IOException {
		
		Properties properties = new Properties();
		properties.load(new FileInputStream(System.getProperty("user.dir")+"/credentials.properties"));
		
		QEventMessage eventMessage = new QEventMessage("EVENT_TYPE", "qw1");
		eventMessage.setMsgMessageType(QBaseMSGMessageType.SMS);
		eventMessage.setSource(properties.getProperty("TWILIO_SOURCE_PHONE"));
		eventMessage.setTarget(properties.getProperty("TWILIO_TARGET_PHONE"));
		QMessage msgmessage = new QMessage("event_msg") {
		};
		MessageData data = msgmessage.new MessageData("code");
		data.setValue("hello world!!");
		eventMessage.setData(data);
		
		QMessageProvider provider = messageFactory.getMessageProvider(eventMessage.getMsgMessageType());
		provider.sendMessage(eventMessage);
	}*/
	
	@Test
	public void testJSON() {
		
		QBaseMSGMessage msgMessage = new QBaseMSGMessage();
		msgMessage.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		msgMessage.setMsgMessageData("hello world");
		msgMessage.setSource("rpgayatri@gmail.com");
		msgMessage.setTarget("rpgayatri@gmail.com");
		msgMessage.setSubject("test_subject");
		
		JsonObject data = new JsonObject();
		data = JsonObject.mapFrom(msgMessage);
		
		
		JsonObject eventMsg = new JsonObject();
		eventMsg.put("msg_type", "EVT_MSG");
		eventMsg.put("event_type", "message");
		eventMsg.put("msgMessageData", data);
		
		System.out.println("message data json ::"+eventMsg);
		
		QBaseMSGMessage qmsg = eventMsg.getJsonObject("msgMessageData").mapTo(QBaseMSGMessage.class);
		System.out.println("source ::"+qmsg.getSource());
				
	}
	
}
