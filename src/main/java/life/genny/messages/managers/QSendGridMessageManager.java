package life.genny.messages.managers;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

import life.genny.messages.exception.BadRecipientException;
import life.genny.messages.exception.MessageException;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.TimeUtils;
import org.jboss.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class QSendGridMessageManager implements QMessageProvider {

	private static final Logger log = Logger.getLogger(QSendGridMessageManager.class);

	// Concurrency for sendgrid api. Just putting it here to start with
	ExecutorService executor = Executors.newFixedThreadPool(10);

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		// WHY DO WE DO THIS?!?!?!?!?
		BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
		recipientBe = beUtils.getBaseEntityByCode(recipientBe.getCode());

		// Ensure null checks
		if (templateBe == null) {
			log.error(ANSIColour.RED+"TemplateBE passed is NULL!!!!"+ANSIColour.RESET);
			return;
		}

		try {
			executeSendMessage(recipientBe, templateBe, contextMap);
		} catch (MessageException e) {
			log.error("An Error Occurred sending a message");
			e.printStackTrace();
		}
	}

	public void executeSendMessage(BaseEntity recipientBe, BaseEntity templateBe, Map<String, Object> contextMap) throws MessageException {
		log.info("Starting new send message thread");
		
		// Execute all the logic in a separate thread. Will block
		Runnable sendGridRunnable = () -> {
			BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
			String timezone = recipientBe.getValue("PRI_TIMEZONE_ID", "UTC");
			log.info("Timezone returned from recipient BE " + recipientBe.getCode() + " is:: " + timezone);

			// test data
			log.info("Showing what is in recipient BE, code=" + recipientBe.getCode());
			for (EntityAttribute ea : recipientBe.getBaseEntityAttributes()) {
				log.info("attributeCode=" + ea.getAttributeCode() + ", value=" + ea.getObjectAsString());
			}

			// Safely parse the recipient
			String recipient = "";
			try {
				recipient = parseRecipient(recipientBe);
			} catch (BadRecipientException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}

			if (timezone == null || timezone.replaceAll(" ", "").isEmpty()) {
				timezone = "UTC";
			}
			log.info("Recipient BeCode: " + recipientBe.getCode() + " Recipient Email: " + recipient + ", Timezone: " + timezone);


			String templateId = templateBe.getValue("PRI_SENDGRID_ID", null);
			String subject = templateBe.getValue("PRI_SUBJECT", null);

			String sendGridEmailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
			String sendGridEmailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
			String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");
			log.info("The name for email sender "+ sendGridEmailNameSender);

			// Build a general data map from context BEs
			Map<String, Object> templateData = constructTemplateData(contextMap, timezone);

			Email from = new Email(sendGridEmailSender, sendGridEmailNameSender);
			Email to = new Email(recipient);

			String urlBasedAttribute = GennySettings.projectUrl().replace("https://","").replace(".gada.io","").replace("-","_").toUpperCase();
			
			log.info("Searching for email attr " + urlBasedAttribute);
			String dedicatedTestEmail = projectBe.getValue("EML_" + urlBasedAttribute, null);
			if (dedicatedTestEmail != null) {
				log.info("Found email " + dedicatedTestEmail + " for project attribute EML_" + urlBasedAttribute);
				to = new Email(dedicatedTestEmail);
			}

			SendGrid sendGrid = new SendGrid(sendGridApiKey);
			Personalization personalization = new Personalization();
			personalization.addTo(to);
			personalization.setSubject(subject);

			// Handle CC and BCC
			Object ccVal = contextMap.get("CC");
			if (ccVal != null) {
				setupCC(personalization, ccVal, to);
			}

			Object bccVal = contextMap.get("BCC");
			if (bccVal != null) {
				setupBCC(personalization, bccVal, to);
			}

			for (String key : templateData.keySet()) {
				log.debug("key: "+ key);
				log.debug("value: "+ templateData.get(key));
				personalization.addDynamicTemplateData(key, templateData.get(key));
			}

			// Construct and send mail
			Mail mail = constructMail(personalization, from, templateId);
			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");

			Response response;
			try {
				request.setBody(mail.build());
				response = sendGrid.api(request);
				log.info("Response Code: " + response.getStatusCode());
				log.info("Headers: " + response.getHeaders());

				log.info(ANSIColour.GREEN+"SendGrid Message Sent to " + recipient + "!"+ANSIColour.RESET);
			} catch (IOException e) {
				log.error("Failed to send message: " + request.toString());
				e.printStackTrace();
			}
		};

		executor.execute(sendGridRunnable);
	}

	private void setupBCC(Personalization personalization, Object bccVal, Email to) {
		BaseEntity[] bccArray = new BaseEntity[1];

		if (bccVal.getClass().equals(BaseEntity.class)) {
			bccArray[0] = (BaseEntity) bccVal;
		} else {
			bccArray = (BaseEntity[]) bccVal;
		}
		for (BaseEntity item : bccArray) {

			String email = item.getValue("PRI_EMAIL", null);
			if (email != null) {
				email = email.trim();
			}

			if (email != null && !email.equals(to.getEmail())) {
				personalization.addBcc(new Email(email));
				log.info(ANSIColour.BLUE+"Found BCC Email: " + email+ANSIColour.RESET);
			}
		}
	}

	private void setupCC(Personalization personalization, Object ccVal, Email to) {
		BaseEntity[] ccArray = new BaseEntity[1];

		if (ccVal.getClass().equals(BaseEntity.class)) {
			ccArray[0] = (BaseEntity) ccVal;
		} else {
			ccArray = (BaseEntity[]) ccVal;
		}
		for (BaseEntity item : ccArray) {

			String email = item.getValue("PRI_EMAIL", null);
			if (email != null) {
				email = email.trim();
			}

			if (email != null && !email.equals(to.getEmail())) {
				personalization.addCc(new Email(email));
				log.info(ANSIColour.BLUE+"Found CC Email: " + email+ANSIColour.RESET);
			}
		}
	}

	private Mail constructMail(Personalization personalization, Email from, String templateId) {
		Mail mail = new Mail();
		mail.addPersonalization(personalization);
		mail.setTemplateId(templateId);
		mail.setFrom(from);

		return mail;
	}

	private String parseRecipient(BaseEntity recipientBe) throws BadRecipientException {
		if (recipientBe == null) {
			log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
			throw new BadRecipientException(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
		}


		String recipient = recipientBe.getValue("PRI_EMAIL", null);
		if (recipient == null) {
			log.error(ANSIColour.RED+"Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
			throw new BadRecipientException(recipientBe);
		}
		
		recipient = recipient.trim();
		return recipient;
	}

	private Map<String, Object> constructTemplateData(Map<String, Object> contextMap, String timezone) {
		HashMap<String, Object> templateData = new HashMap<>();

		for (String key : contextMap.keySet()) {

			Object value = contextMap.get(key);

			if (value.getClass().equals(BaseEntity.class)) {
				log.info("Processing key as BASEENTITY: " + key);
				BaseEntity be = (BaseEntity) value;
				HashMap<String, String> deepReplacementMap = new HashMap<>();
				for (EntityAttribute ea : be.getBaseEntityAttributes()) {

					String attrCode = ea.getAttributeCode();
					if (attrCode.startsWith("LNK") || attrCode.startsWith("PRI")) {
						Object attrVal = ea.getValue();
						if (attrVal != null) {

							String valueString = attrVal.toString();

							if (attrVal.getClass().equals(LocalDate.class)) {
								if (contextMap.containsKey("DATEFORMAT")) {
									String format = (String) contextMap.get("DATEFORMAT");
									valueString = TimeUtils.formatDate((LocalDate) attrVal, format);
								} else {
									log.info("No DATEFORMAT key present in context map, defaulting to stringified date");
								}
							} else if (attrVal.getClass().equals(LocalDateTime.class)) {
								if (contextMap.containsKey("DATETIMEFORMAT")) {

									String format = (String) contextMap.get("DATETIMEFORMAT");
									LocalDateTime dtt = (LocalDateTime) attrVal;

									ZonedDateTime zonedDateTime = dtt.atZone(ZoneId.of("UTC"));
									ZonedDateTime converted = zonedDateTime.withZoneSameInstant(ZoneId.of(timezone));

									valueString = TimeUtils.formatZonedDateTime(converted, format);
									log.info("date format");
									log.info("formatted date: "+  valueString);
								} else {
									log.info("No DATETIMEFORMAT key present in context map, defaulting to stringified dateTime");
								}
							}
							// templateData.put(key+"."+attrCode, valueString);
							deepReplacementMap.put(attrCode, valueString);
						}
					}
				}
				templateData.put(key, deepReplacementMap);
			} else if(value.getClass().equals(String.class)) {
				log.info("Processing key as STRING: " + key);
				templateData.put(key, (String) value);
			}
		}

		return templateData;
	}

}
