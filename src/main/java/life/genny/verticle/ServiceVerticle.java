package life.genny.verticle;

import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.Future;
import life.genny.channels.EBCHandlers;
import life.genny.channels.Routers;
import life.genny.cluster.Cluster;

import life.genny.cluster.CurrentVtxCtx;


public class ServiceVerticle extends AbstractVerticle {
	 @Override
	  public void start() {
	    final Future<Void> startFuture = Future.future();
	    Cluster.joinCluster().compose(res -> {
	      final Future<Void> fut = Future.future();
	        Routers.routers(vertx);
	        Routers.activate(vertx);
	        EBCHandlers.registerHandlers(CurrentVtxCtx.getCurrentCtx().getClusterVtx().eventBus());
	        fut.complete();
	      }, startFuture);
	  }
}
