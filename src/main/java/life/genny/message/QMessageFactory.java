package life.genny.message;

import life.genny.qwanda.message.QBaseMSGMessageType;

public class QMessageFactory {
	
	public QMessageProvider getMessageProvider(QBaseMSGMessageType messageType)
	  {
		QMessageProvider provider;
		System.out.println("message type::"+messageType.toString());
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
	    default:
	    	provider = new QEmailMessageManager();    
	    }
	    
	    return provider;
	    	
	  }

}
