
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import io.vertx.reactivex.ext.web.handler.BodyHandler;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class ApiGateWay extends AbstractVerticle {

    private String com[]={"/api/userApi/selectAllUsers","/api/holidayApi/selectAllHolidays"};

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ApiGateWay());
    }

    @Override
    public void start(Future<Void> future) throws Exception{
        super.start();

        Router router = Router.router(vertx);

        //Body Handler
        router.route("/*").handler(BodyHandler.create());

        // api gateway dispatch
        router.route("/api/*").handler(this::dispatchOneRequests);

        // api gateway for both select all
        router.route("/both/Test1").handler(this::dispatchBothTest1);

        router.route("/both/Test3").handler(this::dispatchBothTest3);

        router.route("/both/Test4").handler(this::dispatchBothTest4);

        // api info
        router.route("/api").handler(this::Info);

        // create http server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8888,"localhost", ar -> {
                    if (ar.succeeded()) {
                        future.isComplete();
                        LOGGER.info("API Gateway is running on port " + 8888);

                    } else {
                        future.failed();
                    }
                });
        }


        //Nie dziala blokuje event loop!!!

//    CompletableFuture<HandelerFuture[]> dispatchBoth(String... urls) {
//        int port=0;
//        ArrayList<HandelerFuture> futures = new ArrayList<>(); // all results
//        for (String url : urls) {
//            if(url=="/api/userApi/selectAllUsers"){
//                port=8081;
//            }else if(url=="/api/holidayApi/selectAllHolidays"){
//                port=8080;
//            }
//            HandelerFuture future = new HandelerFuture();
//            futures.add(future);
//            WebClient client = WebClient.create(vertx);
//            client.get(port, "localhost", url)
//                    .send(future);
//        }
//        CompletableFuture all = new CompletableFuture();
//        HandelerFuture[] array = futures.toArray(new HandelerFuture[0]);
//        CompletableFuture.allOf(array)
//                .thenRunAsync(() -> all.complete(array));
//        return all;
//    }
//
//
//    public void DispatchDoBoth(RoutingContext routingContext){
//        CompletableFuture<HandelerFuture[]> future = dispatchBoth(com);
//        HandelerFuture[] results = new HandelerFuture[0];
//        try {
//            results = future.get();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
//        JsonArray finalArray = new JsonArray();
//        for (HandelerFuture result:results) {
//            try {
//                // extract partial json array
//                JsonArray partialArray = result.get();
//
//                finalArray.addAll(partialArray);
//
//            } catch (Exception e) {
//                LOGGER.info("ERROR in DispatchDoBoth!!!");
//                e.printStackTrace();
//            }
//        }
//        routingContext.response().end(finalArray.encodePrettily());
//    }


    //---------Just single Http Request---------- Test2 -------
    private void dispatchOneRequests(RoutingContext routingContext) {
        String path = routingContext.normalisedPath();
        if(path.contains("userApi")){
            doDispatchOne(routingContext,8081,path);
        }else if(path.contains("holidayApi")){
            doDispatchOne(routingContext,8080,path);
        }
    }

    private void doDispatchOne(RoutingContext routingContext, int port, String command) {
        WebClient client = WebClient.create(vertx);
        if(command.contains("delete")){
            Single<HttpResponse<Buffer>> delete = client
                    .delete(port, "localhost", command)
                    .rxSend();
            delete.subscribe(resp-> routingContext.response().end("Deleted successfully"));
        }else if(command.contains("create")){
            Single<HttpResponse<Buffer>> post = client
                    .post(port, "localhost", command)
                    .rxSend();
            post.subscribe(resp-> routingContext.response().end("Creation successful"));
        }else {
            Single <HttpResponse<JsonArray>> get = client
                    .get(port, "localhost", command)
                    .as(BodyCodec.jsonArray())
                    .rxSend();
            get.subscribe(resp-> routingContext.response().end(resp.body().encodePrettily()));

        }
    }


    private void Info(RoutingContext routingContext) {
        routingContext.response().end("<h1><font color=\"red\">UserApi information</font></h1><br>" +
                "Delete user: api/userApi/deleteUser/:id<br>" +
                "Get limited number of users: api/userApi/limit1/:id1/limit2/:id2<br>" +
                "Get all users: api/userApi/selectAllUsers<br>" +
                "Create user: api/userApi/createUser<br>" +
                "Get one specific user: api/userApi/selectUser/:id<br>" +
                "----------------------------------------------------------<br>" +
                "<h1><font color=\"red\">HolidayApi information</h1></font><br>" +
                "Delete holiday: api/holidayApi/deleteHoliday/:id<br>" +
                "Get all holidays: api/holidayApi/selectAllHolidays<br>" +
                "Create holiday: api/holidayApi/createHoliday<br>" +
                "Get one specific holiday: api/holidayApi/selectHoliday/:id<br>"+
                "<h1><font color=\"red\">BOTH Tests</font></h1><br>" +
                "Test 1: both/Test1 <font color=\"red\"> Dose not work! </font><br>"+
                "Test 3: both/Test3 <font color=\"blue\"> Dose WORK but not all the time </font><br>"+
                "Test 4: both/Test4 <font color=\"green\"> Dose Work! </font><br>");
    }

    @Override
        public void stop() {
        LOGGER.info("API Gateway stopped working");
    }


    public void dispatchBothTest4(RoutingContext context){
        HttpClient client = vertx.createHttpClient();

        HttpClientRequest req1 = client.request(HttpMethod.GET, 8080, "localhost", "/api/holidayApi/selectAllHolidays");
        HttpClientRequest req2 = client.request(HttpMethod.GET, 8081, "localhost", "/api/userApi/selectAllUsers");

        Flowable<JsonArray> obs1 = req1.toFlowable().flatMap(element->element.toFlowable()).
                map(buffer -> new JsonArray(buffer.toString("UTF-8")));
        Flowable<JsonArray> obs2 = req2.toFlowable().flatMap(element->element.toFlowable()).
                map(buffer -> new JsonArray(buffer.toString("UTF-8")));

        obs1.zipWith(obs2, (b1, b2) -> new JsonObject()
                .put("Request 1: ", b1)
                .put("Request 2: ", b2))
                .subscribe(json -> context.response().end(json.encodePrettily()),
                        Throwable::printStackTrace);
        req1.end();
        req2.end();
    }

    //FIXME
    public void dispatchBothTest3(RoutingContext context){
        JsonArray jsonArray = new JsonArray();
        WebClient client = WebClient.create(vertx);
        Single<HttpResponse<JsonArray>> single1 = client
                .get(8080,"localhost","/api/holidayApi/selectAllHolidays")
                .as(BodyCodec.jsonArray()).rxSend();
        single1.subscribe(
                server-> jsonArray.addAll(server.body()),
                failure-> System.out.println(failure.getMessage()));

        Single<HttpResponse<JsonArray>> single2 = client
                .get(8081,"localhost","/api/userApi/selectAllUsers")
                .as(BodyCodec.jsonArray()).rxSend();
        single2.subscribe(
                server->{
                    jsonArray.addAll(server.body());
                    context.response().end(jsonArray.encodePrettily());
                },
                failure-> System.out.println(failure.getMessage()));

    }

