package life.genny.messages.config;

import com.mitchellbosecke.pebble.PebbleEngine;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class MessageConfig {

    @Produces
    public PebbleEngine pebbleEngine () {
        System.out.println("##### Pebble engine instance created");
        return new PebbleEngine.Builder().build();
    }
}
