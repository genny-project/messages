package life.genny.messages.managers;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.quarkus.runtime.annotations.RegisterForReflection;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Sendgrid POJO classes where registered for reflection as native compilation will not serialize it without this.
@RegisterForReflection(targets = {
		com.sendgrid.helpers.mail.objects.ASM.class,
		com.sendgrid.helpers.mail.objects.Attachments.class,
		com.sendgrid.helpers.mail.objects.Attachments.Builder.class,
		com.sendgrid.helpers.mail.objects.BccSettings.class,
		com.sendgrid.helpers.mail.objects.ClickTrackingSetting.class,
		com.sendgrid.helpers.mail.objects.Content.class,
		com.sendgrid.helpers.mail.objects.Email.class,
		com.sendgrid.helpers.mail.objects.FooterSetting.class,
		com.sendgrid.helpers.mail.objects.GoogleAnalyticsSetting.class,
		com.sendgrid.helpers.mail.objects.MailSettings.class,
		com.sendgrid.helpers.mail.objects.OpenTrackingSetting.class,
		com.sendgrid.helpers.mail.objects.Personalization.class,
		com.sendgrid.helpers.mail.objects.Setting.class,
		com.sendgrid.helpers.mail.objects.SpamCheckSetting.class,
		com.sendgrid.helpers.mail.objects.SubscriptionTrackingSetting.class,
		com.sendgrid.helpers.mail.objects.TrackingSettings.class,
		com.sendgrid.helpers.mail.Mail.class,
})
public class QSendGridMessageManager implements QMessageProvider {

	// Concurrency for sendgrid api. Just putting it here to start with
	ExecutorService executor = Executors.newFixedThreadPool(10);

	private static final Logger log = Logger.getLogger(QSendGridMessageManager.class);

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {
		log.info("SendGrid email type");

		Runnable sendGridRunnable = () -> {

			log.info("Starting thread!");

			BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
			BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");

			recipientBe = beUtils.getBaseEntityByCode(recipientBe.getCode());

			if (templateBe == null) {
				log.error(ANSIColour.RED + "templateBe is NULL!" + ANSIColour.RESET);
				return;
			}

			if (recipientBe == null) {
				log.error(ANSIColour.RED + "recipientBe is NULL!" + ANSIColour.RESET);
				return;
			}

			// test data
			log.info("Showing what is in recipient BE, code=" + recipientBe.getCode());
			for (EntityAttribute ea : recipientBe.getBaseEntityAttributes()) {
				log.info("attributeCode=" + ea.getAttributeCode() + ", value=" + ea.getObjectAsString());
			}

			// send email to secondary email if it is present.
			String recipientEmail = findSendableEmail(recipientBe);
			if (recipientEmail == null) {
				log.error(ANSIColour.RED + "recipientEmail is NULL!" + ANSIColour.RESET);
				return;
			}
			log.info("Sending Email to: " + recipientEmail);

			String timezone = recipientBe.getValue("PRI_TIMEZONE_ID", null);
			/*Some BE using old timezone attr value*/
			if (StringUtils.isEmpty(timezone)) {
				timezone = recipientBe.getValue("PRI_TIME_ZONE", "UTC");
			}
			log.info("Recipient BeCode: " + recipientBe.getCode() + " Recipient Email: " + recipientEmail + ", Timezone: " + timezone);

			String templateId = templateBe.getValue("PRI_SENDGRID_ID", null);
			String subject = templateBe.getValue("PRI_SUBJECT", null);

			String sendGridEmailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
			String sendGridEmailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
			String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");
			log.info("The name for email sender "+ sendGridEmailNameSender);

			Email from = new Email(sendGridEmailSender, sendGridEmailNameSender);
			Email to = new Email(recipientEmail);

			String urlBasedAttribute = GennySettings.projectUrl().replace("https://","").replace(".gada.io","").replace("-","_").toUpperCase();
			log.info("Searching for email attr " + urlBasedAttribute);
			String dedicatedTestEmail = projectBe.getValue("EML_" + urlBasedAttribute, null);
			if (dedicatedTestEmail != null) {
				log.info("Found email " + dedicatedTestEmail + " for project attribute EML_" + urlBasedAttribute);
				to = new Email(dedicatedTestEmail);
			}

			Personalization personalization = new Personalization();
			personalization.addTo(to);
			personalization.setSubject(subject);

			/*Handle CC*/
			List<String> ccEmails = getCarbonCopyEmails(contextMap, to, "CC");
			if (ccEmails.isEmpty()) {
				log.info(ANSIColour.BLUE + "Did not find CC Email" + ANSIColour.RESET);
			} else {
				for (String ccEmail : ccEmails) {
					personalization.addCc(new Email(ccEmail));
					log.info(ANSIColour.BLUE + "Found CC Email: " + ccEmail + ANSIColour.RESET);
				}
			}

			/*Handle BCC*/
			List<String> bccEmails = getCarbonCopyEmails(contextMap, to, "BCC");
			if (bccEmails.isEmpty()) {
				log.info(ANSIColour.BLUE + "Did not find BCC Email" + ANSIColour.RESET);
			} else {
				for (String bccEmail : bccEmails) {
					personalization.addBcc(new Email(bccEmail));
					log.info(ANSIColour.BLUE + "Found BCC Email: " + bccEmail + ANSIColour.RESET);
				}
			}

			// Build a general data map from context BEs and put in personalization
			setDynamicTemplateData(contextMap, personalization, timezone);

			Mail mail = new Mail();
			mail.addPersonalization(personalization);
			mail.setTemplateId(templateId);
			mail.setFrom(from);

			Request request = new Request();
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			try {
				request.setBody(mail.build());
			} catch (IOException e) {
				log.error("Failed to send message: " + request.toString());
				e.printStackTrace();
			}
			SendGrid sg = new SendGrid(sendGridApiKey);
			Response response = sendAndHandleRequest(sg, request, recipientEmail);

		};
		executor.execute(sendGridRunnable);
	}


