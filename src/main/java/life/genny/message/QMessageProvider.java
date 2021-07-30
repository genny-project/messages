package life.genny.message;

import java.util.Map;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.utils.BaseEntityUtils;

public interface QMessageProvider {
	
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap);
	// public QBaseMSGMessage setGenericMessageValue(BaseEntityUtils beUtils, QMessageGennyMSG message, Map<String, Object> entityTemplateMap);
	// public QBaseMSGMessage setGenericMessageValueForDirectRecipient(BaseEntityUtils beUtils, QMessageGennyMSG message, Map<String, Object> entityTemplateMap, String to);

}
