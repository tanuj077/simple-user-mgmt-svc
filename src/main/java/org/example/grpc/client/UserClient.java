package org.example.grpc.client;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.example.grpc.stubs.user.*;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserClient {
    private static final Logger logger = Logger.getLogger(UserClient.class.getName());
    private final ManagedChannel channel;
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;

    public UserClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void createUser(String name, String email) {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName(name)
                .setEmail(email)
                .build();

        UserResponse response = blockingStub.createUser(request);
        logger.log(Level.INFO, "User created by ID: "+ response.getId());
    }

    public void getUser(String id) {
        GetUserRequest request = GetUserRequest.newBuilder()
                .setId(id)
                .build();

        try {
            UserResponse response = blockingStub.getUser(request);
            logger.log(Level.INFO, "User Retrieved: " + response.getName());
        } catch (StatusRuntimeException e) {
            logger.log(Level.SEVERE, "Failed with status: " + e.getStatus()
                    + " - " + e.getStatus().getDescription());
        }
    }

    public void listUsers() {
        ListUserResponse users = blockingStub.listUsers(Empty.getDefaultInstance());
        logger.log(Level.INFO, "Users List: " + users.getUsersList());
    }

    public static void main(String[] args) throws InterruptedException {
        UserClient client = new UserClient("localhost", 50051);

        // 1. Create a couple of users
        client.createUser("Alice", "alice@example.com");
        client.createUser("Bob", "bob@example.com");

        // 2. List users
        client.listUsers();

        // 3. Fetch user by id
        client.getUser("1");  // ID starts from 1 in your impl

        client.getUser("10");  // ID will throw error
        client.shutdown();
    }
}
