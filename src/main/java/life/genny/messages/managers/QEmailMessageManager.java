package life.genny.messages.managers;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.utils.HttpUtils;
import org.jboss.logging.Logger;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;

import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.models.ANSIColour;

public class QEmailMessageManager implements QMessageProvider {


	public static final String FILE_TYPE = "application/";

	public static final String MESSAGE_BOTH_DRIVER_OWNER = "BOTH";

	private static final Logger log = Logger.getLogger(QEmailMessageManager.class);

	@Inject
	Mailer mailer;

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		log.info("Sending an Email Type Message...");

		BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");
		BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");

		if(projectBe != null) {
			log.error(ANSIColour.GREEN+"projectBe is -> " + projectBe.getCode());
		} else {
			log.error(ANSIColour.RED+"ProjectBe is NULL"+ANSIColour.RESET);
			return;
		}

		if (recipientBe == null) {
			log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
			return;
		}

		String targetEmail = recipientBe.getValue("PRI_EMAIL", null);

		if (targetEmail == null) {
			log.error(ANSIColour.RED+"Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
			return;
		}

		String body = templateBe.getValue("PRI_BODY", null);
		String subject = templateBe.getValue("PRI_SUBJECT", null);
		String sender = projectBe.getValue("ENV_EMAIL_USERNAME", null);

		if (body == null) {
			log.error(ANSIColour.RED+"Template BE " + templateBe.getCode() + ", PRI_BODY is NULL"+ANSIColour.RESET);
			return;
		}
		if (subject == null) {
			log.error(ANSIColour.RED+"Template BE " + templateBe.getCode() + ", PRI_SUBJECT is NULL"+ANSIColour.RESET);
			return;
		}
		if (sender == null) {
			log.error(ANSIColour.RED+"Project BE " + templateBe.getCode() + ", ENV_EMAIL_USERNAME is NULL"+ANSIColour.RESET);
			return;
		}

		String timezone = recipientBe.getValue("PRI_TIMEZONE_ID", "UTC");

		log.info("Timezone returned from recipient BE " + recipientBe.getCode() + " is:: " + timezone);

		// Mail Merging Data
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



		body = MergeUtils.merge(body, contextMap);
//		Integer randStr = (int) Math.random();

		System.out.println("contextMap values are -> " + contextMap);
		System.out.println("MergeUtils Body value is -> " + body);
		System.out.println("templateData values are -> " + templateData);

		String bodyContainer = "{\"personalizations\":[{\"to\":[{\"email\":\"" + targetEmail +"\",\"name\":\"Rahul Sam\"}],\"subject\":\"Hello, World!\"}],\"content\": [{\"type\": \"text/plain\", \"value\": \"Body--> "+ body + "!\"}],\"from\":{\"email\":\"rahul.samaranayake@outcomelife.com.au\",\"name\":\"Rahul samaranayake\"},\"reply_to\":{\"email\":\"rahul.samaranayake@outcomelife.com.au\",\"name\":\"Rahul samaranayake\"}}";
		String ccEmail = "mrrahulmaxcontact@gmail.com";
		String ccBodyContainer = "{\"personalizations\":[{\"to\":[{\"email\":\"" + ccEmail +"\",\"name\":\"Rahul Sam\"}],\"subject\":\"Hello, World!\"}],\"content\": [{\"type\": \"text/plain\", \"value\": \"CC Body--> "+ body + "!\"}],\"from\":{\"email\":\"rahul.samaranayake@outcomelife.com.au\",\"name\":\"Rahul samaranayake\"},\"reply_to\":{\"email\":\"rahul.samaranayake@outcomelife.com.au\",\"name\":\"Rahul samaranayake\"}}";

		try {

//			mailer.send(Mail.withText(targetEmail, subject, body));

			String sendGridApiKey = projectBe.getValueAsString("ENV_MUQ_SENDGRID_API_KEY");

			HttpResponse<String> post = HttpUtils.post("https://api.sendgrid.com/v3/mail/send", bodyContainer, sendGridApiKey);

			log.info(ANSIColour.GREEN + "Email to " + targetEmail +" is sent" + ANSIColour.RESET);
			log.info(ANSIColour.GREEN + "targetEmail Post response -> " + post);

			HttpResponse<String> ccPost = HttpUtils.post("https://api.sendgrid.com/v3/mail/send", ccBodyContainer, sendGridApiKey);
			log.info(ANSIColour.GREEN + "CC targetEmail Post response -> " + ccPost);

		} catch (Exception e) {
			log.error("ERROR -> ", e);
		}

	}
}