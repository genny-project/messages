package life.genny.channels;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.message.QMessageFactory;
import life.genny.message.QMessageProvider;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QDataMessageIntf;
import life.genny.qwanda.message.QEventMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.KeycloakUtils;

public class EBCHandlers {

	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static Gson gson = new Gson();
	//= new GsonBuilder()
//			.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
//				@Override
//				public LocalDateTime deserialize(final JsonElement json, final Type type,
//						final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
//					return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()).toLocalDateTime();
//				}
//
//				public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
//						final JsonSerializationContext context) {
//					return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // "yyyy-mm-dd"
//				}
//			}).create();
//
//	final static String qwandaApiUrl = System.getenv("REACT_APP_QWANDA_API_URL");
//	final static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");
//	final static String hostIp = System.getenv("HOSTIP");
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
			List<QBaseMSGMessage> baseMsgList =  message.getMsgMessageData();

			if(baseMsgList != null && baseMsgList.size() > 0){
				processMessage(baseMsgList, eventBus, token);
			}

		});

	}

	private static void processMessage(List<QBaseMSGMessage> basemsglist, EventBus eventBus, String token) {
			
			// triggers message depending on the message type
			basemsglist.forEach(msgMessage -> {
				logger.info("about to trigger message");
				QMessageProvider provider = messageFactory.getMessageProvider(msgMessage.getMsgMessageType());
				provider.sendMessage(msgMessage);
				logger.info("message triggered");
			});
		
	}

}
