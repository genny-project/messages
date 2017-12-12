package life.genny.message;

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

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.GoogleDocHelper;
import life.genny.util.MergeHelper;

public class QEmailMessageManager implements QMessageProvider {
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message) {

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
				//msg.setFrom(new InternetAddress(System.getenv("EMAIL_USERNAME")));
				msg.setFrom(new InternetAddress(message.getSource()));
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTarget()));
				msg.setSubject(message.getSubject());
				msg.setContent(message.getMsgMessageData(), "text/html; charset=utf-8");
				
				Transport.send(msg);
				logger.info(ANSI_GREEN + "Email sent" + ANSI_RESET);

			}

		} catch (MessagingException e) {
			//throw new RuntimeException(e);
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap, String recipient) {
		
		QBaseMSGMessage baseMessage = null;
		Map templateMap = MergeHelper.getTemplate(message.getTemplate_code());

		if (templateMap != null) {
			String docId = templateMap.get("email").toString();
			String htmlString = GoogleDocHelper.getGoogleDocString(docId);
			logger.info(ANSI_GREEN + "email doc ID from google sheet ::" + docId + ANSI_RESET);
			BaseEntity be = entityTemplateMap.get(recipient);

			if (be != null) {

				// working on templates
				baseMessage = new QBaseMSGMessage();
				baseMessage.setSubject(templateMap.get("subject").toString());
				baseMessage.setMsgMessageData(MergeUtil.merge(htmlString, entityTemplateMap));
				baseMessage.setSource(System.getenv("EMAIL_USERNAME"));
				baseMessage.setAttachments(message.getAttachments());
				baseMessage.setTarget(MergeUtil.getBaseEntityAttrValue(be, "PRI_EMAIL"));
				logger.info(ANSI_GREEN + "Setting the targer email id ::"+baseMessage.getTarget() + ANSI_RESET);
			} else {
				System.out.println("BaseEntity for the mail recipient is null");
			}
		}		
		
		return baseMessage;
	}
	

}
