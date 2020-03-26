package io.vertx.book.message;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import rx.Single;

import java.util.concurrent.TimeUnit;

public class HelloConsumerMicroservice extends AbstractVerticle {

    @Override
    public void start() {
        vertx.createHttpServer()
            .requestHandler(
                req -> {
                    EventBus bus = vertx.eventBus();

                    Single<JsonObject> obs1 = bus
                        .<JsonObject>rxSend("hello", "Willy")
                            .subscribeOn(RxHelper.scheduler(vertx))
                            .timeout(3, TimeUnit.SECONDS)
                            .retry()
                            .map(Message::body);

                    Single<JsonObject> obs2 = bus
                        .<JsonObject>rxSend("hello", "Jane")
                            .subscribeOn(RxHelper.scheduler(vertx))
                            .timeout(3, TimeUnit.SECONDS)
                            .retry()
                            .map(Message::body);
                    
                    Single
                        .zip(obs1, obs2, (willy, jane) -> new JsonObject()
                            .put("Willy", willy.getString("message")
                                + " from "
                                    + willy.getString("served-by")
                            )
                            .put("Jane", jane.getString("message")
                                + " from "
                                + jane.getString("served-by")
                            ))
                        .subscribe(
                            x -> req.response().end(x.encodePrettily()),
                            t -> {
                                t.printStackTrace();
                                req.response().setStatusCode(500)
                                    .end(t.getMessage());
                            }
                        );
                }
            ).listen(8082);
    }
}
