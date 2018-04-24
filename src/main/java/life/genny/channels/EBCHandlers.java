package life.genny.channels;

import java.lang.invoke.MethodHandles;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.channel.Consumer;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.JsonUtils;
import life.genny.util.MessageProcessHelper;

public class EBCHandlers {
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	public static void registerHandlers(final EventBus eventBus) {

		Consumer.getFromMessages().subscribe(arg -> {
			logger.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));
						
			Vertx.vertx().executeBlocking(arg1->{
				final JsonObject payload = new JsonObject(arg.body().toString());
				
				System.out.println(payload);
				logger.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
				
					System.out.println("GENERIC MESSAGES");
					final QMessageGennyMSG message = JsonUtils.fromJson(payload.toString(), QMessageGennyMSG.class);
					MessageProcessHelper.processGenericMessage(message, payload.getString("token"), eventBus);
					
			
				
			}, arg2->{
				
			});			

		});

	}

}
