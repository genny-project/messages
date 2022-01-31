package life.genny.messages.live.data;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

/**
 * InternalProducer --- Kafka smalltye producer objects to send to internal consumers backends
 * such as wildfly-rulesservice. 
 */

@ApplicationScoped
public class InternalProducer {

  @Inject @Channel("webcmdsout") Emitter<String> webCmds;
  public Emitter<String> getToWebCmds() {
    return webCmds;
  }

}
