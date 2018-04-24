package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
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
import life.genny.qwanda.message.QBaseMSGAttachment;
import life.genny.qwanda.message.QBaseMSGAttachment.AttachmentType;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.util.MergeHelper;

public class QVertxMailManager implements QMessageProvider{
	
	private Vertx vertx;
	
	public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
	
	public static final String FILE_TYPE = "application/";
	
	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";
	private static String hostIP = System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : "127.0.0.1";
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, Object> contextMap) {
		
		vertx = Vertx.vertx();

		MailMessage mailmessage = mailMessage(vertx, message);
		
		//If message has attachments, process them seperately
		if(message.getAttachmentList() != null && message.getAttachmentList().size() > 0) {
			List<MailAttachment> attachmentList = setGenericAttachmentsInMailMessage(message.getAttachmentList(), contextMap);
			mailmessage.setAttachment(attachmentList);
		}
		
		MailClient mailClient = createClient(vertx, contextMap);

		mailClient.sendMail(mailmessage, result -> {
			if (result.succeeded()) {
				System.out.println("email sent to ::"+mailmessage.getTo());
			} else {
				result.cause().printStackTrace();
			}
		});
	}	

	  public MailClient createClient(Vertx vertx, Map<String, Object> contextMap) {
	    MailConfig config = new MailConfig();
	    BaseEntity projectBe = (BaseEntity)contextMap.get("PROJECT");
	    
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

	  public MailAttachment getAttachment(QBaseMSGAttachment message, Map<String, Object> contextMap) {
		  
	    MailAttachment attachment = null;
	    
	    /* Content can be htmlString or an URL of any resource */
	    String content = null;
	    
	    /* Content after converting into Base64 bytes */
	    byte[] contentBytes = null;
	    
		if (message.getIsMergeRequired()) {

			try {
				/* Get content from link in String format */
				String linkString = QwandaUtils.apiGet(message.getLink(), null);

				/* If merge is required, use MergeUtils for merge with context map */
				content = MergeUtil.merge(linkString, contextMap);

			} catch (IOException e) {
				e.printStackTrace();
			}

		} else {
			content = message.getLink();
		}
		
		if(content != null) {
			
			if(message.getContentType().equalsIgnoreCase("application/pdf")) {
				
				/* IF CONTENT TYPE IS PDF :: Hit Camelot service to fetch pdf filepath. Camelot service converts html strings into PDF format using puppetteer */
				String path = MergeHelper.getHtmlStringToPdfInByte(content);

				if(path != null) {
					/* convert contentString into byte[] */
					contentBytes = MergeHelper.getUrlContentInBytes("http://localhost:7331" + path);
				}
			} else {
				contentBytes = MergeHelper.getUrlContentInBytes(message.getLink());
			}
			
			if(contentBytes != null) {
				/* Only if context is not null, create new instance for MailAttachment */
				attachment = new MailAttachment();
				attachment.setContentType(message.getContentType());
				attachment.setData(Buffer.buffer(contentBytes));
				
				if(message.getAttachmentType().equals(AttachmentType.INLINE)) {
					attachment.setDisposition("inline");
				} else {
					attachment.setDisposition("attachment");
				}
				
				attachment.setName(message.getNamePrefix());
			} else {
				logger.error("Error happened during byte conversion of attachment content");
			}
			
			
		} else {
			logger.error("Attachment content is null");
		}
	    	
	    
	    return attachment;
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
						
						/* Getting base email template (which contains the header and footer) from "NTF_BASE_TEMPLATE" attribute of project BaseEntity */
						urlString = QwandaUtils.apiGet(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "NTF_BASE_TEMPLATE"), null);	
						
						/* Getting content email template from notifications-doc and merging with contextMap */
						innerContentString = MergeUtil.merge(QwandaUtils.apiGet(emailLink, null), entityTemplateMap);
						
						/* Inserting the content html into the main email html. The mail html template has an element with Id - content */
						doc = Jsoup.parse(urlString);
						Element element = doc.getElementById("content");
						element.html(innerContentString);
						
						baseMessage.setSource(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_USERNAME"));
						logger.info("source email username ::"+MergeUtil.getBaseEntityAttrValueAsString(projectBe, "ENV_EMAIL_USERNAME"));
						
						baseMessage.setSubject(template.getSubject());
						baseMessage.setMsgMessageData(doc.toString());
						
						/* target email is taken from the "PRI_EMAIL" attribute of recipient BaseEntity */
						baseMessage.setTarget(MergeUtil.getBaseEntityAttrValueAsString(recipientBe, "PRI_EMAIL"));
						
						/* mail attachment list stuff */
						baseMessage.setAttachmentList(message.getAttachmentList());
						
						
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

	private List<MailAttachment> setGenericAttachmentsInMailMessage(List<QBaseMSGAttachment> attachmentList, Map<String, Object> contextMap) {
		
		List<MailAttachment> mailAttachments = new ArrayList<>();
		
		/*List<QBaseMSGAttachment> attachmentListList = new ArrayList<>();
		QBaseMSGAttachment attachment1 = new QBaseMSGAttachment(AttachmentType.INLINE, "image/png", "https://i.imgur.com/VuLkMxX.png", false, "image");
		QBaseMSGAttachment attachment2 = new QBaseMSGAttachment(AttachmentType.NON_INLINE, "application/pdf", "https://raw.githubusercontent.com/genny-project/layouts/master/email/templates/invoice-pdf-owner.html", true, "invoice");
		attachmentListList.add(attachment1);
		attachmentListList.add(attachment2);*/
		
		for(QBaseMSGAttachment attachment : attachmentList) {
			MailAttachment mailAttachment = getAttachment(attachment , contextMap);
			
			if(mailAttachment!= null) {
				mailAttachments.add(mailAttachment);
			}
		}	
		
		return mailAttachments;
	}

}