//-------------------------------------------------------------------- I Don't know!!!!--------------------------------------------------------------------

    //FIXME
    private void dispatchBothTest1(RoutingContext routingContext) {

        Observer<String> observer = new Observer<String>() {

            JsonArray jsonArray = new JsonArray();

            @Override
            public void onSubscribe(Disposable disposable) {
                System.out.println("Start");
            }

            @Override
            public void onNext(String s) {
                    if(s=="/api/userApi/selectAllUsers") {
                            WebClient client = WebClient.create(vertx);
                                client
                                    .get(8081, "localhost", s)
                                    .send(ar->{
                                        if (ar.succeeded()) {
                                            HttpResponse<Buffer> response = ar.result();

                                            jsonArray.addAll(response.bodyAsJsonArray());

                                            System.out.println();
                                            System.out.println(jsonArray.encodePrettily());
                                            System.out.println();
                                        } else {
                                            System.out.println("Something went wrong " + ar.cause().getMessage());
                                        }
                                    });
                    }else if(s=="/api/holidayApi/selectAllHolidays") {
                            WebClient client = WebClient.create(vertx);
                            client
                                    .get(8080, "localhost", s)
                                    .send(ar -> {
                                        HttpResponse<Buffer> response = ar.result();
                                        if (ar.succeeded()) {
                                            jsonArray.addAll(response.bodyAsJsonArray());


                                            System.out.println();
                                            System.out.println(jsonArray.encodePrettily());
                                            System.out.println();

                                        } else {
                                            System.out.println("Something went wrong " + ar.cause().getMessage());
                                        }
                                    });
                            }
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {

                    System.out.println();
                    System.out.println(jsonArray.encodePrettily());
                    System.out.println();
                    routingContext.response().end(jsonArray.encodePrettily());
            }
        };
        Observable.fromArray(com).subscribe(observer);
    }
    //------------------------------------------------------------------------------------------------------------------
}

    //Event loop jest blokowany
//class HandelerFuture extends CompletableFuture<JsonArray>
//        implements Handler<AsyncResult<HttpResponse<Buffer>>> {
//    @Override
//    public void handle(AsyncResult<HttpResponse<Buffer>> ar) {
//        if (ar.succeeded()) {
//            JsonArray array = ar.result().bodyAsJsonArray();
//            super.complete(array);
//        } else {
//            super.completeExceptionally(ar.cause());
//        }
//    }
//}