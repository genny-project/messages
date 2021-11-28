package life.genny.messages.managers;

import java.lang.invoke.MethodHandles;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import life.genny.qwanda.message.QBaseMSGMessageType;

public class QMessageFactory {
	
	private static final Logger log = LoggerFactory
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
	
	public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType)
	  {
		QMessageProvider provider;
		log.info("message type::"+messageType.toString());
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
	    case SLACK:
	    	provider = new QSlackMessageManager();
	    	break;
	    case DEFAULT:
			// Default to Error Manager if no proper message is found
	    	provider = new QErrorManager();
	    	break;
	    default:
			// Default to Error Manager if no proper message is found
	    	provider = new QErrorManager();    
	    }
	    
	    return provider;
	    	
	  }

}
