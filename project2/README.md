# Distributed Chat System (CPD 2025/2026)

Java SE 21 TCP client-server chat system for the CPD 2025/2026 assignment.

The system supports user registration, authentication, token-based session recovery, multiple chat rooms, AI-assisted rooms backed by a local Ollama model, and concurrent clients handled with Java virtual threads.

## Requirements

- Java SE 21 or higher
- Ollama installed locally, only required for AI rooms
- Ollama model available locally, default: `llama3.2:3b`

The AI client expects Ollama at:

```text
http://localhost:11434
```

To prepare the default model:

```bash
ollama pull llama3.2:3b
ollama run llama3.2:3b
```

## Project Structure

```text
.
├── Server.java              # TCP server entry point
├── Client.java              # Console client entry point
├── ClientHandler.java       # Per-client protocol handler
├── Protocol.java            # Command/response constants
├── UserStore.java           # User registration and password storage
├── Session.java             # Authenticated user session state
├── SessionManager.java      # Token and session lifecycle management
├── Room.java                # Normal chat room
├── AIRoom.java              # AI room with explicit PROMPT support
├── RoomManager.java         # Room registry and cleanup
├── Message.java             # Timeline message model
├── ClientConnection.java    # Outbound message pipeline
├── SimpleMessageQueue.java  # Lock-based bounded queue
└── LLMClient.java           # Ollama HTTP API client
```

## Build

Compile all sources from the project directory:

```bash
javac *.java
```

## Run

Start the server on the default port, `12345`:

```bash
java Server
```

Or choose a port:

```bash
java Server 12346
```

Start a client connected to `localhost:12345`:

```bash
java Client
```

Or choose host and port:

```bash
java Client localhost 12346
```

## Client Commands

### Authentication

```text
REGISTER <user> <password>
LOGIN <user> <password>
TOKEN <token>
LOGOUT
```

After a successful login, the server returns a session token. The client stores it in `.session_token` and uses it automatically after reconnecting.

### Rooms

```text
LIST_ROOMS
CREATE <room>
JOIN <room>
LEAVE
```

Room names may contain spaces in `CREATE` and `JOIN` because the command uses the rest of the line as the room name.

### AI Rooms

```text
CREATE_AI <room> | <system prompt>
PROMPT <question or instruction>
```

AI rooms use normal chat messages for conversation and an explicit `PROMPT` command for model interaction.

Example:

```text
CREATE_AI planning room | summarize availability and suggest meeting times
JOIN planning room
MSG I can meet Monday after 14:00
MSG Wednesday morning also works
PROMPT summarize this conversation
```

In an AI room:

- `MSG <text>` sends a normal message to the room.
- `PROMPT <text>` sends the current room conversation plus the AI room system prompt to Ollama.
- The model answer is posted back to the room as `Bot`.

### Messaging

```text
MSG <text>
```

### Utility

```text
HELP
PING
```

## Protocol Output

The server sends room messages in this form:

```text
DELIVER|<room>|<author>|<timestamp>|<text>
SYSTEM|<room>|<text>
```

The provided client formats these responses for display.

## Concurrency Design

The server uses Java virtual threads to reduce thread overhead while keeping the code in a simple blocking I/O style. TCP reads, TCP writes, and Ollama HTTP calls may block, but blocking a virtual thread is cheap compared with blocking a platform thread.

Virtual threads are used in these places:

- The server starts one virtual thread per accepted TCP client connection. This thread runs `ClientHandler` and reads commands from that client.
- Each `ClientConnection` starts one virtual sender thread. Room broadcasts only enqueue outbound messages, and this sender thread writes them to the socket. This avoids one slow client blocking the room broadcast path.
- The server starts one virtual cleanup thread that periodically removes expired sessions.
- Each `AIRoom` starts one virtual prompt worker. Prompts in the same AI room are processed sequentially, so bot replies are written to the room in prompt order.

In practice, a connected user normally has two virtual threads associated with the server side of the connection: one `ClientHandler` reader thread and one `ClientConnection` sender thread. If the user disconnects and reconnects, the old connection is unbound from the session and the new connection gets new virtual threads while keeping the same authenticated session.

Shared state is protected with `ReentrantLock` and `Condition`:

- `Room` protects room members and timelines.
- `RoomManager` protects the room map.
- `SessionManager` protects token and username session maps.
- `UserStore` protects registered users and file writes.
- `SimpleMessageQueue` implements a bounded lock-based queue.

The implementation does not use thread-safe Java collection implementations from `java.util.concurrent`.

## Fault Tolerance

The client automatically reconnects when the TCP connection is lost. It does not cache the username/password. Instead, it reuses the session token received from the server.

On reconnect:

- The server binds the new TCP connection to the previous session.
- If the user was in a room, the session resumes that room without a new join event.
- The server replays only messages missed since the last successful delivery.

Sessions expire after 24 hours. Expired sessions are removed from the session manager and from room memberships.

## Message History

Each room keeps a bounded in-memory timeline of the latest 500 messages. Messages have increasing IDs so reconnecting sessions can receive only missed messages instead of the full room timeline.

## User Database

Registered users are stored in:

```text
users.db
```

Passwords are salted and hashed with SHA-256 before being stored.

## Notes

Generated `.class` files, `.session_token`, and `users.db` are runtime/build artifacts and should normally not be committed unless explicitly required by the submission rules.
