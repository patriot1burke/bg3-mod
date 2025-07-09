package org.baldurs.forge;

import io.quarkus.logging.Log;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;

public class StaticResources {
    @Route(path = "/static/img/*", methods = Route.HttpMethod.GET)
    public void images(RoutingContext rc) {
        //Log.info("STATIC RESOURCES" + rc.request().path());
        StaticHandler staticHandler = StaticHandler.create(FileSystemAccess.ROOT, "/home/bburke/projects/baldurs-forge").setCachingEnabled(false).setDirectoryListing(true);
        staticHandler.handle(rc);
    }
}
