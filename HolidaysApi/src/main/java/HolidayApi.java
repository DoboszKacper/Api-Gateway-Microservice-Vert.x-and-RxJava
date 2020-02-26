import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;


public class HolidayApi extends AbstractVerticle {
    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;
    public HolidayApi() {

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3308)
                .setHost("localhost")
                .setDatabase("holidays")
                .setUser("root")
                .setPassword("123");

        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        //Creating server and router
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        Route needThis = router.route("/api/holidayApi/*").handler(BodyHandler.create());

        //GET
        Route info = router
                .get("/api/holidayApi")
                .handler(this::Info);
        info.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Info unavailable");
        });

        //GET
        Route getId = router
                .get("/api/holidayApi/selectHoliday/:id")
                .handler(this::SelectOne);
        getId.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetId unavailable");
        });

        //POST
        Route post = router
                .post("/api/holidayApi/createHoliday")
                .handler(BodyHandler.create()).handler(this::CreateHoliday);
        post.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Create unavailable");
        });

        //GET
        Route getAll = router
                .get("/api/holidayApi/selectAllHolidays")
                .handler(this::SelectAllHolidays);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:"+statusCode);
        });

        //DELETE
        Route delete = router
                .delete("/api/holidayApi/deleteHoliday/:id")
                .handler(this::DeleteHoliday);
        delete.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Delete unavailable");
        });

        //Server config
        server.requestHandler(router).listen(8080,asyncResult->{
            if(asyncResult.succeeded()){
                LOGGER.info("Api running successfully on port 8080");
            }else{
                LOGGER.info("Error : " + asyncResult.cause());
            }
        });
    }

    private void Info(RoutingContext routingContext) {
        routingContext.response().end("<h1>HolidayApi information<h1><br>" +
                "<h2>Delete holiday: /holidayApi/deleteHoliday/:id<h2><br>" +
                "<h2>Get all holidays: /holidayApi/selectAllHolidays<h2><br>" +
                "<h2>Create holiday: /holidayApi/createHoliday<h2><br>" +
                "<h2>Get one specific holiday: /holidayApi/selectHoliday/:id<h2>");
    }

    //------------------------------------------SELECT ONE----------------------------------------------//
    public void SelectOne(RoutingContext routingContext) {
        client.query("SELECT * FROM holidays",res->{
            if(res.succeeded()){
                Holiday holiday = new Holiday();
                int id = Integer.parseInt(routingContext.request().getParam("id"));
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    if(row.getInteger(0)==id) {
                        holiday.setHolidayId(row.getInteger(0));
                        holiday.setUserId(row.getInteger(1));
                        holiday.setPlace(row.getString(2));
                        holiday.setDate(row.getLocalDate(3));
                    }
                }
                JsonObject jsonObject =JsonObject.mapFrom(holiday);

                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonObject.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------SELECT ALL USERS----------------------------------------------//
    public void SelectAllHolidays(RoutingContext routingContext) {
        client.query("SELECT * FROM holidays",res->{
            if(res.succeeded()){
                RowSet<Row> result = res.result();
                List<Holiday> list = new ArrayList<>();
                JsonArray jsonHolidays = new JsonArray(list);
                for (Row row : result) {
                    Holiday holiday = new Holiday();
                    holiday.setHolidayId(row.getInteger(0));
                    holiday.setUserId(row.getInteger(1));
                    holiday.setPlace(row.getString(2));
                    holiday.setDate(row.getLocalDate(3));
                    list.add(holiday);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonHolidays.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------DELETE----------------------------------------------//
    public void DeleteHoliday(RoutingContext routingContext){
        int id = Integer.parseInt(routingContext.request().getParam("id"));
        client.query("DELETE FROM holidays WHERE holiday_id='"+id+"';", res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("Holiday of id: "+id+" deleted");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------Create----------------------------------------------//
    public void CreateHoliday(RoutingContext routingContext){
        final Holiday holiday = Json.decodeValue(routingContext.getBody(),Holiday.class);
        int idU = holiday.getUserId();
        String place = holiday.getPlace();
        LocalDate date = holiday.getDate();

        client.preparedQuery("insert into holidays (user_id,place,datee) values (?, ?, ?)", Tuple.of(idU,place,date), res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("Holiday on: "+date+" created");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
}
