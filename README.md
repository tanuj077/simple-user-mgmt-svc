
# gRPC User Service (Java)

A simple Java-based gRPC service demonstrating client–server communication with Protocol Buffers.  
Supports **Create User**, **Get User by ID**, and **List Users** with proper gRPC status codes.
---

## 📂 Project Structure

```text
simple-user-mgmt-svc/
│
├─ src/main/proto/
│  └─ user_service.proto
│
├─ src/main/java/org/example/grpc/
│  ├─ server/
│  │  └─ UserServer.java          # boots the gRPC server (port 50051)
│  ├─ service/
│  │  └─ UserServiceImpl.java     # in-memory implementation of RPCs
│  └─ client/
│     └─ UserClient.java          # sample blocking client
│
├─ pom.xml
└─ README.md
````

> Note: gRPC/Protobuf stubs are generated under `target/generated-sources/protobuf` during build and are **not** checked into source control.

---

## 🚀 Build & Run

### 1) Build + generate stubs

```bash
mvn clean install
```

### 2) Run the server

* **From IDE:** run `org.example.grpc.server.UserServer.main`
* **CLI (if you have maven-exec-plugin configured):**

```bash
mvn exec:java -Dexec.mainClass=org.example.grpc.server.UserServer
```

### 3) Run the client

* **From IDE:** run `org.example.grpc.client.UserClient.main`
* **CLI (if you have maven-exec-plugin configured):**

```bash
mvn exec:java -Dexec.mainClass=org.example.grpc.client.UserClient
```

---

## 🛠 Features

* **CreateUser** — adds a user (name, email)
* **GetUser** — fetches a user by ID
* **ListUsers** — returns all users
* Uses proper gRPC `Status` codes (e.g., `INVALID_ARGUMENT`, `NOT_FOUND`)

---

## 📊 Sample Output

```text
INFO: User created by ID: 1
INFO: User created by ID: 2
INFO: Users List: [id: "1", name: "Alice", email: "alice@example.com",
                   id: "2", name: "Bob",   email: "bob@example.com"]
INFO: User Retrieved: Alice
SEVERE: Failed with status: NOT_FOUND - User not found with id 10
```

---

## 🖼 Architecture 

```text
+-------------------+      gRPC (Protobuf)      +------------------+
|    UserClient     |  <--------------------->  |    UserServer    |
| (blocking stub)   |                           |  (port 50051)    |
+-------------------+                           +------------------+
          |                                                |
          |  CreateUser / GetUser / ListUsers              |
          v                                                v
                                   org.example.grpc.service.UserServiceImpl
```

---

## Notes

* Server listens on **localhost:50051**.
* `.proto` lives in `src/main/proto/user_service.proto`.
* Generated sources are under `target/generated-sources/protobuf` after build.

