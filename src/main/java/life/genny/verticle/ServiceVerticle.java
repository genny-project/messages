package life.genny.verticle;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;

import life.genny.channel.Routers;
import life.genny.channels.EBCHandlers;
import life.genny.cluster.Cluster;

import life.genny.cluster.CurrentVtxCtx;
import life.genny.eventbus.EventBusInterface;
import life.genny.eventbus.EventBusVertx;
import life.genny.eventbus.VertxCache;
import life.genny.qwandautils.GennyCacheInterface;

import life.genny.utils.VertxUtils;

public class ServiceVerticle extends AbstractVerticle {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Override
	public void start() {
		log.info("Setting up routes new version");
		final Future<Void> startFuture = Future.future();
		Cluster.joinCluster().compose(res -> {
		    log.info("Starting eventbus:::::::::::::::::::::::::::::");
			EventBusInterface eventBus = new EventBusVertx();
		    log.info("Starting VertxCache:::::::::::::::::::::::::::");
			GennyCacheInterface vertxCache = new VertxCache();
		    log.info("Starting init:::::::::::::::::::::::::::::::::");
			VertxUtils.init(eventBus, vertxCache);
		    log.info("Starting routers::::::::::::::::::::::::::::::");
			Routers.routers(vertx);
		    log.info("Starting activate:::::::::::::::::::::::::::::");
			Routers.activate(vertx);
			log.info("Messages now ready");

			EBCHandlers.registerHandlers(CurrentVtxCtx.getCurrentCtx().getClusterVtx().eventBus());
			startFuture.complete();
		}, startFuture);

	}
}
