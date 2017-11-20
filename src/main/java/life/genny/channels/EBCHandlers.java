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

	static Gson gson = new GsonBuilder()
			.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
				@Override
				public LocalDateTime deserialize(final JsonElement json, final Type type,
						final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
					return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()).toLocalDateTime();
				}

				public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
						final JsonSerializationContext context) {
					return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // "yyyy-mm-dd"
				}
			}).create();

	final static String qwandaApiUrl = System.getenv("REACT_APP_QWANDA_API_URL");
	final static String vertxUrl = System.getenv("REACT_APP_VERTX_URL");
	final static String hostIp = System.getenv("HOSTIP");
	final static String defaultSmsProvider = System.getenv("DEFAULT_SMS_PROVIDER_CODE");
	final static String defaultMailProvider = System.getenv("DEFAULT_MAIL_PROVIDER_CODE");
	final static String defaultVoiceProvider = System.getenv("DEFAULT_VOICE_PROVIDER_CODE");

	final static String twilio_source = System.getenv("TWILIO_SOURCE_PHONE");
	final static String smtp_host = System.getenv("mail.smtp.host");

	static String token;
	// static MessageProvider messageProvider;
	static QMessageFactory messageFactory = new QMessageFactory();

	public static void registerHandlers(final EventBus eventBus) {
		/*
		 * EBConsumers.getFromEvents().subscribe(arg -> {
		 * logger.info("Received EVENT :" + (System.getenv("PROJECT_REALM") ==
		 * null ? "tokenRealm" : System.getenv("PROJECT_REALM")));
		 * 
		 * final JsonObject payload = new JsonObject(arg.body().toString());
		 * final String token = payload.getString("token");
		 * System.out.println(payload); final QEventMessage eventMsg =
		 * gson.fromJson(payload.toString(), QEventMessage.class);
		 * 
		 * if(eventMsg.getMsgMessageData() != null) { processEvent(eventMsg,
		 * eventBus, token,
		 * messageFactory.getMessageProvider(eventMsg.getMsgMessageData().
		 * getMsgMessageType())); } final JsonObject object =
		 * gson.fromJson(payload.toString(), JsonObject.class);
		 * System.out.println("object value"+object.getString("hello"));
		 * 
		 * 
		 * });
		 * 
		 * EBConsumers.getFromData().subscribe(arg -> {
		 * 
		 * logger.info("Received DATA :" + (System.getenv("PROJECT_REALM") ==
		 * null ? "tokenRealm" : System.getenv("PROJECT_REALM"))); final
		 * JsonObject payload = new JsonObject(arg.body().toString()); final
		 * String token = payload.getString("token"); final QDataMessageIntf
		 * dataMsg = gson.fromJson(payload.toString(), QDataMessageIntf.class);
		 * processData(dataMsg, eventBus, token);
		 * 
		 * });
		 */

		EBConsumers.getFromMessages().subscribe(arg -> {
			logger.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

			final JsonObject payload = new JsonObject(arg.body().toString());
			final String token = payload.getString("token");
			System.out.println(payload);
			logger.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
			final QMSGMessage message = gson.fromJson(payload.toString(), QMSGMessage.class);
			List<QBaseMSGMessage> baseMsgList = (List<QBaseMSGMessage>) message.getMsgMessageData();

			if (baseMsgList != null && baseMsgList.size() > 0) {
				processMessage(baseMsgList, eventBus, token);
			}

			logger.info("smtp host from environment variables ::" + smtp_host);
			logger.info("twilio source phone ::" + twilio_source);

		});

	}

	private static void processMessage(List<QBaseMSGMessage> basemsglist, EventBus eventBus, String token) {
		Vertx.vertx().executeBlocking(future -> {
			// Getting decoded token in Hash Map from QwandaUtils
			final Map<String, Object> decodedToken = KeycloakUtils.getJsonMap(token);
			// Getting Set of User Roles from QwandaUtils
			final Set<String> userRoles = KeycloakUtils.getRoleSet(decodedToken.get("realm_access").toString());

			System.out.println("The Roles value are: " + userRoles.toString());

			/*
			 * Getting Prj Realm name from KeyCloakUtils - Just cheating the
			 * keycloak realm names as we can't add multiple realms in genny
			 * keyclaok as it is open-source
			 */
			final String projectRealm = KeycloakUtils.getPRJRealmFromDevEnv();
			if ((projectRealm != null) && (!projectRealm.isEmpty())) {
				decodedToken.put("realm", projectRealm);
			} else {
				// Extracting realm name from iss value
				final String realm = (decodedToken.get("iss").toString()
						.substring(decodedToken.get("iss").toString().lastIndexOf("/") + 1));
				// Adding realm name to the decoded token
				decodedToken.put("realm", realm);
			}
			System.out.println("######  The realm name is:  #####  " + decodedToken.get("realm"));
			// Printing Decoded Token values
			for (final Map.Entry entry : decodedToken.entrySet()) {
				System.out.println(entry.getKey() + ", " + entry.getValue());
			}
			
			// triggers message depending on the message type
			basemsglist.forEach(msgMessage -> {
				logger.info("about to trigger message");
				QMessageProvider provider = messageFactory.getMessageProvider(msgMessage.getMsgMessageType());
				provider.sendMessage(msgMessage);
				logger.info("message triggered");
			});
			

			future.complete();
		}, res -> {
			if (res.succeeded()) {
				System.out.println("ProcessedEvent");
			}
		});

	}

	public static void processEvent(final QEventMessage eventMsg, final EventBus bus, final String token,
			QMessageProvider messageProvider) {
		Vertx.vertx().executeBlocking(future -> {
			// Getting decoded token in Hash Map from QwandaUtils
			final Map<String, Object> decodedToken = KeycloakUtils.getJsonMap(token);
			// Getting Set of User Roles from QwandaUtils
			final Set<String> userRoles = KeycloakUtils.getRoleSet(decodedToken.get("realm_access").toString());

			System.out.println("The Roles value are: " + userRoles.toString());

			/*
			 * Getting Prj Realm name from KeyCloakUtils - Just cheating the
			 * keycloak realm names as we can't add multiple realms in genny
			 * keyclaok as it is open-source
			 */
			final String projectRealm = KeycloakUtils.getPRJRealmFromDevEnv();
			if ((projectRealm != null) && (!projectRealm.isEmpty())) {
				decodedToken.put("realm", projectRealm);
			} else {
				// Extracting realm name from iss value
				final String realm = (decodedToken.get("iss").toString()
						.substring(decodedToken.get("iss").toString().lastIndexOf("/") + 1));
				// Adding realm name to the decoded token
				decodedToken.put("realm", realm);
			}
			System.out.println("######  The realm name is:  #####  " + decodedToken.get("realm"));
			// Printing Decoded Token values
			for (final Map.Entry entry : decodedToken.entrySet()) {
				System.out.println(entry.getKey() + ", " + entry.getValue());
			}

			// triggers message service depending on the type
			// messageProvider.sendMessage(eventMsg.getMsgMessageData());

			future.complete();
		}, res -> {
			if (res.succeeded()) {
				System.out.println("ProcessedEvent");
			}
		});

	}

	/*
	 * public static void processEvent(final QEventMessage eventMsg, final
	 * EventBus bus, final String token, QMessageProvider messageProvider) {
	 * Vertx.vertx().executeBlocking(future -> { // Getting decoded token in
	 * Hash Map from QwandaUtils final Map<String, Object> decodedToken =
	 * KeycloakUtils.getJsonMap(token); // Getting Set of User Roles from
	 * QwandaUtils final Set<String> userRoles =
	 * KeycloakUtils.getRoleSet(decodedToken.get("realm_access").toString());
	 * 
	 * System.out.println("The Roles value are: " + userRoles.toString());
	 * 
	 * 
	 * Getting Prj Realm name from KeyCloakUtils - Just cheating the keycloak
	 * realm names as we can't add multiple realms in genny keyclaok as it is
	 * open-source
	 * 
	 * final String projectRealm = KeycloakUtils.getPRJRealmFromDevEnv(); if
	 * ((projectRealm != null) && (!projectRealm.isEmpty())) {
	 * decodedToken.put("realm", projectRealm); } else { // Extracting realm
	 * name from iss value final String realm =
	 * (decodedToken.get("iss").toString()
	 * .substring(decodedToken.get("iss").toString().lastIndexOf("/") + 1)); //
	 * Adding realm name to the decoded token decodedToken.put("realm", realm);
	 * } System.out.println("######  The realm name is:  #####  " +
	 * decodedToken.get("realm")); // Printing Decoded Token values for (final
	 * Map.Entry entry : decodedToken.entrySet()) {
	 * System.out.println(entry.getKey() + ", " + entry.getValue()); }
	 * 
	 * 
	 * //triggers message service depending on the type
	 * messageProvider.sendMessage(eventMsg);
	 * 
	 * future.complete(); }, res -> { if (res.succeeded()) {
	 * System.out.println("ProcessedEvent"); } });
	 * 
	 * }
	 */

	public static void processData(final QDataMessageIntf dataMsg, final EventBus bus, final String token) {
		Vertx.vertx().executeBlocking(future -> {
			// Getting decoded token in Hash Map from QwandaUtils
			final Map<String, Object> decodedToken = KeycloakUtils.getJsonMap(token);
			// Getting Set of User Roles from QwandaUtils
			final Set<String> userRoles = KeycloakUtils.getRoleSet(decodedToken.get("realm_access").toString());

			System.out.println("The Roles value are: " + userRoles.toString());

			/*
			 * Getting Prj Realm name from KeyCloakUtils - Just cheating the
			 * keycloak realm names as we can't add multiple realms in genny
			 * keyclaok as it is open-source
			 */
			final String projectRealm = KeycloakUtils.getPRJRealmFromDevEnv();
			if ((projectRealm != null) && (!projectRealm.isEmpty())) {
				decodedToken.put("realm", projectRealm);
			} else {
				// Extracting realm name from iss value
				final String realm = (decodedToken.get("iss").toString()
						.substring(decodedToken.get("iss").toString().lastIndexOf("/") + 1));
				// Adding realm name to the decoded token
				decodedToken.put("realm", realm);
			}
			System.out.println("######  The realm name is:  #####  " + decodedToken.get("realm"));
			// Printing Decoded Token values
			for (final Map.Entry entry : decodedToken.entrySet()) {
				System.out.println(entry.getKey() + ", " + entry.getValue());
			}

			/*
			 * DO STUFF HERE
			 */
			future.complete();
		}, res -> {
			if (res.succeeded()) {
				System.out.println("ProcessedData");
			}
		});

	}

}
