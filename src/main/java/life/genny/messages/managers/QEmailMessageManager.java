package life.genny.messages.managers;

import java.net.http.HttpResponse;
import java.util.Map;
import javax.inject.Inject;

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
		BaseEntity target = (BaseEntity) contextMap.get("RECIPIENT");

		if (target == null) {
			log.error(ANSIColour.RED+"Target is NULL"+ANSIColour.RESET);
			return;
		}
		if (projectBe == null) {
			log.error(ANSIColour.RED+"ProjectBe is NULL"+ANSIColour.RESET);
			return;
		}

		String targetEmail = target.getValue("PRI_EMAIL", null);

		if (targetEmail == null) {
			log.error(ANSIColour.RED+"Target " + target.getCode() + ", PRI_EMAIL is NULL"+ANSIColour.RESET);
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

		// Mail Merging Data
//		body = MergeUtils.merge(body, contextMap);

		body = "test email from QEmailMessageManager";

		try {

//			mailer.send(Mail.withText(targetEmail, subject, body));

			String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");

			HttpResponse<String> post = HttpUtils.post("https://api.sendgrid.com/v3/mail/send", body, sendGridApiKey);

			log.info(ANSIColour.GREEN + "Email to " + targetEmail +" is sent" + ANSIColour.RESET);
			log.info(ANSIColour.GREEN + "Post response -> " + post);
			
		} catch (Exception e) {
			log.error("ERROR -> ", e);
		} 

	}
}
