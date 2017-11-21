package life.genny.message;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.DataHandler;
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
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.FilenameUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.message.QBaseMSGMessage;

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
				messageTextPart.setContent("<h1>" + message.getMsgMessageData() + "</h1>", "text/html;charset=UTF-8");
				multipart.addBodyPart(messageTextPart);


				if (message.getAttachments() != null) {
					Arrays.stream(message.getAttachments()).forEach(file -> {
						
						//checking for empty filenames in the array
						if (!file.isEmpty()) {
							
							logger.info("initial file name::"+file);
							MimeBodyPart mimePart = new MimeBodyPart();

							try {	
								
								InputStream in = getClass().getResourceAsStream(file);								
								ByteArrayDataSource bbs = new ByteArrayDataSource(in, FILE_TYPE + FilenameUtils.getExtension(file));
								
								/*ByteArrayDataSource bds = new ByteArrayDataSource(bytes,
										FILE_TYPE + FilenameUtils.getExtension(file));*/
								mimePart.setDataHandler(new DataHandler(bbs));
								logger.info("file name ::"+FilenameUtils.getExtension(file));
						        mimePart.setFileName(file);
								multipart.addBodyPart(mimePart);
								
							} catch (MessagingException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					});
				}

				Message msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(message.getSource()));
				msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTarget()));
				msg.setSubject(message.getSubject());
				msg.setContent(multipart);
				
				Transport.send(msg);
				System.out.println("Done");

			}

		} catch (MessagingException e) {
			throw new RuntimeException(e);
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

}
