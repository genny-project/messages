package life.genny.message;

import java.util.Map;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;

public interface QMessageProvider {
	
	public void sendMessage(QBaseMSGMessage message);
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap, String recipient);
}
