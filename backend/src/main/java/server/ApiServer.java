package server;

import com.sun.net.httpserver.HttpServer;
import server.handlers.LoadHandler;
import server.handlers.TrainHandler;
import server.handlers.StatusHandler;

import java.net.InetSocketAddress;
import java.util.List;
import model.UserRecord;

public class ApiServer {

    // dosya yuklenince handlerlar buradan datayi aliyor
    public static volatile List<UserRecord> currentData = null;

    public static void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/load", new LoadHandler());
        server.createContext("/api/train", new TrainHandler());
        server.createContext("/api/status", new StatusHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println("  ML API Server running on http://localhost:8080");
        System.out.println("  Endpoints: /api/load, /api/train, /api/status");
        System.out.println("=================================================");
    }
}
