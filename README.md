# CPD Projects

CPD Projects of group T02G11.

Group members:

1. Carolina Roque (up202305062@up.pt)
2. João Martins   (up202207341@up.pt)
3. Filipe Paiva   (up202304284@up.pt)


# Assignment 1

See report of part 1 of the first project in project1/assign1/doc/Part_1_Report.pdf

See report of part 2 of the first project in project1/assign2/doc/Part_2_Report.pdf


Commands used:
<!-- perf stat -e \
cycles,instructions,\
cache-references,cache-misses,\
L1-dcache-loads,L1-dcache-load-misses,\
LLC-loads,LLC-load-misses,\
stalled-cycles-backend \
./a.out -->

# Assignment 2

# Distributed Chat System (CPD 2025/2026)

## Overview

This project implements a **distributed client-server chat system in Java (TCP)** for the CPD course (2025/2026).

It supports:
- User registration and authentication
- Token-based session management
- Chat rooms with real-time messaging
- AI-powered chat rooms (Ollama integration)
- Fault-tolerant reconnection
- Concurrency control using locks (no concurrent collections)

---

## Requirements

- Java SE 21 or higher
- Ollama installed locally (for AI rooms)
- Model available (e.g. `llama3.2:3b`)
- Network access to `localhost:11434`

---

## Project Structure 

├── Server.java

├── Client.java

├── ClientHandler.java

├── Room.java

├── AIRoom.java

├── RoomManager.java

├── Session.java

├── SessionManager.java

├── ClientConnection.java

├── SimpleMessageQueue.java

├── UserStore.java

├── LLMClient.java

├── Message.java

├── Protocol.java

## Compilation

- javac *.java

### Start Server

- java Server


### Start Client
- java Client

## Features
- TCP chat system
- Authentication (username/password)
- Session tokens (persistent login)
- Chat rooms
- AI rooms (Ollama integration)
- Message timeline
- Reconnection support (fault tolerance)
- Virtual threads for scalability
- AI setup (required)

### Install Ollama and run:

ollama run llama3.2

### Server expects Ollama at:

http://localhost:11434


## Client commands:

### Authentication
- REGISTER <user> <pass>
- LOGIN <user> <pass> 
- TOKEN <token> 
- LOGOUT

### Room Management
- LIST_ROOMS
- CREATE <room>
- CREATE_AI <room> <prompt>
- JOIN <room>
- LEAVE

### Messaging
- MSG <text>

### Utility
- HELP
- PING

## Architecture

### Main components:

- Server -> accepts TCP connections
- ClientHandler -> processes commands
- Room / AIRoom -> chat logic
- RoomManager -> manages rooms
- SessionManager -> authentication and tokens
- UserStore -> persistent user database
- ClientConnection -> outbound message pipeline
- LLMClient -> interface with Ollama API
