package messages;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;

public class MessageTest {
	
	static QMessageFactory messageFactory = new QMessageFactory();
	
	//@Test
	public void mailTest() throws FileNotFoundException, IOException {
		QBaseMSGMessage msgMessage = new QBaseMSGMessage();
		msgMessage.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		msgMessage.setMsgMessageData("Please show the attachments!!!");
		msgMessage.setSource("rpgayatri@gmail.com");
		msgMessage.setTarget("rpgayatri@gmail.com");
		msgMessage.setSubject("test_subject_with attachment");
		
		String[] emailAttachments = {"/attachments/bornstein-airbnb.png"};
		//String[] emailAttachments = {};
		//String[] emailAttachments = null;
		//String[] emailAttachments = {""};
		//String[] emailAttachments = {"","bornstein-airbnb.png"};
		
		msgMessage.setAttachments(emailAttachments);
		
		
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
	
	//@Test
	public void testJSON() {
		
		QBaseMSGMessage msgMessage = new QBaseMSGMessage();
		msgMessage.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		msgMessage.setMsgMessageData("hello world");
		msgMessage.setSource("rpgayatri@gmail.com");
		msgMessage.setTarget("rpgayatri@gmail.com");
		msgMessage.setSubject("test_subject");
		
		String[] emailAttachments = {"bornstein-airbnb.png","mountains.jpg"};
		msgMessage.setAttachments(emailAttachments);
		
		QBaseMSGMessage msgMessage1 = new QBaseMSGMessage();
		msgMessage1.setMsgMessageType(QBaseMSGMessageType.EMAIL);
		msgMessage1.setMsgMessageData("hello world again");
		msgMessage1.setSource("rpgayatri@gmail.com");
		msgMessage1.setTarget("rpgayatri@gmail.com");
		msgMessage1.setSubject("test_subject_something");
		
		String[] emailAttachments1 = {"bornstein-airbnb.png","mountains.jpg"};
		msgMessage1.setAttachments(emailAttachments1);
		
		/*List<QBaseMSGMessage> msgList = new ArrayList<>();
		msgList.add(msgMessage);
		msgList.add(msgMessage1);*/
		
		JsonObject data = new JsonObject();
		data = JsonObject.mapFrom(msgMessage);
		
		JsonObject data1 = new JsonObject();
		data1 = JsonObject.mapFrom(msgMessage1);
		
		JsonArray msglist = new JsonArray();
		msglist.add(data);
		msglist.add(data1);	
		
			
		JsonObject eventMsg = new JsonObject();
		eventMsg.put("msg_type", "EVT_MSG");
		eventMsg.put("event_type", "message");
		eventMsg.put("msgMessageData", msglist);
		
		System.out.println("message data json ::"+eventMsg);
		
		/*JsonArray qmsglist =  eventMsg.getJsonObject("msgMessageData").mapTo(JsonArray.class);
		System.out.println("source ::"+qmsglist.toString());*/
				
	}
	
}
