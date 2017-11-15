package life.genny.message;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QEventMessage;

public class QEmailMessageManager implements QMessageProvider{
	
	@Override
	public void sendMessage(QBaseMSGMessage message) {
		
		Properties props = setProperties("emailprops");
		Properties credentials = setProperties("credentials");
		
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(credentials.getProperty("USERNAME"), credentials.getProperty("PASSWORD"));
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


	private static Properties setProperties(String propertyType) {

		Properties properties = new Properties();
		
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(System.getProperty("user.dir") + "/credentials.properties"));
			
			if (propertyType.equalsIgnoreCase("credentials")) {
				properties.put("USERNAME", props.getProperty("USERNAME"));
				properties.put("PASSWORD", props.getProperty("PASSWORD"));
			} else if (propertyType.equalsIgnoreCase("emailprops")) {
				properties.put("mail.smtp.auth", props.getProperty("mail.smtp.auth"));
				properties.put("mail.smtp.starttls.enable", props.getProperty("mail.smtp.starttls.enable"));
				properties.put("mail.smtp.host", props.getProperty("mail.smtp.host"));
				properties.put("mail.smtp.port", props.getProperty("mail.smtp.port"));
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return properties;
	}

}
