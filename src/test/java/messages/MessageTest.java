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
	
	//@Test
	public void mailTest() throws FileNotFoundException, IOException {
		QBaseMSGMessage msgMessage = new QBaseMSGMessage();
		msgMessage.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		msgMessage.setMsgMessageData("hello world");
		msgMessage.setSource("rpgayatri@gmail.com");
		msgMessage.setTarget("rpgayatri@gmail.com");
		msgMessage.setSubject("test_subject");
		
		
		QMessageProvider provider = messageFactory.getMessageProvider(msgMessage.getMsgMessageType());
		provider.sendMessage(msgMessage);
	}
		
	//@Test
	public void messageFactorySMSTesting() throws FileNotFoundException, IOException {
		QBaseMSGMessage msgMessage = new QBaseMSGMessage();
		msgMessage.setMsgMessageType(QBaseMSGMessageType.SMS);
		msgMessage.setMsgMessageData("hello world");
		msgMessage.setSource("+61488807705");
		msgMessage.setTarget("+61468497227");
		msgMessage.setSubject("test_subject");
		
		QMessageProvider provider = messageFactory.getMessageProvider(msgMessage.getMsgMessageType());
		provider.sendMessage(msgMessage);
	}
	
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
