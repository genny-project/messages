package life.genny.channels;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.Logger;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import io.vertx.core.json.JsonObject;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.util.MessageProcessHelper;

@ApplicationScoped
public class EBCHandlers {
	
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


		@Incoming("messages")
		public void getFromMessages(String arg){
			log.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));

			final JsonObject payload = new JsonObject(arg);

			log.info(payload);
			log.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");

			log.info("GENERIC MESSAGES");
			final QMessageGennyMSG message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
			MessageProcessHelper.processGenericMessage(message, payload.getString("token"));
					
		}

}
