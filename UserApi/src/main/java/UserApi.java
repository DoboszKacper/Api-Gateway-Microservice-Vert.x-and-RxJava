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
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

public class UserApi extends AbstractVerticle {
    private static MySQLConnectOptions connectOptions;
    private static MySQLPool client;

    public UserApi(){

        //Connect to the database
        connectOptions = new MySQLConnectOptions()
                .setPort(3309)
                .setHost("localhost")
                .setDatabase("users")
                .setUser("root")
                .setPassword("123");


        //Pool options
        PoolOptions poolOptions = new PoolOptions();

        //Create the client pool
        client = MySQLPool.pool(connectOptions,poolOptions);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception{

        //Create server and router
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        Route needThis = router.route("/api/userApi/*").handler(BodyHandler.create());

        //GET
        Route info = router
                .get("/api/userApi")
                .handler(this::Info);
        info.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Info unavailable");
        });

        //GET
        Route getId = router
                .get("/api/userApi/selectUser/:id")
                .handler(this::SelectOne);
        getId.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetId unavailable");
        });

        //POST
        Route post = router
                .post("/api/userApi/createUser")
                .handler(this::CreateUser);
        post.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Create unavailable");
        });

        //GET
        Route getAll = router
                .get("/api/userApi/selectAllUsers")
                .handler(this::SelectAllUsers);
        getAll.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetAll unavailable code:"+statusCode);
        });

        //GET
        Route getLimit = router
                .get("/api/userApi/limit1/:id1/limit2/:id2")
                .handler(this::SelectNumberOfUsers);
        getLimit.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! GetLimit unavailable");
        });

        //DELETE
        Route delete = router
                .delete("/api/userApi/deleteUser/:id")
                .handler(this::DeleteUser);
        delete.failureHandler(frc -> {
            int statusCode = frc.statusCode();
            HttpServerResponse response = frc.response();
            response.setStatusCode(statusCode).end("Sorry! Delete unavailable");
        });

        //Server config
        server.requestHandler(router).listen(8081,asyncResult->{
            if(asyncResult.succeeded()){
                LOGGER.info("Api running successfully on port 8081");
            }else{
                LOGGER.info("Error : " + asyncResult.cause());
            }
        });
    }

    private void Info(RoutingContext routingContext) {
        routingContext.response().end("<h1>UserApi information<h1><br>" +
                "<h2>Delete user: /userApi/deleteUser/:id<h2><br>" +
                "<h2>Get limited number of users: /userApi/limit1/:id1/limit2/:id2<h2><br>" +
                "<h2>Get all users: /userApi/selectAllUsers<h2><br>" +
                "<h2>Create user: /userApi/createUser<h2><br>" +
                "<h2>Get one specific user: /userApi/selectUser/:id<h2>");
    }

    //------------------------------------------DELETE----------------------------------------------//
    public void DeleteUser(RoutingContext routingContext){
        int id = Integer.parseInt(routingContext.request().getParam("id"));
        client.query("DELETE FROM users WHERE user_id="+id, res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("User of id: "+id+" deleted");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }

    //------------------------------------------SELECT ONE----------------------------------------------//
    public void SelectOne(RoutingContext routingContext){
        client.query("SELECT * FROM users", res->{
            if(res.succeeded()){
                int id = Integer.parseInt(routingContext.request().getParam("id"));
                User user = new User();
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    if(row.getInteger(0)==id) {
                        user.setUserId(row.getInteger(0));
                        user.setName(row.getString(1));
                        user.setPhone_number(row.getString(2));
                    }
                }
                JsonObject jsonObject =JsonObject.mapFrom(user);
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonObject.encodePrettily());
            }else{
                System.out.println("fail");
                client.close();
            }
        });
    }
    //------------------------------------------SELECT ALL USERS----------------------------------------------//
    public void SelectAllUsers(RoutingContext routingContext) {

        client.query("SELECT * FROM users",res->{
            if(res.succeeded()){
                List<User> users = new ArrayList<>();
                JsonArray jsonUsers = new JsonArray(users);
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    User user = new User();
                    user.setUserId(row.getInteger(0));
                    user.setName(row.getString(1));
                    user.setPhone_number(row.getString(2));
                    users.add(user);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonUsers.encodePrettily());
            }else{
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
    //------------------------------------------SELECT LIMIT----------------------------------------------//
    public void SelectNumberOfUsers(RoutingContext routingContext){
        int id1 = Integer.parseInt(routingContext.request().getParam("id1"));
        int id2 = Integer.parseInt(routingContext.request().getParam("id2"));
        client.query("SELECT * FROM users LIMIT "+id1+","+id2, res->{
            if(res.succeeded()){
                List<User> users = new ArrayList<>();
                JsonArray jsonUsers = new JsonArray(users);
                RowSet<Row> result = res.result();
                for (Row row : result) {
                    User user = new User();
                    user.setUserId(row.getInteger(0));
                    user.setName(row.getString(1));
                    user.setPhone_number(row.getString(2));
                    users.add(user);
                }
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .setStatusCode(200)
                        .end(jsonUsers.encodePrettily());
            }else{
                System.out.println("fail");
                client.close();
            }
        });
    }
    //------------------------------------------Create----------------------------------------------//
    public void CreateUser(RoutingContext routingContext){
        final User user = Json.decodeValue(routingContext.getBody(),User.class);
        String name = user.getName();
        String phone_number = user.getPhone_number();
        client.preparedQuery("INSERT INTO users (name, phone_number) VALUES (?, ?)", Tuple.of(name, phone_number), res -> {
            if (res.succeeded()) {
                routingContext.response().setStatusCode(200).end("User : "+name+" created");
            } else {
                System.out.println("Failure: " + res.cause().getMessage());
                client.close();
            }
        });
    }
}
