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
import life.genny.qwandautils.StringFormattingUtils;
import life.genny.util.MergeHelper;

public class QVertxMailManager implements QMessageProvider{
	
	private Vertx vertx;
	
	public static final String ANSI_RESET = "\u001B[0m"; 
    public static final String ANSI_GREEN = "\u001B[32m";
			
    public final static String PDF_GEN_SERVICE_API_URL = System.getenv("PDF_GEN_SERVICE_API_URL") == null ? "http://localhost:7331/raw"
			: System.getenv("PDF_GEN_SERVICE_API_URL");
	
	public static final Boolean devMode = System.getenv("GENNYDEV") == null ? false : true;
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	final public static Boolean IS_STAGING = System.getenv("GENNYSTAGING") == null ? false : true;

	@Override
	public void sendMessage(QBaseMSGMessage message, EventBus eventBus, Map<String, Object> contextMap) {

		vertx = Vertx.vertx();

		if ( message.getTarget() != null ) {
			
			/* get the project Baseentity, null check is already done in processHelper */
			BaseEntity projectBe = (BaseEntity)contextMap.get("PROJECT");
			
			MailMessage mailmessage = mailMessage(message, projectBe);
			

			// If message has attachments, process them seperately
			if (message.getAttachmentList() != null && message.getAttachmentList().size() > 0) {
				List<MailAttachment> attachmentList = setGenericAttachmentsInMailMessage(message.getAttachmentList(),
						contextMap);
				mailmessage.setAttachment(attachmentList);
			}

			/* create vertx instance of MailClient */
			MailClient mailClient = createClient(vertx, projectBe);
			
			/* Trigger email */
			mailClient.sendMail(mailmessage, result -> {
				if (result.succeeded()) {
					logger.info("email sent to ::" + mailmessage.getTo());
				} else {
					result.cause().printStackTrace();
				}
			});
		}
		// END-OF manual hack
		
	}	

	  public MailClient createClient(Vertx vertx, BaseEntity projectBe) {
	    MailConfig config = new MailConfig();
	    
	    config.setHostname(projectBe.getValue("ENV_MAIL_SMTP_HOST", null));
	    config.setPort(Integer.parseInt(projectBe.getValue("ENV_MAIL_SMTP_PORT", null)));
	    config.setStarttls(StartTLSOptions.REQUIRED);
	    config.setUsername(projectBe.getValue("ENV_EMAIL_USERNAME", null));
	    config.setPassword(projectBe.getValue("ENV_EMAIL_PASSWORD", null));
	
	    return MailClient.createNonShared(vertx, config);
	    
	  }

