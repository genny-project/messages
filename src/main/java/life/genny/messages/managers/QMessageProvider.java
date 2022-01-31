package life.genny.messages.managers;

import java.util.Map;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.BaseEntityUtils;

public interface QMessageProvider {
	
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap);

}