	/**
	 * Send a SendGrid request (will log response if there are errors)
	 * @param sg - {@link SendGrid} instance
	 * @param request - a {@link Request} to send
	 * @param recipientEmail - email to send to (for logging purposes)
	 * @return - {@link Response} returned from SendGrid
	 */
	private Response sendAndHandleRequest(SendGrid sg, Request request, String recipientEmail) {

		Response response;
		try {
			response = sg.api(request);

			int statusCode = response.getStatusCode();
			log.info("SendGrid status code: "+ statusCode);
			int statusFamily = (int)Math.floor(statusCode / 100);
			if (statusFamily != 2) {// Not ok
				log.error(ANSIColour.RED+"Error sending SendGrid message to " + recipientEmail + "!"+ANSIColour.RESET);
				logResponse(response, log::error);
			} else {
				log.info(ANSIColour.GREEN+"SendGrid message sent to " + recipientEmail + "!"+ANSIColour.RESET);
			}
		} catch (IOException e) {
			log.error("Error sending request to SendGrid!: " + request);
			logRequest(request, log::error);
			log.error("\n\nException: ");
			e.printStackTrace();
			return null;
		}

		return response;
	}

	/**
	 * Log a SendGrid response
	 * @param response
	 * @param log - log level
	 */
	private void logResponse(Response response, LogCallback log) {
		
		log.log("Response Code: " + response.getStatusCode());
		log.log("Headers: " + response.getHeaders());
		logHeaders(response.getHeaders(), log);
	}

	/**
	 * Log a SendGrid request
	 * @param request
	 * @param log - log level
	 */
	private void logRequest(Request request, LogCallback log) {
		log.log("URI: " + request.getBaseUri() + "/" + request.getEndpoint());
		log.log("Body: " + request.getBody());
		logHeaders(request.getHeaders(), log);
	}

	/**
	 * Log a set of headers
	 * @param headers
	 * @param log - log level
	 */
	private void logHeaders(Map<String, String> headers, LogCallback log) {
		log.log("========Headers============");
		for(String header : headers.keySet()) {
			log.log("		" + header + " = " + headers.get(header));
		}
	}

	private String findSendableEmail(BaseEntity recipient) {
		// fetch additional email
		String additionalEmail = recipient.getValue("PRI_EMAIL_ADDITIONAL", null);
		/*isNotEmpty() does null checks as well*/
		if (StringUtils.isNotEmpty(additionalEmail)) {
			return additionalEmail.trim();
		}

		// fetch primary email
		String primaryEmail = recipient.getValue("PRI_EMAIL", null);
		if (StringUtils.isNotEmpty(primaryEmail)) {
			return primaryEmail.trim();
		}

		log.error(ANSIColour.RED+"Target " + recipient.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
		return null;
	}

	private void setDynamicTemplateData(Map<String, Object> contextMap, Personalization personalization, String timezone) {

		for (String key : contextMap.keySet()) {
			Object value = contextMap.get(key);

			log.info("contextMap key: "+ key);
			log.info("contextMap value: "+ value);

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
								log.info("LocalDate valueString: " + valueString);

								if (contextMap.containsKey("DATEFORMAT")) {
									String format = (String) contextMap.get("DATEFORMAT");
									valueString = TimeUtils.formatDate((LocalDate) attrVal, format);

									log.info("formatted date: " +  valueString);
								} else {
									log.info("No DATEFORMAT key present in context map, defaulting to stringified date");
								}
							} else if (attrVal.getClass().equals(LocalDateTime.class)) {
								log.info("LocalDateTime valueString: " + valueString);

								if (contextMap.containsKey("DATETIMEFORMAT")) {
									String format = (String) contextMap.get("DATETIMEFORMAT");
									LocalDateTime dtt = (LocalDateTime) attrVal;

									ZonedDateTime zonedDateTime = dtt.atZone(ZoneId.of("UTC"));
									ZonedDateTime converted = zonedDateTime.withZoneSameInstant(ZoneId.of(timezone));

									valueString = TimeUtils.formatZonedDateTime(converted, format);
									valueString += " (" + timezone +")";	// show converted timezone in email

									log.info("formatted datetime with timezone: " +  valueString);
								} else {
									log.info("No DATETIMEFORMAT key present in context map, defaulting to stringified dateTime");
								}
							}
							/*If Date or DateTime, convert to provided format, else put String value in map*/
							deepReplacementMap.put(attrCode, valueString);
						}
					}
				}
				personalization.addDynamicTemplateData(key, deepReplacementMap);
			} else if (value.getClass().equals(String.class)) {
				log.info("Processing key as STRING: " + key);
				personalization.addDynamicTemplateData(key, value);
			}
		}
	}

	private List<String> getCarbonCopyEmails(Map<String, Object> contextMap, Email to, String toGet){
		List<String> emails = new ArrayList<>();
		Object value = contextMap.get(toGet);

		if (value != null) {
			BaseEntity[] ccArray = new BaseEntity[1];

			if (value.getClass().equals(BaseEntity.class)) {
				ccArray[0] = (BaseEntity) value;
			} else {
				ccArray = (BaseEntity[]) value;
			}

			for (BaseEntity item : ccArray) {
				String email = item.getValue("PRI_EMAIL", null);
				if (email != null) {
					email = email.trim();
				}

				if (email != null && !email.equals(to.getEmail())) {
					emails.add(email);
				}
			}
		}
		return emails;
	}

	private interface LogCallback {
		public void log(Object obj);
	}
}
