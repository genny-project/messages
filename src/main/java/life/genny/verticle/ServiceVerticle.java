package life.genny.verticle;

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
	 @Override
	  public void start() {
	    final Future<Void> startFuture = Future.future();
	    Cluster.joinCluster().compose(res -> {
	      final Future<Void> fut = Future.future();
	        EventBusInterface eventBus = new EventBusVertx();
	        GennyCacheInterface vertxCache = new VertxCache();
	        VertxUtils.init(eventBus,vertxCache);

	        Routers.routers(vertx);
	        Routers.activate(vertx);
	        EBCHandlers.registerHandlers(CurrentVtxCtx.getCurrentCtx().getClusterVtx().eventBus());
	        fut.complete();
	      }, startFuture);
	  }
}