	  public MailMessage mailMessage( QBaseMSGMessage messageTemplate, BaseEntity projectBe) {
	    MailMessage message = new MailMessage();
	    message.setFrom(messageTemplate.getSource());
	    
	    if(devMode || IS_STAGING) {
	    	
		    	/* In dev/staging mode, send only to devs */
	    		List<String> devs = new ArrayList<>();
	    		devs.add("adam@gada.io");
	    		devs.add("gayatri@gada.io");
	    		devs.add("anish@gada.io");
	    		devs.add("safal0079@gmail.com");
	    		
	    		String testEmailIds = projectBe.getValue("PRI_TEST_EMAIL_IDS", null);
	    		logger.info("testEmailIds ::"+testEmailIds);
		    /* add bcc list only if environment is not dev */
			if (testEmailIds != null) {
				List<String> listOfTestRecipients = StringFormattingUtils.splitCharacterSeperatedStringToList(testEmailIds, ",");
				if (listOfTestRecipients != null) {
					devs.addAll(listOfTestRecipients);
					logger.info("listOfTestRecipients :"+listOfTestRecipients.toString());
				}
			}	    		
	    		message.setTo(devs);
	    } else {
		    	/* if in production mode, send email to original recipients */
	    		message.setTo(messageTemplate.getTarget());
	    }
	    
	    if(devMode || IS_STAGING) {
	    		message.setSubject("TEST:"+messageTemplate.getSubject());
	    } else {
	    		message.setSubject(messageTemplate.getSubject());
	    }
	    
	    message.setHtml(messageTemplate.getMsgMessageData());
	    
	    String bccString = projectBe.getValue("PRI_EMAIL_BCC_LIST", null);
	    /* add bcc list only if environment is not dev */
		if (!(devMode || IS_STAGING) && bccString != null) {
			List<String> listOfBccRecipients = StringFormattingUtils.splitCharacterSeperatedStringToList(bccString, ",");
			if (listOfBccRecipients != null) {
				message.setBcc(listOfBccRecipients);
				logger.info("listOfBccRecipients :"+listOfBccRecipients.toString());
			}
		}
    	    
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
			
			/* IF CONTENT TYPE IS PDF and URL is a HTML template :: Then Hit Camelot service to fetch pdf filepath. Camelot service converts html strings into PDF format using puppetteer */
			if(message.getContentType().equalsIgnoreCase("application/pdf")  && message.getLink().contains(".html")) {
				
				String path = MergeHelper.getHtmlStringToPdfInByte(content);

				if(path != null) {
					/* convert contentString into byte[] */
					contentBytes = MergeHelper.getUrlContentInBytes(PDF_GEN_SERVICE_API_URL + path);
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
		logger.info("Recepients::" + recipientBe);
		
		if(recipientBe != null) {
			if (template != null) {
					
				String emailLink = template.getEmail_templateId();
				logger.info("emailLink ::" + emailLink);
				String urlString = null;
				String innerContentString = null;
				String mainContentString = null;
				Document doc = null;
				try {
					
					BaseEntity projectBe = (BaseEntity)entityTemplateMap.get("PROJECT");
					logger.info("projectBe ::" + projectBe);
					
					if(projectBe != null) {
						
						/* Getting base email template (which contains the header and footer) from "NTF_BASE_TEMPLATE" attribute of project BaseEntity */
						urlString = projectBe.findEntityAttribute("NTF_BASE_TEMPLATE").isPresent()?projectBe.findEntityAttribute("NTF_BASE_TEMPLATE").get().getAsString():null; //QwandaUtils.apiGet(MergeUtil.getBaseEntityAttrValueAsString(projectBe, "NTF_BASE_TEMPLATE"), null);	
						
						/* Getting content email template from notifications-doc and merging with contextMap */
						innerContentString = MergeUtil.merge(QwandaUtils.apiGet(emailLink, null), entityTemplateMap);
						
						/* Getting content email template from notifications-doc and merging with contextMap */
						mainContentString = QwandaUtils.apiGet(urlString, null);
						
						/* Inserting the content html into the main email html. The mail html template has an element with Id - content */
						doc = Jsoup.parse(mainContentString);
					
						Element element = doc.getElementById("content");
						
						element.html(innerContentString);
						logger.info("doc after merge :: " + doc);
						
						/* Amazon mail accounts have an extra config of sourceEmail..amazon mail service do not have sameID username and email. Google account has the same ID for username and sourceEmail */
						String emailSourceEmail = projectBe.getValue("ENV_MAIL_SMTP_SOURCE_EMAIL", null);
						logger.info("elememailSourceEmailent" + emailSourceEmail);
						
						/* setting up all source, target, priority, subject, content, attachment list in the constructor */
						if(emailSourceEmail != null) {
							logger.info("this email account has sourceEmail, so setting it as source ::" +emailSourceEmail);
							baseMessage = new QBaseMSGMessage(emailSourceEmail, recipientBe.getValue("PRI_EMAIL", null), null, MergeUtil.merge(template.getSubject(), entityTemplateMap), doc.toString(), message.getAttachmentList());
						} else {
							logger.info("this email account does not sourceEmail, so setting username as source");
							baseMessage = new QBaseMSGMessage(projectBe.getValue("ENV_EMAIL_USERNAME", null), recipientBe.getValue("PRI_EMAIL", null), null, template.getSubject(), doc.toString(), message.getAttachmentList());
						}
								
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
		
		for(QBaseMSGAttachment attachment : attachmentList) {
			MailAttachment mailAttachment = getAttachment(attachment , contextMap);
			
			if(mailAttachment!= null) {
				mailAttachments.add(mailAttachment);
			}
		}	
		
		return mailAttachments;
	}

	@Override
	public QBaseMSGMessage setGenericMessageValueForDirectRecipient(QMessageGennyMSG message,
			Map<String, Object> entityTemplateMap, String token, String to) {
		QBaseMSGMessage baseMessage = null;
		QBaseMSGMessageTemplate template = MergeHelper.getTemplate(message.getTemplate_code(), token);
		
		if (template != null) {
				
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
					
					/* Amazon mail accounts have an extra config of sourceEmail..amazon mail service do not have sameID username and email. Google account has the same ID for username and sourceEmail */
					String emailSourceEmail = projectBe.getValue("ENV_MAIL_SMTP_SOURCE_EMAIL", null);
					
					/* setting up all source, target, priority, subject, content, attachment list in the constructor */
					if(emailSourceEmail != null) {
						logger.info("this email account has sourceEmail, so setting it as source ::" +emailSourceEmail);
						baseMessage = new QBaseMSGMessage(emailSourceEmail, to, null, MergeUtil.merge(template.getSubject(), entityTemplateMap), doc.toString(), message.getAttachmentList());
					} else {
						logger.info("this email account does not sourceEmail, so setting username as source");
						baseMessage = new QBaseMSGMessage(projectBe.getValue("ENV_EMAIL_USERNAME", null), to, null, template.getSubject(), doc.toString(), message.getAttachmentList());
					}
							
				} else {
					logger.error("NO PROJECT BASEENTITY FOUND");
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
										
		} else {
			logger.error("NO TEMPLATE FOUND");
		}
		
		return baseMessage;
	}

}
