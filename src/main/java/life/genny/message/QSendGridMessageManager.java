package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.util.MergeHelper;
import life.genny.notifications.EmailHelper;
import life.genny.utils.BaseEntityUtils;
import java.util.stream.Collectors;

public class QSendGridMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		logger.info("SendGrid email type");

		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

		if (target == null) {
			logger.error("Target is NULL");
		}

		String targetEmail = target.getValue("PRI_EMAIL", null);

		if (targetEmail == null) {
			logger.error("Target " + target.getCode() + ", PRI_EMAIL is NULL");
			return;
		}

		String ccArr = (String) contextMap.get("CC");
		String bccArr = (String) contextMap.get("BCC");

		List<BaseEntity> ccEntities = MergeHelper.convertToBaseEntityArray(ccArr);
		List<BaseEntity> bccEntities = MergeHelper.convertToBaseEntityArray(bccArr);

		List<String> ccList = ccEntities.stream().map(item -> item.getValue("PRI_EMAIL", ""))
				.filter(item -> !item.isEmpty()).collect(Collectors.toList());
		List<String> bccList = bccEntities.stream().map(item -> item.getValue("PRI_EMAIL", ""))
				.filter(item -> !item.isEmpty()).collect(Collectors.toList());

		String templateId = templateBe.getValue("PRI_SENDGRID_ID", null);

		// Build a general data map from context BEs
		HashMap<String, String> templateData = new HashMap<>();

		for (String key : contextMap.keySet()) {

			Object value = contextMap.get(key);

			if (value.getClass().equals(BaseEntity.class)) {
				BaseEntity be = (BaseEntity) value;
				for (EntityAttribute ea : be.getBaseEntityAttributes()) {

					String attrCode = ea.getAttributeCode();
					if (attrCode.startsWith("LNK") || attrCode.startsWith("PRI")) {
						String valueString = ea.getValue().toString();
						templateData.put(key+"."+attrCode, valueString);
					}
				}
			} else if(value.getClass().equals(BaseEntity.class)) {
				templateData.put(key, (String) value);
			}
		}

		// NOTE: This bool determines if email is sent on non-prod servers
		Boolean testFlag = true;

		try {
			EmailHelper.sendGrid(beUtils, targetEmail, ccList, bccList, "", templateId, templateData, testFlag);
		} catch (IOException e) {
			logger.error(e.getStackTrace());
		}

	}

}
