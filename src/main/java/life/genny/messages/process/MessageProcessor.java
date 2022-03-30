package life.genny.messages.process;

import life.genny.messages.managers.QMessageFactory;
import life.genny.messages.managers.QMessageProvider;
import life.genny.messages.util.MsgUtils;
import life.genny.qwandaq.attribute.Attribute;
import life.genny.qwandaq.attribute.EntityAttribute;
import life.genny.qwandaq.entity.BaseEntity;
import life.genny.qwandaq.message.QBaseMSGMessageType;
import life.genny.qwandaq.message.QMessageGennyMSG;
import life.genny.qwandaq.models.ANSIColour;
import life.genny.qwandaq.models.GennySettings;
import life.genny.qwandaq.models.GennyToken;
import life.genny.qwandaq.utils.BaseEntityUtils;
import life.genny.qwandaq.utils.KeycloakUtils;
import life.genny.qwandaq.utils.MergeUtils;
import life.genny.qwandaq.utils.QwandaUtils;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

public class MessageProcessor {

    private static final Logger log = Logger.getLogger(MessageProcessor.class);

    static QMessageFactory messageFactory = new QMessageFactory();

    /**
     * Generic Message Handling method.
     *
     * @param message
     * @param serviceToken
     * @param userToken
     */
    public static void processGenericMessage(QMessageGennyMSG message, BaseEntityUtils beUtils) {

        // Begin recording duration
        long start = System.currentTimeMillis();

        GennyToken userToken = beUtils.getGennyToken();
        GennyToken serviceToken = beUtils.getServiceToken();
        String realm = beUtils.getGennyToken().getRealm();

        log.debug("Realm is " + realm + " - Incoming Message :: " + message.toString());

        if (message == null) {
            log.error(ANSIColour.RED + "GENNY COM MESSAGE IS NULL" + ANSIColour.RESET);
        }

        BaseEntity projectBe = beUtils.getBaseEntityByCode("PRJ_" + realm.toUpperCase());

        // try {
        // log.warn("*** HORRIBLE ACC HACK TO DELAY FOR 10 SEC TO ALLOW CACHE ITEM TO BE
        // COMPLETE");
        // Thread.sleep(10000);
        // } catch (InterruptedException e1) {
        // e1.printStackTrace();
        // } /* TODO: horrible hack by ACC to give the be time to save - should use
        // Shleemy , hopefully updated cache will help */

        List<QBaseMSGMessageType> messageTypeList = Arrays.asList(message.getMessageTypeArr());

        String[] recipientArr = message.getRecipientArr();
        List<BaseEntity> recipientBeList = new ArrayList<BaseEntity>();

        BaseEntity templateBe = null;
        if (message.getTemplateCode() != null) {
            templateBe = beUtils.getBaseEntityByCode(message.getTemplateCode());
        }

        if (templateBe != null) {
            String cc = templateBe.getValue("PRI_CC", null);
            String bcc = templateBe.getValue("PRI_BCC", null);

            if (cc != null) {
                log.debug("Using CC from template BaseEntity");
                cc = beUtils.cleanUpAttributeValue(cc);
                message.getMessageContextMap().put("CC", cc);
            }
            if (bcc != null) {
                log.debug("Using BCC from template BaseEntity");
                bcc = beUtils.cleanUpAttributeValue(bcc);
                message.getMessageContextMap().put("BCC", bcc);
            }
        }

        // Create context map with BaseEntities
        HashMap<String, Object> baseEntityContextMap = new HashMap<>();
        baseEntityContextMap = createBaseEntityContextMap(beUtils, message);
        baseEntityContextMap.put("PROJECT", projectBe);

        if (templateBe == null) {
            log.warn(ANSIColour.YELLOW + "No Template found for " + message.getTemplateCode() + ANSIColour.RESET);
        } else {
            log.info("Using TemplateBE " + templateBe.getCode());

            // Handle any default context associations
            String contextAssociations = templateBe.getValue("PRI_CONTEXT_ASSOCIATIONS", null);
            if (contextAssociations != null) {
                MergeUtils.addAssociatedContexts(beUtils, baseEntityContextMap, contextAssociations, false);
            }

            // Check for Default Message
            if (Arrays.stream(message.getMessageTypeArr()).anyMatch(item -> item == QBaseMSGMessageType.DEFAULT)) {
                // Use default if told to do so
                List<String> typeList = beUtils.getBaseEntityCodeArrayFromLNKAttr(templateBe, "PRI_DEFAULT_MSG_TYPE");
                try {
                    messageTypeList = typeList.stream().map(item -> QBaseMSGMessageType.valueOf(item))
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    log.error(e.getLocalizedMessage());
                }
            }
        }

        QwandaUtils qwandaUtils = new QwandaUtils();
        Attribute emailAttr = qwandaUtils.getAttribute("PRI_EMAIL");
        Attribute mobileAttr = qwandaUtils.getAttribute("PRI_MOBILE");

        for (String recipient : recipientArr) {

            BaseEntity recipientBe = null;

            if (recipient.contains("[\"") && recipient.contains("\"]")) {
                // This is a BE Code
                String code = beUtils.cleanUpAttributeValue(recipient);
                recipientBe = beUtils.getBaseEntityByCode(code);
            } else {
                // Probably an actual email
                String code = "RCP_" + UUID.randomUUID().toString().toUpperCase();
                recipientBe = new BaseEntity(code, recipient);

                try {
                    EntityAttribute email = new EntityAttribute(recipientBe, emailAttr, 1.0, recipient);
                    recipientBe.addAttribute(email);
                    EntityAttribute mobile = new EntityAttribute(recipientBe, mobileAttr, 1.0, recipient);
                    recipientBe.addAttribute(mobile);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            if (recipientBe != null) {
                recipientBeList.add(recipientBe);
            } else {
                log.error(ANSIColour.RED + "Could not process recipient " + recipient + ANSIColour.RESET);
            }
        }

        BaseEntity unsubscriptionBe = beUtils.getBaseEntityByCode("COM_EMAIL_UNSUBSCRIPTION");
        log.info("unsubscribe be :: " + unsubscriptionBe);
        String templateCode = message.getTemplateCode() + "_UNSUBSCRIBE";

        /* Iterating and triggering email to each recipient individually */
        for (BaseEntity recipientBe : recipientBeList) {

            // Set our current recipient
            baseEntityContextMap.put("RECIPIENT", recipientBe);

            // Process any URL contexts for this recipient
            if (baseEntityContextMap.containsKey("URL:ENCODE")) {
                // Fetch form contexts
                String[] componentArray = ((String) baseEntityContextMap.get("URL:ENCODE")).split("/");
                // Init and grab url structure
                String parentCode = null;
                String code = null;
                String targetCode = null;
                if (componentArray.length > 0) {
                    parentCode = componentArray[0];
                }
                if (componentArray.length > 1) {
                    code = componentArray[1];
                }
                if (componentArray.length > 2) {
                    targetCode = componentArray[2];
                }

                log.info("Fetching Token from " + serviceToken.getKeycloakUrl() + " for user "
                        + recipientBe.getCode() + " with realm " + serviceToken.getRealm());

                // Fetch access token
                String accessToken = KeycloakUtils.getImpersonatedToken(recipientBe, serviceToken, projectBe);

                // Encode URL and put back in the map
                String url = MsgUtils.encodedUrlBuilder(GennySettings.projectUrl + "/home", parentCode, code,
                        targetCode, accessToken);
                log.info("Access URL: " + url);
                baseEntityContextMap.put("URL", url);
            }

            // Iterate our array of send types
            for (QBaseMSGMessageType msgType : messageTypeList) {

                /* Get Message Provider */
                QMessageProvider provider = messageFactory.getMessageProvider(msgType);

                Boolean isUserUnsubscribed = false;
                if (unsubscriptionBe != null) {
                    /* check if unsubscription list for the template code has the userCode */
                    String templateAssociation = unsubscriptionBe.getValue(templateCode, "");
                    isUserUnsubscribed = templateAssociation.contains(recipientBe.getCode());
                }

                if (provider != null) {
                    /*
                     * if user is unsubscribed, then dont send emails. But toast and sms are still
                     * applicable
                     */
                    if (isUserUnsubscribed && !msgType.equals(QBaseMSGMessageType.EMAIL)) {
                        log.info("unsubscribed");
                        provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
                    }

                    /* if subscribed, allow messages */
                    if (!isUserUnsubscribed) {
                        log.info("subscribed");
                        provider.sendMessage(beUtils, templateBe, baseEntityContextMap);
                    }
                } else {
                    log.error(ANSIColour.RED + ">>>>>> Provider is NULL for entity: " + ", msgType: "
                            + msgType.toString() + " <<<<<<<<<" + ANSIColour.RESET);
                }
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("FINISHED PROCESSING MESSAGE :: time taken = " + String.valueOf(duration));
    }

    private static HashMap<String, Object> createBaseEntityContextMap(BaseEntityUtils beUtils,
            QMessageGennyMSG message) {

        HashMap<String, Object> baseEntityContextMap = new HashMap<>();

        for (Map.Entry<String, String> entry : message.getMessageContextMap().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            String logStr = "key: " + key + ", value: " + (key.toUpperCase().equals("PASSWORD") ? "REDACTED" : value);
            log.info(logStr);

            if ((value != null) && (value.length() > 4)) {

                // MUST CONTAIN A BE CODE
                if (value.matches("[A-Z]{3}\\_.*") && !key.startsWith("URL")) {
                    // Create Array of Codes
                    String[] codeArr = beUtils.cleanUpAttributeValue(value).split(",");
                    log.info("Fetching contextCodeArray :: " + Arrays.toString(codeArr));
                    // Convert to BEs
                    BaseEntity[] beArray = Arrays.stream(codeArr)
                            .map(itemCode -> (BaseEntity) beUtils.getBaseEntityByCode(itemCode))
                            .toArray(BaseEntity[]::new);

                    if (beArray.length == 1) {
                        baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray[0]);
                    } else {
                        baseEntityContextMap.put(entry.getKey().toUpperCase(), beArray);
                    }

                    continue;

                }
            }

            // By Default, add it as is
            baseEntityContextMap.put(entry.getKey().toUpperCase(), value);
        }

        return baseEntityContextMap;
    }

}
