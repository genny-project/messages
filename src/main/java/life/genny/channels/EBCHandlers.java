package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class EBCHandlers {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_RED = "\u001B[31m";
	
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
						
			Vertx.vertx().executeBlocking(arg1->{
				final JsonObject payload = new JsonObject(arg.body().toString());
				
				System.out.println(payload);
				logger.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
				final QMSGMessage message = gson.fromJson(payload.toString(), QMSGMessage.class);
				processMessage(message, payload.getString("token"));
			}, arg2->{
				
			});
			

		});

	}
	

	private static void processMessage(QMSGMessage message, String token) {
		
				
		Map<String, String> keyEntityAttrMap = MergeHelper.getKeyEntityAttrMap(message);
				
		if(keyEntityAttrMap.containsKey("code")) {
			Map<String, BaseEntity> templateBaseEntMap = MergeUtil.getBaseEntWithChildrenForAttributeCode(keyEntityAttrMap.get("code"), token);
			
			if(templateBaseEntMap != null && !templateBaseEntMap.isEmpty()) {
				logger.info(ANSI_BLUE+"template base entity map ::"+templateBaseEntMap+ANSI_RESET);
				triggerMessage(message, templateBaseEntMap, keyEntityAttrMap.get("recipient").toString(), token);				
			}
		}
		
	}
	
	public static void triggerMessage(QMSGMessage message, Map<String, BaseEntity> templateBaseEntMap, String recipient, String token) {
		QMessageProvider provider = messageFactory.getMessageProvider(message.getMsgMessageType());
		QBaseMSGMessage msgMessage = provider.setMessageValue(message, templateBaseEntMap, recipient, token);
		
		if(msgMessage != null) {
			logger.info(ANSI_BLUE+">>>>>>>>>>Message info is set<<<<<<<<<<<<"+ANSI_RESET);
			provider.sendMessage(msgMessage);
		} else {
			System.out.println(ANSI_RED+">>>>>>Message wont be sent since baseEntities returned is null<<<<<<<<<"+ANSI_RESET);
		}
		
	}

}
