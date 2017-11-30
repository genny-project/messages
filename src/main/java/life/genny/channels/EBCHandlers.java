package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.message.QMessageFactory;
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
		
			logger.info("message object ::"+message);
			logger.info("token ::"+token);
			
			processMessage(message, token);
			

		});

	}

	private static void processMessage(QMSGMessage message, String token) {
		
		MergeHelper helper = new MergeHelper();
		
		Map<String, String> keyEntityAttrMap = helper.getKeyEntityAttrMap(message);
		
		if(keyEntityAttrMap.containsKey("code")) {
			//Working on it currently, should return be a map of linkValue & BaseEntity
			helper.getBaseEntWithChildrenForAttributeCode(keyEntityAttrMap.get("code"), token);
		}
		
		
		
		/*
		
		Map<String, BaseEntity> entityTemplateMap = new MergeHelper().mergeHelper(message);
		System.out.println("entityTemplateMap ::"+entityTemplateMap);
		
		if(!entityTemplateMap.isEmpty()){
			String messageData = MergeUtil.merge(SAMPLE_SMS_TEMPLATE, entityTemplateMap);
			
			QBaseMSGMessage baseMessage = new QBaseMSGMessage();
			baseMessage.setMsgMessageData(messageData);
		
		}*/
		
		
			
			/*// triggers message depending on the message type
			basemsglist.forEach(msgMessage -> {
				logger.info("about to trigger message");
				QMessageProvider provider = messageFactory.getMessageProvider(msgMessage.getMsgMessageType());
				provider.sendMessage(msgMessage);
				logger.info("message triggered");
			});*/
		
	}

}
