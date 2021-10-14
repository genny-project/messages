package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.ANSIColour;
import life.genny.util.MergeHelper;
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
