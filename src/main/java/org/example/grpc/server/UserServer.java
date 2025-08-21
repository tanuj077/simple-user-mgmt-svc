package org.example.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.example.grpc.service.UserServiceImpl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserServer {
    private static final Logger logger = Logger.getLogger(UserServer.class.getName());
    private static final int PORT = 50051;
    private Server server;

    public void startServer() {
        try {
            server = ServerBuilder.forPort(PORT)
                    .addService(new UserServiceImpl())
                    .build()
                    .start();
            logger.log(Level.INFO, "gRPC User Server started at port " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.log(Level.INFO, "Clean Server Shutdown, in case JVM was shutdown!!!");
                try {
                    UserServer.this.stopServer();
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "Server Shutdown Interrupted", e.getMessage());
                }
            }));

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to start server: {0}", e.getMessage());
        }
    }

    public void stopServer() throws InterruptedException {
        if(server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void blockUntilSHutDown() throws InterruptedException {
        if(server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        UserServer server = new UserServer();
        server.startServer();
        server.blockUntilSHutDown();
    }
}
