package life.genny.channels;

import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Observable;

public class EBConsumers {
	  
	private static Observable<Message<Object>> fromMessages;

	public static Observable<Message<Object>> getFromMessages() {
		return fromMessages;
	}

	public static void setFromMessages(Observable<Message<Object>> fromMessages) {
		EBConsumers.fromMessages = fromMessages;
	}

	public static void registerAllConsumer(EventBus eb){
		setFromMessages(eb.consumer("messages").toObservable());
	}
	
}
