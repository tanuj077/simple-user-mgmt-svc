package org.example.grpc.service;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.grpc.stubs.user.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    private final Map<String, UserResponse> userStore = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<UserResponse> responseObserver) {
        if (request.getName().isBlank() || request.getEmail().isBlank()) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Name and Email must not be empty")
                            .asRuntimeException()
            );
            return;
        }

        String id = String.valueOf(idCounter.getAndIncrement());

        UserResponse user = UserResponse.newBuilder()
                .setId(id)
                .setName(request.getName())
                .setEmail(request.getEmail())
                .build();

        userStore.put(id, user);
        responseObserver.onNext(user);
        responseObserver.onCompleted();
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        UserResponse user = userStore.get(request.getId());

        if (user != null) {
            responseObserver.onNext(user);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("User not found with id " + request.getId())
                            .asRuntimeException()
            );
        }
    }

    @Override
    public void listUsers(Empty request, StreamObserver<ListUserResponse> responseObserver) {
        if (userStore.isEmpty()) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("No users available")
                            .asRuntimeException()
            );
            return;
        }

        ListUserResponse users = ListUserResponse.newBuilder()
                .addAllUsers(userStore.values())
                .build();

        responseObserver.onNext(users);
        responseObserver.onCompleted();
    }
}
