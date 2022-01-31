package life.genny.messages.managers;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Logger;

import life.genny.qwandaq.message.QBaseMSGMessageType;

public class QMessageFactory {
	
	private static final Logger log = Logger.getLogger(QMessageFactory.class);
	
	public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType)
	  {
		QMessageProvider provider;
		log.info("message type::"+messageType.toString());
	    switch(messageType) {
	    case SMS:
	    	provider = new QSMSMessageManager();
	    	break;
	    case EMAIL:
	    	provider = new QEmailMessageManager();
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
