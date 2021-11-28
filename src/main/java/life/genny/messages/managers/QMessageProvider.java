package life.genny.messages.managers;

import java.util.Map;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.message.QMessageGennyMSG;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.utils.BaseEntityUtils;

public interface QMessageProvider {
	
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap);

}
