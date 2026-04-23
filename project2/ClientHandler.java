package project2;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final UserStore userStore;
    private final SessionManager sessionManager;
    private final RoomManager roomManager;

    public ClientHandler(Socket socket, UserStore userStore, SessionManager sessionManager, RoomManager roomManager) {
        this.socket = socket;
        this.userStore = userStore;
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            ClientConnection conn = new ClientConnection(socket, out);
            Session session = null;
            Room currentRoom = null;
            boolean authenticated = false;
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                String[] parts = Protocol.splitArgs(line);
                String cmd = parts[0];
                String arg = parts.length > 1 ? parts[1] : "";

                if (!authenticated) {
                    if (cmd.equals(Protocol.REGISTER)) {
                        String[] creds = arg.split(" ", 2);
                        if (creds.length < 2) { out.println(Protocol.ERR + " Invalid REGISTER"); continue; }
                        if (userStore.register(creds[0], creds[1])) {
                            out.println(Protocol.OK + " Registered");
                        } else {
                            out.println(Protocol.ERR + " Username exists");
                        }
                    } else if (cmd.equals(Protocol.LOGIN)) {
                        String[] creds = arg.split(" ", 2);
                        if (creds.length < 2) { out.println(Protocol.ERR + " Invalid LOGIN"); continue; }
                        if (userStore.authenticate(creds[0], creds[1])) {
                            session = sessionManager.createSession(creds[0]);
                            session.bindConnection(conn);
                            out.println(Protocol.TOKEN_RESP + " " + session.getToken());
                            authenticated = true;
                        } else {
                            out.println(Protocol.ERR + " Invalid credentials");
                        }
                    } else if (cmd.equals(Protocol.TOKEN)) {
                        Session s = sessionManager.getSessionByToken(arg.trim());
                        if (s != null) {
                            session = s;
                            session.bindConnection(conn);
                            out.println(Protocol.OK + " Token accepted");
                            authenticated = true;
                        } else {
                            out.println(Protocol.ERR + " Invalid token");
                        }
                    } else {
                        out.println(Protocol.ERR + " Authenticate first");
                    }
                    continue;
                }

                // Authenticated commands
                switch (cmd) {
                    case Protocol.LIST_ROOMS -> {
                        out.println(Protocol.ROOMS + " " + String.join(",", roomManager.listRooms()));
                    }
                    case Protocol.CREATE -> {
                        Room room = roomManager.getOrCreateRoom(arg.trim());
                        out.println(Protocol.OK + " Room created");
                    }
                    case Protocol.JOIN -> {
                        Room room = roomManager.getRoom(arg.trim());
                        if (room == null) {
                            out.println(Protocol.ERR + " No such room");
                        } else {
                            if (currentRoom != null) currentRoom.leave(session);
                            room.join(session);
                            session.setCurrentRoom(room.getName());
                            currentRoom = room;
                            out.println(Protocol.OK + " Joined " + room.getName());
                            // Send timeline
                            for (Message m : room.getTimeline()) {
                                session.deliver(m, room.getName());
                            }
                        }
                    }
                    case Protocol.MSG -> {
                        if (currentRoom == null) {
                            out.println(Protocol.ERR + " Not in a room");
                        } else {
                            Message msg = new Message(session.getUsername(), arg, Message.Type.USER);
                            currentRoom.postMessage(msg);
                        }
                    }
                    case Protocol.LEAVE -> {
                        if (currentRoom != null) {
                            currentRoom.leave(session);
                            session.setCurrentRoom(null);
                            currentRoom = null;
                            out.println(Protocol.OK + " Left room");
                        } else {
                            out.println(Protocol.ERR + " Not in a room");
                        }
                    }
                    case Protocol.LOGOUT -> {
                        sessionManager.expireSession(session.getToken());
                        out.println(Protocol.OK + " Logged out");
                        return;
                    }
                    case Protocol.PING -> out.println(Protocol.OK + " pong");
                    default -> out.println(Protocol.ERR + " Unknown command");
                }
            }
        } catch (Exception e) {
            // Ignore, client disconnected
        }
    }
}
