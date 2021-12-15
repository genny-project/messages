package life.genny.messages.managers;

import java.util.Map;

import org.jboss.logging.Logger;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.utils.BaseEntityUtils;

public class QErrorManager implements QMessageProvider {
	
    
    public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger log = Logger.getLogger(QErrorManager.class);
	
	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		/*
		 * If a message makes it to this point, then something is probably 
		 * wrong with the message or the template.
		 */
		log.error(ANSIColour.RED+"Message Type Supplied was bad. Please check the Message and Template Code!!!!!"+ANSIColour.RESET);

	}
}
