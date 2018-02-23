package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMSGMessage;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.util.GoogleDocHelper;
import life.genny.util.MergeHelper;

public class QVertxMailManager implements QMessageProvider{
	
	private Vertx vertx;
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, BaseEntity> contextMap) {
		
		vertx = Vertx.vertx();

		MailMessage mailmessage = mailMessage(vertx, message);
		MailClient mailClient = createClient(vertx, contextMap);

		mailClient.sendMail(mailmessage, result -> {
			if (result.succeeded()) {
				System.out.println("email sent to ::"+mailmessage.getTo());
			} else {
				result.cause().printStackTrace();
			}
		});
	}	

	  public MailClient createClient(Vertx vertx, Map<String, BaseEntity> contextMap) {
	    MailConfig config = new MailConfig();
	    BaseEntity projectBe = contextMap.get("PROJECT");
	    
	    config.setHostname(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_MAIL_SMTP_HOST"));
	    config.setPort(Integer.parseInt(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_MAIL_SMTP_PORT")));
	    config.setStarttls(StartTLSOptions.REQUIRED);
	    config.setUsername(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_USERNAME"));
	    config.setPassword(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_PASSWORD"));
	    MailClient mailClient = MailClient.createNonShared(vertx, config);
	    
	    return mailClient;
	  }

	  public MailMessage mailMessage(Vertx vertx, QBaseMSGMessage messageTemplate) {
	    MailMessage message = new MailMessage();
	    message.setFrom(messageTemplate.getSource());
	    message.setTo(messageTemplate.getTarget());
	    message.setSubject(messageTemplate.getSubject());
	    message.setHtml(messageTemplate.getMsgMessageData());
	    
	    return message;
	  }

	  public void attachment(Vertx vertx, MailMessage message) {
	    MailAttachment attachment = new MailAttachment();
	    attachment.setContentType("text/plain");
	    attachment.setData(Buffer.buffer("attachment file"));

	    message.setAttachment(attachment);
	  }

	  public void inlineAttachment(Vertx vertx, MailMessage message) {
	    MailAttachment attachment = new MailAttachment();
	    attachment.setContentType("image/jpeg");
	    attachment.setData(Buffer.buffer("image data"));
	    attachment.setDisposition("inline");
	    attachment.setContentId("<image1@example.com>");

	    message.setInlineAttachment(attachment);
	  }

	@Override
	public QBaseMSGMessage setMessageValue(QMSGMessage message, Map<String, BaseEntity> entityTemplateMap,
			String recipient, String token) {
		return null;
	}

	@Override
	public QBaseMSGMessage setGenericMessageValue(QMessageGennyMSG message, Map<String, BaseEntity> entityTemplateMap,
			String token) {
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		BaseEntity recipientBe = entityTemplateMap.get("RECIPIENT");
		
		if(recipientBe != null) {
			if (template != null) {
					
				baseMessage = new QBaseMSGMessage();
				String emailLink = template.getEmail_templateId();
			
				String urlString = null;
				String innerContentString = null;
				Document doc = null;
				try {
					
					BaseEntity projectBe = entityTemplateMap.get("PROJECT");
					
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
