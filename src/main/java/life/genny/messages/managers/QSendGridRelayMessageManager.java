package life.genny.messages.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import io.smallrye.openapi.api.util.MergeUtil;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.MergeUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QSendGridRelayMessageManager implements QMessageProvider {

	private static final Logger log = Logger.getLogger(QSendGridRelayMessageManager.class);


	private static final ExecutorService executorService = Executors.newFixedThreadPool(20);

	private static HttpClient httpClient = HttpClient.newBuilder().executor(executorService)
			.version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(20)).build();

	private HttpRequest.Builder createJsonRequest(String path, Optional<Map<String, String>> additionalHeaders, String body) {
		return createRequest(path, additionalHeaders)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
	}

	private HttpRequest.Builder createRequest(String path, Optional<Map<String, String>> additionalHeaders) {
		HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(path));
		builder.timeout(Duration.ofMinutes(1L));
		if (additionalHeaders.isPresent()) {
			additionalHeaders.get().forEach((k, v) -> builder.header(k, v));
		}
		return builder;
	}

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		log.info("SendGrid email type");

		BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
		BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
		
		recipientBe = beUtils.getBaseEntityByCode(recipientBe.getCode());

		if (templateBe == null) {
			log.error(ANSIColour.RED+"TemplateBE passed is NULL!!!!"+ANSIColour.RESET);
			return;
		}

		if (recipientBe == null) {
			log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
		}

		String timezone = recipientBe.getValue("PRI_TIMEZONE_ID", "UTC");

		log.info("Timezone returned from recipient BE " + recipientBe.getCode() + " is:: " + timezone);

		// test data
		log.info("Showing what is in recipient BE, code=" + recipientBe.getCode());
		for (EntityAttribute ea : recipientBe.getBaseEntityAttributes()) {
			log.info("attributeCode=" + ea.getAttributeCode() + ", value=" + ea.getObjectAsString());
		}

		String recipient = recipientBe.getValue("PRI_EMAIL", null);

		if (recipient != null) {
			recipient = recipient.trim();
		}
		if (timezone == null || timezone.replaceAll(" ", "").isEmpty()) {
			timezone = "UTC";
		}
		log.info("Recipient BeCode: " + recipientBe.getCode() + " Recipient Email: " + recipient + ", Timezone: " + timezone);

		if (recipient == null) {
			log.error(ANSIColour.RED+"Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
			return;
		}

		String subject = templateBe.getValue("PRI_SUBJECT", null);
		String body = templateBe.getValue("PRI_BODY", null);
		String sendGridEmailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
		String sendGridEmailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
		String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");
		log.info("The name for email sender "+ sendGridEmailNameSender);

		// Build a general data map from context BEs
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
									valueString = MergeUtils.getFormattedDateString((LocalDate) attrVal, format);
								} else {
									log.info("No DATEFORMAT key present in context map, defaulting to stringified date");
								}
							} else if (attrVal.getClass().equals(LocalDateTime.class)) {
								if (contextMap.containsKey("DATETIMEFORMAT")) {

									String format = (String) contextMap.get("DATETIMEFORMAT");
									LocalDateTime dtt = (LocalDateTime) attrVal;

									ZonedDateTime zonedDateTime = dtt.atZone(ZoneId.of("UTC"));
									ZonedDateTime converted = zonedDateTime.withZoneSameInstant(ZoneId.of(timezone));

									valueString = MergeUtils.getFormattedZonedDateTimeString(converted, format);
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

		Email from = new Email(sendGridEmailSender, sendGridEmailNameSender);
		Email to = new Email(recipient);

		String urlBasedAttribute = GennySettings.projectUrl().replace("https://","").replace(".gada.io","").replace("-","_").toUpperCase();
		log.info("Searching for email attr " + urlBasedAttribute);
		String dedicatedTestEmail = projectBe.getValue("EML_" + urlBasedAttribute, null);
		if (dedicatedTestEmail != null) {
			log.info("Found email " + dedicatedTestEmail + " for project attribute EML_" + urlBasedAttribute);
			to = new Email(dedicatedTestEmail);
		}

		SendGrid sg = new SendGrid(sendGridApiKey);
		Personalization personalization = new Personalization();
		personalization.addTo(to);
		personalization.setSubject(subject);

		// Hande CC and BCC
		Object ccVal = contextMap.get("CC");
		Object bccVal = contextMap.get("BCC");

		if (ccVal != null) {
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

		if (bccVal != null) {
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

		for (String key : templateData.keySet()) {
			personalization.addDynamicTemplateData(key, templateData.get(key));
		}

		Mail mail = new Mail();
		mail.addPersonalization(personalization);
		Content content = new Content();
		content.setType("text/html");
		String merged = MergeUtils.merge(body, contextMap);
		content.setValue(merged);
		mail.setFrom(from);

		sendRequest(mail,sendGridApiKey);
	}

	private void sendRequest(Mail mail, String apiKey) {
		String path = "https://api.sendgrid.com/v3/mail/send";
		try {
			String requestBody = new ObjectMapper().writeValueAsString(mail);
			System.out.println("####### requestBody: "+requestBody);

			Optional<Map<String, String>> additionalHeaders = Optional.of(Map.of("Authorization", "Bearer " + apiKey));

			HttpRequest.Builder httpRequestBuilder = createJsonRequest(path, additionalHeaders, requestBody);
			HttpRequest httpRequest = httpRequestBuilder.build();

			HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			int statusCode = httpResponse.statusCode();
			System.out.println("####### statusCode: "+statusCode);
		} catch (Exception ex) {
			System.out.println("Exception: " + ex.getMessage());
		}
	}
}
