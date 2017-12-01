package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageType;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.util.MergeHelper;

public class EBCHandlers {
	
	public static final String SAMPLE_SMS_TEMPLATE = "Welcome {{RECIPIENT.PRI_FIRSTNAME}} {{RECIPIENT.PRI_LASTNAME}} !! Your contact number is {{RECIPIENT.PRI_MOBILE}} and email ID is {{RECIPIENT.PRI_EMAIL}}";

	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static Gson gson = new Gson();
	
//	final static String defaultSmsProvider = System.getenv("DEFAULT_SMS_PROVIDER_CODE");
//	final static String defaultMailProvider = System.getenv("DEFAULT_MAIL_PROVIDER_CODE");
//	final static String defaultVoiceProvider = System.getenv("DEFAULT_VOICE_PROVIDER_CODE");

	final static String twilio_source = System.getenv("TWILIO_SOURCE_PHONE");
	
	static String token;
	// static MessageProvider messageProvider;
	static QMessageFactory messageFactory = new QMessageFactory();

	public static void registerHandlers(final EventBus eventBus) {

		EBConsumers.getFromMessages().subscribe(arg -> {
			logger.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));
			
			final JsonObject payload = new JsonObject(arg.body().toString());
			final String token = payload.getString("token");

			
			System.out.println(payload);
			logger.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
			final QMSGMessage message = gson.fromJson(payload.toString(), QMSGMessage.class);
			
			//processMessage(message, token);
			processTestMessage(message);

		});

	}
	
	
	private static void processTestMessage(QMSGMessage message) {
		
		Map<String, String> messageContent = new HashMap<String, String>();
		messageContent.put("GRP_QUOTES", "The project has been quoted");
		messageContent.put("GRP_APPROVED", "The project owner has aproved the quote");
		messageContent.put("GRP_IN_TRANSIT", "The Load is now in transit");
		messageContent.put("GRP_COMPLETED", "The load has been delivered");
		messageContent.put("GRP_PAID","The payment has been made");
		
		Map<String, String> keyEntityAttrMap = MergeHelper.getKeyEntityAttrMap(message);
		
		QBaseMSGMessage testMessage = new QBaseMSGMessage();
		testMessage.setMsgMessageData("Dear Customer,"+messageContent.get(keyEntityAttrMap.get("targetBE")));
		
		if(message.getMsgMessageType().equals(QBaseMSGMessageType.SMS)) {
			testMessage.setSource(System.getenv("TWILIO_SOURCE_PHONE"));
			testMessage.setTarget(System.getenv("TWILIO_TARGET_PHONE"));
		}else if(message.getMsgMessageType().equals(QBaseMSGMessageType.EMAIL)) {
			testMessage.setTarget(System.getenv("EMAIL_TARGET"));
		}
		
		
		QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
		provider.sendMessage(testMessage);
		
	}

	private static void processMessage(QMSGMessage message, String token) {
				
		Map<String, String> keyEntityAttrMap = MergeHelper.getKeyEntityAttrMap(message);
		
		if(keyEntityAttrMap.containsKey("code")) {
			//Working on it currently, should return be a map of linkValue & BaseEntity
			Map<String, BaseEntity> templateBaseEntMap = MergeHelper.getBaseEntWithChildrenForAttributeCode(keyEntityAttrMap.get("code"), token);
			
			if(templateBaseEntMap != null && !templateBaseEntMap.isEmpty()) {
				triggerMessage(message, templateBaseEntMap, keyEntityAttrMap.get("recipient").toString());				
			}
		}
		
	}
	
	public static void triggerMessage(QMSGMessage message, Map<String, BaseEntity> templateBaseEntMap, String recipient) {
		QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
		QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntMap, recipient);
		provider.sendMessage(msgMessage);
	}

}
