package life.genny.messages.live.data;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;

import life.genny.messages.process.MessageProcessor;
import life.genny.qwandaq.data.GennyCache;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.CacheUtils;
import life.genny.qwandaq.utils.DatabaseUtils;
import life.genny.qwandaq.utils.DefUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.QwandaUtils;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(InternalConsumer.class);

	@ConfigProperty(name = "genny.keycloak.url", defaultValue = "https://keycloak.gada.io")
	String keycloakUrl;

	@ConfigProperty(name = "genny.keycloak.realm", defaultValue = "genny")
	String keycloakRealm;

	@ConfigProperty(name = "genny.service.username", defaultValue = "service")
	String serviceUsername;

	@ConfigProperty(name = "genny.service.password", defaultValue = "password")
	String servicePassword;

	@ConfigProperty(name = "genny.oidc.client-id", defaultValue = "backend")
	String clientId;

	@ConfigProperty(name = "genny.oidc.credentials.secret", defaultValue = "secret")
	String secret;

	@Inject
	EntityManager entityManager;

	@Inject
	GennyCache cache;

	GennyToken serviceToken;

	BaseEntityUtils beUtils;

	Jsonb jsonb = JsonbBuilder.create();

    void onStart(@Observes StartupEvent ev) {

        log.info("The application is starting...");
		serviceToken = new KeycloakUtils().getToken(keycloakUrl, keycloakRealm, clientId, secret, serviceUsername, servicePassword, null);

		// Init Utility Objects
		beUtils = new BaseEntityUtils(serviceToken);

		// Establish connection to DB and cache, and init utilities
		DatabaseUtils.init(entityManager);
		CacheUtils.init(cache);
		QwandaUtils.init(serviceToken);
		// DefUtils.init(beUtils);

		log.info("[*] Consumer Ready!");
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }

	@Incoming("messages")
	public void getFromMessages(String payload) {

		log.info("Received EVENT :" + (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

		// Log entire payload for debugging purposes
		log.debug(payload);

		log.info("################################################################");
		log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
		log.info("################################################################");

		QMessageGennyMSG message = null;
		GennyToken userToken = null;

		// Try Catch to stop consumer from dying upon error
		try {
			message = jsonb.fromJson(payload, QMessageGennyMSG.class);
			userToken = new GennyToken(message.getToken());
		} catch (Exception e) {
			log.error(ANSIColour.RED+"Message Deserialisation Failed!!!!!"+ANSIColour.RESET);
			log.error(ANSIColour.RED+e+ANSIColour.RESET);
		}

		if (message != null && userToken != null) {
			// Try Catch to stop consumer from dying upon error
			try {
				MessageProcessor.processGenericMessage(message, beUtils);
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Processing Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+e+ANSIColour.RESET);
			}
		}
	}
}
