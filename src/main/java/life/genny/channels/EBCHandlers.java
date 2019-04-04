package life.genny.channels;

import java.lang.invoke.MethodHandles;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.channel.Consumer;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.util.MessageProcessHelper;

public class EBCHandlers {
	
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	public static void registerHandlers(final EventBus eventBus) {

		Consumer.getFromMessages().subscribe(arg -> {
			log.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));
						
			Vertx.vertx().executeBlocking(arg1->{
				final JsonObject payload = new JsonObject(arg.body().toString());
				
				log.info(payload);
				log.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
				
				log.info("GENERIC MESSAGES");
					final QMessageGennyMSG message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
					MessageProcessHelper.processGenericMessage(message, payload.getString("token"), eventBus);
					
			
				
			}, arg2->{
				
			});			

		});

	}

}
