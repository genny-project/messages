package life.genny.message;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import life.genny.qwanda.message.QBaseMSGMessage;

public class QEmailMessageManager implements QMessageProvider {

	@Override
	public void sendMessage(QBaseMSGMessage message) {

		Properties emailProperties = setProperties();

		Session session = Session.getInstance(emailProperties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(System.getenv("EMAIL_USERNAME"),
						System.getenv("EMAIL_PASSWORD"));
			}
		});

		try {

			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(message.getSource()));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(message.getTarget()));
			msg.setSubject(message.getSubject());
			msg.setText(message.getMsgMessageData());

			Transport.send(msg);

			System.out.println("Done");

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
