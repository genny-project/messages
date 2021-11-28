package life.genny.channels;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;
import life.genny.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.process.MessageProcessor;

import life.genny.qwandautils.ANSIColour;
import life.genny.models.GennyToken;
import life.genny.util.KeycloakUtils;

@ApplicationScoped
public class EBCHandlers {
	
	private static final Logger log = Logger.getLogger(EBCHandlers.class);

	GennyToken serviceToken;


	@Incoming("messages")
	public void getFromMessages(String arg) {
		log.info("Received EVENT :"
				+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

		final JsonObject payload = new JsonObject(arg);

		log.debug(payload);
		log.info("################################################################");
		log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
		log.info("################################################################");

		String realm = System.getenv("PROJECT_REALM");
		String keycloakUrl = System.getenv("KEYCLOAK_URL");
		String clientId = System.getenv("KEYCLOAK_CLIENT_ID");
		String secret = System.getenv("KEYCLOAK_SECRET");
		String serviceUsername = System.getenv("SERVICE_USERNAME");
		String servicePassword = System.getenv("SERVICE_PASSWORD");

		GennyToken serviceToken = new KeycloakUtils().getToken(keycloakUrl, realm, clientId, secret, serviceUsername, servicePassword, null);
		GennyToken userToken = new GennyToken(payload.getString("token"));

		// Try Catch to stop consumer from dying upon error
		try {
			final QMessageGennyMSG message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
			MessageProcessor.processGenericMessage(message, serviceToken, userToken);
		} catch (Exception e) {
			log.error(ANSIColour.RED+"Message Handling Failed!!!!!"+ANSIColour.RESET);
			log.error(ANSIColour.RED+e+ANSIColour.RESET);
		}
				
	}

}
