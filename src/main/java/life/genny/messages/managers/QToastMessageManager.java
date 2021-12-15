package life.genny.messages.managers;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.jboss.logging.Logger;

import life.genny.messages.live.data.InternalProducer;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QCmdMessage;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;

@ApplicationScoped
public class QToastMessageManager implements QMessageProvider{
	
	private static final Logger log = Logger.getLogger(QToastMessageManager.class);

	@Inject
	InternalProducer producer;

	Jsonb jsonb = JsonbBuilder.create();

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		
		log.info("About to send toast message");
		
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

		if (target == null) {
			log.error("Target is NULL");
			return;
		}

		// Check for Toast Body
		String body = null;
		if (contextMap.containsKey("BODY")) {
			body = (String) contextMap.get("BODY");
		} else {
			body = templateBe.getValue("PRI_BODY", null);
		}
		if (body == null) {
			log.error("body is NULL");
			return;
		}

		// Check for Toast Style
		String style = null;
		if (contextMap.containsKey("STYLE")) {
			style = (String) contextMap.get("STYLE");
		} else {
			style = templateBe.getValue("PRI_STYLE", "INFO");
		}
		if (style == null) {
			log.error("style is NULL");
			return;
		}

		// Mail Merging Data
		body = MergeUtils.merge(body, contextMap);

		QCmdMessage msg = new QCmdMessage("TOAST", style);
		msg.setMessage(body);
		msg.setToken(beUtils.getGennyToken().getToken());
		msg.setSend(true);

		String json = jsonb.toJson(msg);

		producer.getToWebCmds().send(json);
	}

}
