package life.genny.message;

import java.util.Map;

import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwanda.message.QMessageGennyMSG;

public interface QMessageProvider {
	
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, Object> contextMap);
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, Object> entityTemplateMap, String recipient, String token);
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, Object> entityTemplateMap, String token);

}
