package life.genny.message;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.github.alexanderwe.bananaj;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.eventbus.EventBus;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QBaseMSGMessage;
import life.genny.qwanda.message.QBaseMSGMessageTemplate;
import life.genny.qwanda.message.QMessageGennyMSG;
import life.genny.qwandautils.MergeUtil;
import life.genny.util.MergeHelper;

public class QSMSMessageManager implements QMessageProvider {

	String mailchimpAPIkey = projectBe.getValue("ENV_MAILCHIMP_API_KEY", null);
    System.out.println(mailchimpAPIkey);

    MailChimpConnection mailchimpconn = new MailChimpConnection("mailchimpAPIkey");
    
    List<MailChimpList> allLists = mailchimpconn.getLists();
	System.out.println(allLists);

}