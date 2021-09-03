package life.genny.message;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.EEntityStatus;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.ANSIColour;
import life.genny.util.MergeHelper;
import life.genny.notifications.EmailHelper;
import life.genny.utils.BaseEntityUtils;
import life.genny.qwandautils.GennySettings;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;

public class QSendGridMessageManager implements QMessageProvider {
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

		logger.info("SendGrid email type");

		BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
		BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");

		if (recipientBe == null) {
			logger.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
		}

		String recipient = recipientBe.getValue("PRI_EMAIL", null);

		if (recipient == null) {
			logger.error(ANSIColour.RED+"Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
			return;
		}

		List<String> ccList = null;
		List<String> bccList = null;

		if (contextMap.containsKey("CC")) {
			String ccArr = (String) contextMap.get("CC");
			List<BaseEntity> ccEntities = MergeHelper.convertCodesToBaseEntityArray(beUtils, ccArr);
			ccList = ccEntities.stream().map(item -> item.getValue("PRI_EMAIL", ""))
					.filter(item -> !item.isEmpty()).collect(Collectors.toList());
		}
		if (contextMap.containsKey("CC")) {
			String bccArr = (String) contextMap.get("BCC");
			List<BaseEntity> bccEntities = MergeHelper.convertCodesToBaseEntityArray(beUtils, bccArr);
			bccList = bccEntities.stream().map(item -> item.getValue("PRI_EMAIL", ""))
					.filter(item -> !item.isEmpty()).collect(Collectors.toList());
		}

		String templateId = templateBe.getValue("PRI_SENDGRID_ID", null);
		String subject = templateBe.getValue("PRI_SUBJECT", null);

		String sendGridEmailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
		String sendGridEmailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
		String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");
		logger.info("The name for email sender "+ sendGridEmailNameSender);

		// Build a general data map from context BEs
		HashMap<String, Object> templateData = new HashMap<>();

		for (String key : contextMap.keySet()) {

			Object value = contextMap.get(key);

			if (value.getClass().equals(BaseEntity.class)) {
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
									valueString = MergeUtil.getFormattedDateString((LocalDate) attrVal, format);
								} else {
									logger.info("No DATEFORMAT key present in context map, defaulting to stringified date");
								}
							} else if (attrVal.getClass().equals(LocalDateTime.class)) {
								if (contextMap.containsKey("DATETIMEFORMAT")) {
									String format = (String) contextMap.get("DATETIMEFORMAT");
									valueString = MergeUtil.getFormattedDateTimeString((LocalDateTime) attrVal, format);
								} else {
									logger.info("No DATETIMEFORMAT key present in context map, defaulting to stringified dateTime");
								}
							}
							// templateData.put(key+"."+attrCode, valueString);
							deepReplacementMap.put(attrCode, valueString);
						}
					}
				}
				templateData.put(key, deepReplacementMap);
			} else if(value.getClass().equals(String.class)) {
				templateData.put(key, (String) value);
			}
		}

		Email from = new Email(sendGridEmailSender, sendGridEmailNameSender);
		Email to = new Email(recipient);

		String urlBasedAttribute = GennySettings.projectUrl.replace("https://","").replace(".gada.io","").replace("-","_").toUpperCase();
		logger.info("Searching for email attr " + urlBasedAttribute);
		String dedicatedTestEmail = projectBe.getValue("EML_" + urlBasedAttribute, null);
		if (dedicatedTestEmail != null) {
			logger.info("Found email " + dedicatedTestEmail + " for project attribute EML_" + urlBasedAttribute);
			to = new Email(dedicatedTestEmail);
		}

		SendGrid sg = new SendGrid(sendGridApiKey);
		Personalization personalization = new Personalization();
		personalization.addTo(to);
		personalization.setSubject(subject);

		if (ccList != null) {
			for (String email : ccList) {
				if (!email.equals(to.getEmail())) {
					personalization.addCc(new Email(email));
				}
			}
		}
		if (bccList != null) {
			for (String email : bccList) {
				if (!email.equals(to.getEmail())) {
					personalization.addBcc(new Email(email));
				}
			}
		}

		for (String key : templateData.keySet()) {
			personalization.addDynamicTemplateData(key, templateData.get(key));
		}

		Mail mail = new Mail();
		mail.addPersonalization(personalization);
		mail.setTemplateId(templateId);
		mail.setFrom(from);

		Request request = new Request();
		try {
			request.setMethod(Method.POST);
			request.setEndpoint("mail/send");
			request.setBody(mail.build());
			Response response = sg.api(request);
			logger.info(response.getStatusCode());
			logger.info(response.getBody());
			logger.info(response.getHeaders());

			logger.info(ANSIColour.GREEN+"SendGrid Message Sent!"+ANSIColour.RESET);
		} catch (IOException e) {
			logger.error(e);
		}
		
	}

}
