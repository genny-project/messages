package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
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
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.util.MergeHelper;

public class QEmailMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus bus, Map<String, Object> contextMap) {

		Properties emailProperties = setProperties();

		Session session = Session.getInstance(emailProperties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(System.getenv("EMAIL_USERNAME"), System.getenv("EMAIL_PASSWORD"));
			}
		});

		try {
			
	        logger.info("email type");
	        
			String target = message.getTarget();
			if (target != null && !target.isEmpty()) {

				MimeMessage msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(message.getSource()));
				
				//InternetAddress[] iAdressArray = InternetAddress.parse(message.getTarget());
				//msg.setRecipients(Message.RecipientType.TO, );
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTarget(), false));
				msg.setSubject(message.getSubject());
				msg.setContent(message.getMsgMessageData(), "text/html; charset=utf-8");
				
				Transport.send(msg, msg.getAllRecipients());
				logger.info(ANSI_GREEN + "Email to " + message.getTarget() +" is sent" + ANSI_RESET);

			}

		} catch (MessagingException e) {
			//throw new RuntimeException(e);
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
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
	


	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, Object> entityTemplateMap,
			String token) {
		
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
					e.printStackTrace();
				}
											
			} else {
				logger.error("NO TEMPLATE FOUND");
			}
		} else {
			logger.error("Recipient BaseEntity is NULL");
		}
		
		
		return baseMessage;
	}
	

}
