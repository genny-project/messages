package life.genny.messages.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.*;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.HttpUtils;
import life.genny.qwandaq.utils.MergeUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jboss.logging.Logger;

import javax.json.*;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class QSendGridRelayMessageManager implements QMessageProvider {

    private static final Logger log = Logger.getLogger(QSendGridRelayMessageManager.class);

    @Override
    public void sendMessage(BaseEntityUtils beUtils, BaseEntity templateBe, Map<String, Object> contextMap) {

        log.info("SendGrid email type");

        BaseEntity recipientBe = (BaseEntity) contextMap.get("RECIPIENT");
        BaseEntity projectBe = (BaseEntity) contextMap.get("PROJECT");

        recipientBe = beUtils.getBaseEntityByCode(recipientBe.getCode());

        if (templateBe == null) {
            log.error(ANSIColour.RED + "TemplateBE passed is NULL!!!!" + ANSIColour.RESET);
            return;
        }

        if (recipientBe == null) {
            log.error(ANSIColour.RED + "Target is NULL" + ANSIColour.RESET);
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
            log.error(ANSIColour.RED + "Target " + recipientBe.getCode() + ", PRI_EMAIL is NULL" + ANSIColour.RESET);
            return;
        }

        String subject = templateBe.getValue("PRI_SUBJECT", null);
        String body = templateBe.getValue("PRI_BODY", null);

        String sendGridEmailSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_SENDER");
        String sendGridEmailNameSender = projectBe.getValueAsString("ENV_SENDGRID_EMAIL_NAME_SENDER");
        String sendGridApiKey = projectBe.getValueAsString("ENV_SENDGRID_API_KEY");

        log.info("The name for email sender " + sendGridEmailNameSender);
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
                                    log.info("formatted date: " + valueString);

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
            } else if (value.getClass().equals(String.class)) {
                log.info("Processing key as STRING: " + key);
                templateData.put(key, (String) value);
            }
        }
        // Base Wrapper
        JsonObjectBuilder mailJsonObjectBuilder = Json.createObjectBuilder();

        JsonObject fromJsonObject = Json
                .createObjectBuilder()
                .add("name", sendGridEmailNameSender)
                .add("email", sendGridEmailSender)
                .build();

        JsonObject toJsonObject = Json
                .createObjectBuilder()
                .add("email", recipient)
                .build();

        JsonArray tosJsonArray = Json.createArrayBuilder()
                .add(toJsonObject)
                .build();

        String urlBasedAttribute = GennySettings.projectUrl().replace("https://", "").replace(".gada.io", "").replace("-", "_").toUpperCase();
        log.info("Searching for email attr " + urlBasedAttribute);
        String dedicatedTestEmail = projectBe.getValue("EML_" + urlBasedAttribute, null);
        if (dedicatedTestEmail != null) {
            log.info("Found email " + dedicatedTestEmail + " for project attribute EML_" + urlBasedAttribute);
            tosJsonArray = Json.createArrayBuilder()
                    .add(Json
                            .createObjectBuilder()
                            .add("email", dedicatedTestEmail)
                            .build())
                    .build();
        }


        JsonArrayBuilder personalizationArrayBuilder = Json.createArrayBuilder();
        JsonObjectBuilder personalizationInnerObjectWrapper = Json.createObjectBuilder();
        personalizationInnerObjectWrapper.add("to", tosJsonArray);
        personalizationInnerObjectWrapper.add("subject", subject);

        // Handle CC and BCC
        Object ccVal = contextMap.get("CC");
        Object bccVal = contextMap.get("BCC");

        if (ccVal != null) {
            BaseEntity[] ccArray = new BaseEntity[1];

            if (ccVal.getClass().equals(BaseEntity.class)) {
                ccArray[0] = (BaseEntity) ccVal;
            } else {
                ccArray = (BaseEntity[]) ccVal;
            }

            JsonArrayBuilder ccJsonArrayBuilder = Json.createArrayBuilder();

            for (BaseEntity item : ccArray) {

                String email = item.getValue("PRI_EMAIL", null);
                if (email != null) {
                    email = email.trim();
                }

                if (email != null && !email.equals(recipient)) {
                    ccJsonArrayBuilder.add(
                            Json
                                    .createObjectBuilder()
                                    .add("email", email)
                                    .build()
                    );
                    log.info(ANSIColour.BLUE + "Found CC Email: " + email + ANSIColour.RESET);
                }
            }
            personalizationInnerObjectWrapper.add("cc", ccJsonArrayBuilder.build());
        }

        if (bccVal != null) {
            BaseEntity[] bccArray = new BaseEntity[1];

            JsonArrayBuilder bccJsonArrayBuilder = Json.createArrayBuilder();

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

                if (email != null && !email.equals(recipient)) {
                    bccJsonArrayBuilder.add(
                            Json
                                    .createObjectBuilder()
                                    .add("email", email)
                                    .build()
                    );
                    log.info(ANSIColour.BLUE + "Found BCC Email: " + email + ANSIColour.RESET);
                }
            }
            personalizationInnerObjectWrapper.add("bcc", bccJsonArrayBuilder.build());
        }

        Map<String, Object> finalData = new HashMap<>();

        for (String key : templateData.keySet()) {
            finalData.put(key, templateData.get(key));
        }

        personalizationArrayBuilder.add(personalizationInnerObjectWrapper.build());

        mailJsonObjectBuilder.add("personalizations", personalizationArrayBuilder.build());
        mailJsonObjectBuilder.add("subject", subject);
        mailJsonObjectBuilder.add("from", fromJsonObject);

        JsonArrayBuilder contentArray = Json.createArrayBuilder();
        JsonObjectBuilder contentJson = Json.createObjectBuilder();

        body = StringEscapeUtils.unescapeHtml4(body);
        System.out.println("body unescaped: " + body);
        body = parseToTemplate(body, finalData);

        contentJson.add("type", "text/html");
        contentJson.add("value", body);
        contentArray.add(contentJson.build());
        mailJsonObjectBuilder.add("content", contentArray.build());

        sendRequest(mailJsonObjectBuilder.build(), sendGridApiKey);
    }

    private void sendRequest(JsonObject mail, String apiKey) {
        // TODO: Fetch from env
        String path = "https://api.sendgrid.com/v3/mail/send";
        try {
            String requestBody = mail.toString();
            System.out.println("####### requestBody: " + requestBody);

            HttpResponse<String> httpResponse = HttpUtils.post(path, requestBody, apiKey);
            if (httpResponse != null) {
                int statusCode = httpResponse.statusCode();
                System.out.println("####### response: " + httpResponse.body());
                System.out.println("####### statusCode: " + statusCode);
            } else {
                System.out.println("####### Sendgrid NULL!");
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    private String parseToTemplate(String template, Map<String, Object> data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            System.out.println("##### template: " + template);

            JsonNode jsonNode = objectMapper.valueToTree(data);
            Handlebars handlebars = new Handlebars();
            handlebars.registerHelper("json", Jackson2Helper.INSTANCE);

            Context context = Context
                    .newBuilder(jsonNode)
                    .resolver(
                            JsonNodeValueResolver.INSTANCE,
                            JavaBeanValueResolver.INSTANCE,
                            FieldValueResolver.INSTANCE,
                            MapValueResolver.INSTANCE,
                            MethodValueResolver.INSTANCE
                    )
                    .build();
            Template handleBarTemplate = handlebars.compileInline(template);
            String output = handleBarTemplate.apply(context);
            System.out.println("##### parsed template: " + output);
            return output;
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return null;
        }
    }


}
