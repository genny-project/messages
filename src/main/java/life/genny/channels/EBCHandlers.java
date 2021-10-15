package life.genny.channels;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.process.MessageProcessor;

import life.genny.qwandautils.ANSIColour;

@ApplicationScoped
public class EBCHandlers {
	
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


		@Incoming("messages")
		public void getFromMessages(String arg){
			log.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

			final JsonObject payload = new JsonObject(arg);

			log.debug(payload);
			log.info("################################################################");
			log.info(">>>>>>>>>>>>>>>>>> PROCESSING NEW MESSAGE <<<<<<<<<<<<<<<<<<<<<<");
			log.info("################################################################");

			// Try Catch to stop consumer from dying upon error
			try {
				final QMessageGennyMSG message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
				MessageProcessor.processGenericMessage(message, payload.getString("token"));
			} catch (Exception e) {
				log.error(ANSIColour.RED+"Message Handling Failed!!!!!"+ANSIColour.RESET);
				log.error(ANSIColour.RED+e+ANSIColour.RESET);
			}
					
		}

}
