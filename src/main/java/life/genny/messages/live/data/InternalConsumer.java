package life.genny.messages.live.data;

import org.jboss.logging.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.apache.commons.lang3.exception.ExceptionUtils;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import life.genny.messages.process.MessageProcessor;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennyToken;
import life.genny.serviceq.Service;

@ApplicationScoped
public class InternalConsumer {

	private static final Logger log = Logger.getLogger(InternalConsumer.class);

	Jsonb jsonb = JsonbBuilder.create();

	@Inject
	Service service;

    void onStart(@Observes StartupEvent ev) {

		service.fullServiceInit();
		log.info("[*] Consumer Ready!");
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("The application is stopping...");
    }

	@Incoming("messages")
	public void getFromMessages(String payload) {

		log.info("Received EVENT :" + (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

		log.info("################################################################");
		log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
		log.info("################################################################");

		// Log entire payload for debugging purposes
		log.info("payload ----> " + payload);
		
		QMessageGennyMSG message = null;
		GennyToken userToken = null;

		// Try Catch to stop consumer from dying upon error
		try {

			message = jsonb.fromJson(payload, QMessageGennyMSG.class);
			userToken = new GennyToken(message.getToken());
		} catch (Exception e) {
			log.error(ANSIColour.RED+"Message Deserialisation Failed!!!!!"+ANSIColour.RESET);
			log.error(ANSIColour.RED+ExceptionUtils.getStackTrace(e)+ANSIColour.RESET);
		}

		if (message != null && userToken != null) {

			// update the beUtils with new token
			service.getBeUtils().setGennyToken(userToken);

			// Try Catch to stop consumer from dying upon error
			try {
				MessageProcessor.processGenericMessage(message, service);
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Processing Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+ExceptionUtils.getStackTrace(e)+ANSIColour.RESET);
			}
		}
	}
}
