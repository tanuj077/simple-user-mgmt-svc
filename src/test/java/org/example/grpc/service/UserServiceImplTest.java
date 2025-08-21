package org.example.grpc.service;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.example.grpc.stubs.user.CreateUserRequest;
import org.example.grpc.stubs.user.GetUserRequest;
import org.example.grpc.stubs.user.ListUserResponse;
import org.example.grpc.stubs.user.UserResponse;
import org.example.grpc.stubs.user.UserServiceGrpc;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserServiceImplTest {

    private static Server server;
    private static ManagedChannel channel;
    private static UserServiceGrpc.UserServiceBlockingStub stub;

    @BeforeClass
    public static void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new UserServiceImpl())
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();

        stub = UserServiceGrpc.newBlockingStub(channel);
    }

    @AfterClass
    public static void tearDown() {
        if (channel != null) channel.shutdownNow();
        if (server != null) server.shutdownNow();
    }

    @Test
    public void createUser_success_returnsPopulatedUser() {
        CreateUserRequest req = CreateUserRequest.newBuilder()
                .setName("Alice")
                .setEmail("alice@example.com")
                .build();

        UserResponse res = stub.createUser(req);

        assertNotNull("ID should be set", res.getId());
        assertFalse("ID should not be empty", res.getId().isEmpty());
        assertEquals("Alice", res.getName());
        assertEquals("alice@example.com", res.getEmail());
        // ID is numeric & positive (implementation uses AtomicInteger)
        assertTrue("ID should be numeric", res.getId().matches("\\d+"));
        assertTrue("ID should be >= 1", Integer.parseInt(res.getId()) >= 1);
    }

    @Test
    public void createUser_invalid_blankName_throwsInvalidArgument() {
        CreateUserRequest req = CreateUserRequest.newBuilder()
                .setName("")          // invalid
                .setEmail("x@y.z")
                .build();

        try {
            stub.createUser(req);
            fail("Expected INVALID_ARGUMENT");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
            assertTrue(e.getStatus().getDescription().contains("must not be empty"));
        }
    }

    @Test
    public void createUser_invalid_blankEmail_throwsInvalidArgument() {
        CreateUserRequest req = CreateUserRequest.newBuilder()
                .setName("Bob")
                .setEmail("")         // invalid
                .build();

        try {
            stub.createUser(req);
            fail("Expected INVALID_ARGUMENT");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.INVALID_ARGUMENT.getCode(), e.getStatus().getCode());
            assertTrue(e.getStatus().getDescription().contains("must not be empty"));
        }
    }

    @Test
    public void getUser_roundTrip_matchesCreatedUser() {
        // create
        UserResponse created = stub.createUser(CreateUserRequest.newBuilder()
                .setName("Carol")
                .setEmail("carol@example.com")
                .build());

        assertNotNull(created);
        assertFalse(created.getId().isEmpty());

        // get
        UserResponse fetched = stub.getUser(GetUserRequest.newBuilder()
                .setId(created.getId())
                .build());

        assertEquals(created.getId(), fetched.getId());
        assertEquals("Carol", fetched.getName());
        assertEquals("carol@example.com", fetched.getEmail());
    }

    @Test
    public void getUser_notFound_returnsNotFoundStatus() {
        String missingId = "999999";
        try {
            stub.getUser(GetUserRequest.newBuilder().setId(missingId).build());
            fail("Expected NOT_FOUND for id=" + missingId);
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            assertTrue(e.getStatus().getDescription().contains("User not found"));
        }
    }

    @Test
    public void listUsers_containsRecentlyCreatedUsers() {
        // create two distinct users and validate their returns
        UserResponse u1 = stub.createUser(CreateUserRequest.newBuilder()
                .setName("Dave")
                .setEmail("dave@example.com")
                .build());
        assertNotNull(u1);
        assertFalse(u1.getId().isEmpty());

        UserResponse u2 = stub.createUser(CreateUserRequest.newBuilder()
                .setName("Eve")
                .setEmail("eve@example.com")
                .build());
        assertNotNull(u2);
        assertFalse(u2.getId().isEmpty());

        // list and assert both present
        ListUserResponse list = stub.listUsers(Empty.getDefaultInstance());
        assertNotNull(list);
        assertFalse("List should not be empty", list.getUsersList().isEmpty());

        boolean foundU1 = list.getUsersList().stream().anyMatch(
                u -> u.getId().equals(u1.getId())
                        && "Dave".equals(u.getName())
                        && "dave@example.com".equals(u.getEmail()));
        boolean foundU2 = list.getUsersList().stream().anyMatch(
                u -> u.getId().equals(u2.getId())
                        && "Eve".equals(u.getName())
                        && "eve@example.com".equals(u.getEmail()));

        assertTrue("Created user u1 must be present in list", foundU1);
        assertTrue("Created user u2 must be present in list", foundU2);
    }

    @Test
    public void listUsers_whenEmpty_returnsNotFound() throws Exception {
        // Spin up a fresh service to guarantee empty store
        String freshName = InProcessServerBuilder.generateName();
        Server freshServer = InProcessServerBuilder.forName(freshName)
                .directExecutor()
                .addService(new UserServiceImpl())
                .build()
                .start();
        ManagedChannel freshChannel = InProcessChannelBuilder.forName(freshName)
                .directExecutor()
                .build();
        UserServiceGrpc.UserServiceBlockingStub freshStub = UserServiceGrpc.newBlockingStub(freshChannel);

        try {
            // Expect NOT_FOUND; no return to ignore here because it throws
            freshStub.listUsers(Empty.getDefaultInstance());
            fail("Expected NOT_FOUND when no users exist");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            assertTrue(e.getStatus().getDescription().contains("No users available"));
        } finally {
            freshChannel.shutdownNow();
            freshServer.shutdownNow();
        }
    }

    @Test
    public void createUser_idsMonotonicIncreasing_forSequentialCalls() {
        UserResponse a = stub.createUser(CreateUserRequest.newBuilder()
                .setName("Foo")
                .setEmail("foo@example.com")
                .build());
        UserResponse b = stub.createUser(CreateUserRequest.newBuilder()
                .setName("Bar")
                .setEmail("bar@example.com")
                .build());

        assertNotNull(a.getId());
        assertNotNull(b.getId());
        assertTrue(a.getId().matches("\\d+"));
        assertTrue(b.getId().matches("\\d+"));

        int aId = Integer.parseInt(a.getId());
        int bId = Integer.parseInt(b.getId());
        assertTrue("IDs should increase monotonically in this in-process test", bId >= aId);
    }
}
