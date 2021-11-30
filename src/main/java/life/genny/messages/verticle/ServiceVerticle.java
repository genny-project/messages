package life.genny.messages.verticle;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.Startup;
import life.genny.channel.Routers;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.utils.VertxUtils;
import io.vertx.core.Vertx;

@Startup
@ApplicationScoped
public class ServiceVerticle {

	private static final Logger log = Logger.getLogger(ServiceVerticle.class);

  @Inject 
  Vertx vertx;

	@PostConstruct
	public void start() {
		log.info("Setting up routes");
		EventBusInterface eventBus = new EventBusVertx();
		GennyCacheInterface vertxCache = new VertxCache();
		VertxUtils.init(eventBus, vertxCache);
		Routers.routers(vertx);
		Routers.activate(vertx);
	}
}
