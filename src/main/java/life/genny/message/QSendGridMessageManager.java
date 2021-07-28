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

public class QSendGridMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(BaseEntityUtils beUtils, QBaseMSGMessage message, Map<String, Object> contextMap) {

		logger.info("SendGrid email type");

		String target = message.getTarget();
		if (target != null && !target.isEmpty()) {

			List<String> ccList = new ArrayList<>();
			List<String> bccList = new ArrayList<>();

			String templateId = message.getSubject();

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
				EmailHelper.sendGrid(beUtils, target, ccList, bccList, "", templateId, templateData, testFlag);
			} catch (IOException e) {
				logger.error(e.getStackTrace());
			}

		}

	}

	@Override
	public QBaseMSGMessage setGenericMessageValue(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap) {

		String token = beUtils.getGennyToken().getToken();
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = (BaseEntity)entityTemplateMap.get("RECIPIENT");
		
		if(recipientBe != null) {
			if (template != null) {
					
				baseMessage = new QBaseMSGMessage();
				String emailLink = template.getEmail_templateId();
			
				String urlString = null;
				String innerContentString = null;
				Document doc = null;
				try {
					
					BaseEntity projectBe = (BaseEntity)entityTemplateMap.get("PROJECT");
					
					if(projectBe != null) {
						
						/* Getting base email template from project google doc */
						urlString = QwandaUtils.apiGet(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "NTF_BASE_TEMPLATE"), null);	
						
						/* Getting content email template from notifications-doc and merging with contextMap */
						innerContentString = MergeUtil.merge(QwandaUtils.apiGet(emailLink, null), entityTemplateMap);
						
						/* Inserting the content html into the main email html */
						doc = Jsoup.parse(urlString);
						Element element = doc.getElementById("content");
						element.html(innerContentString);
						
						baseMessage.setSource(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_USERNAME"));
						baseMessage.setSubject(template.getSubject());
						baseMessage.setMsgMessageData(doc.toString());
						baseMessage.setTarget(MergeUtil.getBaseEntityAttrValueAsString(recipientBe, "PRI_EMAIL"));	
						
					} else {
						logger.error("NO PROJECT BASEENTITY FOUND");
					}
					
				} catch (IOException e) {
					logger.error("ERROR", e);
				}
											
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		
		
		return baseMessage;
	}

	@Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String to) {
		
		String token = beUtils.getGennyToken().getToken();

		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
	
		if (template != null) {
				
			baseMessage = new QBaseMSGMessage();
			String emailLink = template.getEmail_templateId();
		
			String urlString = null;
			String innerContentString = null;
			Document doc = null;
			
			try {
				
				BaseEntity projectBe = (BaseEntity)entityTemplateMap.get("PROJECT");
				
				if(projectBe != null) {
					
					/* Getting base email template from project google doc */
					urlString = QwandaUtils.apiGet(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "NTF_BASE_TEMPLATE"), null);	
					
					/* Getting content email template from notifications-doc and merging with contextMap */
					innerContentString = MergeUtil.merge(QwandaUtils.apiGet(emailLink, null), entityTemplateMap);
					
					/* Inserting the content html into the main email html */
					doc = Jsoup.parse(urlString);
					Element element = doc.getElementById("content");
					element.html(innerContentString);
					
					baseMessage.setSource(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_USERNAME"));
					baseMessage.setSubject(template.getSubject());
					baseMessage.setMsgMessageData(doc.toString());
					baseMessage.setTarget(to);	
					
				} else {
					logger.error("NO PROJECT BASEENTITY FOUND");
				}
				
				} catch (IOException e) {
					logger.error("ERROR", e);
			}
										
		} else {
			logger.error("NO TEMPLATE FOUND");
		}	
		
		return baseMessage;
	}

	

}
