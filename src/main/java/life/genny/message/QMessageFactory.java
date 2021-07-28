package life.genny.message;

import java.lang.invoke.MethodHandles;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.message.QBaseMSGMessageType;

public class QMessageFactory {
	
	private static final Logger logger = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType)
	  {
		QMessageProvider provider;
		logger.info("message type::"+messageType.toString());
	    switch(messageType) {
	    case SMS:
	    	provider = new QSMSMessageManager();
	    	break;
	    case EMAIL:
	    	provider = new QVertxMailManager();
	    	break;
	    case TOAST:
	    	provider = new QToastMessageManager();
	    	break;
	    case SENDGRID:
	    	provider = new QSendGridMessageManager();
	    	break;
	    default:
	    	provider = new QEmailMessageManager();    
	    }
	    
	    return provider;
	    	
	  }

}
