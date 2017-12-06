package life.genny.message;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class QEmailMessageManager implements QMessageProvider {
	
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

				Multipart multipart = new MimeMultipart();

				//To set message data in the mail
				MimeBodyPart messageTextPart = new MimeBodyPart();

				MimeMessage msg = new MimeMessage(session);
				//msg.setFrom(new InternetAddress(System.getenv("EMAIL_USERNAME")));
				msg.setFrom(new InternetAddress(message.getSource()));
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTarget()));
				msg.setSubject(message.getSubject());
				msg.setContent(message.getMsgMessageData(), "text/html; charset=utf-8");
				
				Transport.send(msg);
				System.out.println("Done");

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
	
	private String getHtmlContent(String url) throws Exception, IOException {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);

		String html = "";
		InputStream in = response.getEntity().getContent();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		StringBuilder str = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			str.append(line);
		}
		in.close();
		html = str.toString();
		System.out.println("string html ::"+html);

		return html;
	}


	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap, String recipient) {
		
		final String messageTemplate = message.getTemplate_code();
		QBaseMSGMessage baseMessage = new QBaseMSGMessage();
		
		
		String messageData;
		//use this, still under dev
		messageData = MergeUtil.merge(messageTemplate, entityTemplateMap);
		
		//messageData = MergeUtil.merge(FileUtils.readFileToString(new File(""), Charset.defaultCharset()), entityTemplateMap);
		baseMessage.setMsgMessageData(messageData);
		baseMessage.setSource(System.getenv("EMAIL_USERNAME"));
		baseMessage.setAttachments(message.getAttachments());
		
		BaseEntity be = entityTemplateMap.get(recipient);
		//use this
		//baseMessage.setTarget(MergeHelper.getBaseEntityAttrValue(be, "PRI_EMAIL"));
		
		System.out.println("sender email value ::"+MergeHelper.getBaseEntityAttrValue(be, "PRI_EMAIL"));
		baseMessage.setTarget(System.getenv("EMAIL_USERNAME"));
		
		
		
		return baseMessage;
	}
	

}
