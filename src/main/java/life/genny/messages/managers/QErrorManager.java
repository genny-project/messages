package life.genny.messages.managers;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwandautils.ANSIColour;
import life.genny.utils.BaseEntityUtils;

public class QErrorManager implements QMessageProvider {
	
    
    public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		/*
		 * If a message makes it to this point, then something is probably 
		 * wrong with the message or the template.
		 */
		log.error(ANSIColour.RED+"Message Type Supplied was bad. Please check the Message and Template Code!!!!!"+ANSIColour.RESET);

	}
}
