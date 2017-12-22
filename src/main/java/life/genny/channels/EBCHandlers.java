package life.genny.channels;

import java.lang.invoke.MethodHandles;

import com.google.gson.Gson;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.util.MessageProcessHelper;

public class EBCHandlers {
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	static Gson gson = new Gson();

	public static void registerHandlers(final EventBus eventBus) {

		EBConsumers.getFromMessages().subscribe(arg -> {
			logger.info("Received EVENT :"
					+ (System.getenv("PROJECT_REALM") == null ? "tokenRealm" : System.getenv("PROJECT_REALM")));
						
			Vertx.vertx().executeBlocking(arg1->{
				final JsonObject payload = new JsonObject(arg.body().toString());
				
				System.out.println(payload);
				logger.info(">>>>>>>>>>>>>>>>>>GOT THE PAYLOAD IN MESSAGES<<<<<<<<<<<<<<<<<<<<<<");
				final QMSGMessage message = gson.fromJson(payload.toString(), QMSGMessage.class);
				
				if(message.getCode() != null){
					//for test_comm
					MessageProcessHelper.processTestMessage(message, payload.getString("token"));
					
				} else {
					//for BEG bucket shift
					MessageProcessHelper.processMessage(message, payload.getString("token"));
				}
				
			}, arg2->{
				
			});			

		});

	}

}
