package life.genny.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;

public class MergeHelper {
	
	public static Map<String, String> getKeyEntityAttrMap(QMSGMessage message) {
		
		String[] messageDataArr = message.getMsgMessageData();
		Map<String, String> keyEntityMap = new HashMap<>();
		if (messageDataArr != null && messageDataArr.length > 0) {

			for (String data : messageDataArr) {

				String[] dataEntity = StringUtils.split(data, ":");
				if (dataEntity.length > 0) {

					keyEntityMap.put(dataEntity[0], dataEntity[1]);

				}

			}

		}
		System.out.println("key entity map ::" + keyEntityMap);
		return keyEntityMap;
	}
	
	
	public static QBaseMSGMessageTemplate getTemplate(String templateCode, String token) {
		
		QBaseMSGMessageTemplate template = MergeUtil.getTemplate(templateCode, token);
		return template;
	}

}
