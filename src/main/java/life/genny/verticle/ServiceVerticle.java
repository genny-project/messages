package life.genny.verticle;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

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

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

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
