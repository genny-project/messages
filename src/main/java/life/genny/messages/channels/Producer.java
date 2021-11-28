package life.genny.messages.channels;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class Producer {


  @Inject @Channel("webdataout") Emitter<String> webData;
  public Emitter<String> getToWebData() {
    return webData;
  }

}

