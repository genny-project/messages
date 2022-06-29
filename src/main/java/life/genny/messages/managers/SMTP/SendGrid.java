package life.genny.messages.managers.SMTP;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/*
* Does not work with MFA
*/

public class SendGrid {

    private static final String SMTP_AUTH_USER = "";
    private static final String SMTP_AUTH_PWD  = "";
    private static final String SMTP_HOST_NAME = "";

    private InternetAddress to;
    private InternetAddress from;
    private String subject;
    private String html;
    private String text;

    public void to(String email, String name) {
        try {
            to = new InternetAddress(email, name);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void to(String email) {
        try {
            to = new InternetAddress(email);
        } catch (AddressException e) {
            e.printStackTrace();
        }
    }

    public void from(String email, String name) {
        try {
            from = new InternetAddress(email, name);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void from(String email) {
        try {
            from = new InternetAddress(email);
        } catch (AddressException e) {
            e.printStackTrace();
        }
    }

    public void subject(String subject) {
        this.subject = subject;
    }

    public void html(String html) {
        this.html = html;
    }

    public void text(String text) {
        this.text = text;
    }

    public void send() {
        try {
            Properties props = new Properties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.host", SMTP_HOST_NAME);
            props.put("mail.smtp.port", 587);
            props.put("mail.smtp.auth", "true");

            Authenticator auth = new SMTPAuthenticator();
            Session mailSession = Session.getInstance(props, auth);
            Transport transport = mailSession.getTransport();

            MimeMessage message = new MimeMessage(mailSession);

            Multipart multipart = new MimeMultipart("alternative");

            if (text != null) {
                BodyPart part1 = new MimeBodyPart();
                part1.setText(text);
                multipart.addBodyPart(part1);
            }

            if (html != null) {
                BodyPart part2 = new MimeBodyPart();
                part2.setContent(html, "text/html");
                multipart.addBodyPart(part2);
            }

            message.setContent(multipart);
            message.setFrom(from);
            message.setSubject(subject);
            message.addRecipient(Message.RecipientType.TO, to);

            transport.connect();
            transport.sendMessage(message,
                    message.getRecipients(Message.RecipientType.TO));
            transport.close();
        } catch(MessagingException e) {
            e.printStackTrace();
        }
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            String username = SMTP_AUTH_USER;
            String password = SMTP_AUTH_PWD;
            return new PasswordAuthentication(username, password);
        }
    }
}