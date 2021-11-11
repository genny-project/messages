package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
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
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.ANSIColour;
import life.genny.util.MergeHelper;
import life.genny.utils.BaseEntityUtils;

public class QEmailMessageManager implements QMessageProvider {
	
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		Properties emailProperties = setProperties();

		Session session = Session.getInstance(emailProperties, new javax.mail.Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				
				return new PasswordAuthentication(System.getenv("EMAIL_USERNAME"), System.getenv("EMAIL_PASSWORD"));
			}
		});

		try {
			
	        log.info("email type");

			BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
			BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");
			
			if (target == null) {
				log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
				return;
			}
			if (projectBe == null) {
				log.error(ANSIColour.RED+"ProjectBe is NULL"+ANSIColour.RESET);
				return;
			}

			String targetEmail = target.getValue("PRI_EMAIL", null);

			if (targetEmail == null) {
				log.error(ANSIColour.RED+"Target " + target.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
				return;
			}

			String body = templateBe.getValue("PRI_BODY", null);
			String subject = templateBe.getValue("PRI_SUBJECT", null);
			String sender = projectBe.getValue("ENV_EMAIL_USERNAME", null);

			if (body == null) {
				log.error(ANSIColour.RED+"Template BE " + templateBe.getCode() + ", PRI_BODY is NULL"+ANSIColour.RESET);
				return;
			}
			if (subject == null) {
				log.error(ANSIColour.RED+"Template BE " + templateBe.getCode() + ", PRI_SUBJECT is NULL"+ANSIColour.RESET);
				return;
			}
			if (sender == null) {
				log.error(ANSIColour.RED+"Project BE " + templateBe.getCode() + ", ENV_EMAIL_USERNAME is NULL"+ANSIColour.RESET);
				return;
			}

			// Mail Merging Data
			body = MergeUtil.merge(body, contextMap);

			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(sender));
			
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail, false));
			msg.setSubject(subject);
			msg.setContent(body, "text/html; charset=utf-8");
			
			Transport.send(msg, msg.getAllRecipients());
			log.info(ANSIColour.GREEN + "Email to " + targetEmail +" is sent" + ANSIColour.RESET);

		} catch (Exception e) {
			log.error("ERROR", e);
		} 

	}

	private static Properties setProperties() {

		Properties properties = new Properties();

		properties.put("mail.smtp.auth", System.getenv("MAIL_SMTP_AUTH"));
		properties.put("mail.smtp.starttls.enable", System.getenv("MAIL_SMTP_STARTTLS_ENABLE"));
		properties.put("mail.smtp.host", System.getenv("MAIL_SMTP_HOST"));
		properties.put("mail.smtp.port", System.getenv("MAIL_SMTP_PORT"));

		return properties;
	}
	


	// @Override
	public QBaseMSGMessage setGenericMessageValue(BaseEntityUtils beUtils, QMessageGennyMSG message, Map<String, Object> entityTemplateMap) {
		
		String token = beUtils.getGennyToken().getToken();

		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplateCode(), token);
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
						log.error("NO PROJECT BASEENTITY FOUND");
					}
					
				} catch (IOException e) {
					log.error("ERROR", e);
				}
											
			} else {
				log.error("NO TEMPLATE FOUND");
			}
		} else {
			log.error("Recipient BaseEntity is NULL");
		}
		
		
		return baseMessage;
	}

	// @Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(BaseEntityUtils beUtils, QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String to) {

		String token = beUtils.getGennyToken().getToken();
		
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplateCode(), token);
	
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
					log.error("NO PROJECT BASEENTITY FOUND");
				}
				
				} catch (IOException e) {
					log.error("ERROR", e);
			}
										
		} else {
			log.error("NO TEMPLATE FOUND");
		}	
		
		return baseMessage;
	}

	

}
