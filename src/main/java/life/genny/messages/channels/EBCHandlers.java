package life.genny.messages.channels;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;
import life.genny.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.messages.process.MessageProcessor;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import javax.enterprise.event.Observes;

import life.genny.qwandautils.ANSIColour;
import life.genny.models.GennyToken;
import life.genny.messages.util.KeycloakUtils;

@ApplicationScoped
public class EBCHandlers {
	
	private static final Logger log = Logger.getLogger(EBCHandlers.class);

	GennyToken serviceToken;

    void onStart(@Observes StartupEvent ev) {
        log.info("The application is starting...");

		log.info("Fetching Environment Variables...");
		String realm = System.getenv("PROJECT_REALM");
		String keycloakUrl = System.getenv("KEYCLOAK_URL");
		String clientId = System.getenv("KEYCLOAK_CLIENT_ID");
		String secret = System.getenv("KEYCLOAK_SECRET");
		String serviceUsername = System.getenv("SERVICE_USERNAME");
		String servicePassword = System.getenv("SERVICE_PASSWORD");

		log.info("Fetching serviceToken...");
		serviceToken = new KeycloakUtils().getToken(keycloakUrl, realm, clientId, secret, serviceUsername, servicePassword, null);
		log.info("Application Ready!");
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }

	@Incoming("messages")
	public void getFromMessages(String arg) {
		log.info("Received EVENT :" + (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

		final JsonObject payload = new JsonObject(arg);

		log.debug(payload);
		log.info("################################################################");
		log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
		log.info("################################################################");

		GennyToken userToken = new GennyToken(payload.getString("token"));

		// Try Catch to stop consumer from dying upon error
		QMessageGennyMSG message = null;
		try {
			message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
		} catch (Exception e) {
			log.error(ANSIColour.RED+"Message Deserialisation Failed!!!!!"+ANSIColour.RESET);
			log.error(ANSIColour.RED+e+ANSIColour.RESET);
		}

		if (message != null) {
			// Try Catch to stop consumer from dying upon error
			try {
				MessageProcessor.processGenericMessage(message, serviceToken, userToken);
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Processing Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+e+ANSIColour.RESET);
			}
		}
				
	}

}
