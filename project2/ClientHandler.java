import java.io.*;
import java.net.Socket;
import java.util.List;

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
        Session session = null;
        Room currentRoom = null;
        ClientConnection conn = null;

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            conn = new ClientConnection(socket, out);
            boolean authenticated = false;

            while (true) {
                String line = in.readLine();
                if (line == null) break;

                String[] parts = Protocol.splitArgs(line);
                String cmd = parts[0].toUpperCase();
                String arg = parts.length > 1 ? parts[1] : "";

                if (!authenticated) {
                    if (cmd.equals("HELP")) {
                        out.println(Protocol.OK + " Comandos disponíveis: REGISTER <user> <pass>, LOGIN <user> <pass>, TOKEN <token>, LIST_ROOMS, CREATE <room>, CREATE_AI <room> | <prompt>, JOIN <room>, MSG <text>, PROMPT <text>, LEAVE, LOGOUT, HELP");
                    } else if (cmd.equals(Protocol.REGISTER)) {
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
                            currentRoom = resumePreviousRoom(session, out, false);
                            authenticated = true;
                        } else {
                            out.println(Protocol.ERR + " Invalid credentials");
                        }
                    } else if (cmd.equals(Protocol.TOKEN)) {
                        Session s = sessionManager.getSessionByToken(arg.trim());
                        if (s != null) {
                            session = s;
                            session.bindConnection(conn);
                            currentRoom = resumePreviousRoom(session, out, true);
                            authenticated = true;
                        } else {
                            out.println(Protocol.ERR + " Invalid token");
                        }
                    } else {
                        out.println(Protocol.ERR + " Authenticate first");
                    }
                    continue;
                }

                switch (cmd) {
                    case "HELP" -> out.println(Protocol.OK + " Comandos disponíveis: LIST_ROOMS, CREATE <room>, CREATE_AI <room> | <prompt>, JOIN <room>, MSG <text>, PROMPT <text>, LEAVE, LOGOUT, HELP");
                    case Protocol.LIST_ROOMS -> out.println(Protocol.ROOMS + " " + String.join(",", roomManager.listRooms()));
                    case Protocol.CREATE -> {
                        if (arg.isBlank()) {
                            out.println(Protocol.ERR + " Usage: CREATE <room>");
                            break;
                        }
                        String roomName = arg.trim();
                        roomManager.getOrCreateRoom(roomName);
                        out.println(Protocol.OK + " Room ready: " + roomName);
                    }
                    case Protocol.CREATE_AI -> {
                        String[] aiParts = parseAIRoomArgs(arg);
                        if (aiParts == null) {
                            out.println(Protocol.ERR + " Usage: CREATE_AI <room> | <prompt>");
                            break;
                        }

                        String roomName = aiParts[0];
                        String prompt = aiParts[1];
                        Room newRoom = roomManager.createAIRoom(roomName, prompt);

                        if (newRoom instanceof AIRoom) {
                            out.println(Protocol.OK + " AI Room ready: " + roomName);
                        } else {
                            out.println(Protocol.OK + " Room already existed");
                        }
                    }
                    case Protocol.JOIN -> {
                        String roomName = arg.trim();
                        Room room = roomManager.getRoom(roomName);
                        if (room == null) {
                            out.println(Protocol.ERR + " No such room");
                            break;
                        }

                        if (currentRoom == room) {
                            out.println(Protocol.OK + " Already in " + room.getName());
                            break;
                        }

                        if (currentRoom != null) {
                            currentRoom.leave(session);
                        }

                        long lastSeen = session.getLastSeenMessageId(room.getName());
                        List<Message> missedMessages = room.getMessagesAfter(lastSeen);
                        room.join(session);
                        session.setCurrentRoom(room.getName());
                        currentRoom = room;
                        out.println(Protocol.OK + " Joined " + room.getName());
                        deliverHistory(session, room, missedMessages);
                    }
                    case Protocol.MSG -> {
                        if (currentRoom == null) {
                            out.println(Protocol.ERR + " Not in a room");
                        } else if (arg.isBlank()) {
                            out.println(Protocol.ERR + " Usage: MSG <text>");
                        } else {
                            currentRoom.postMessage(new Message(session.getUsername(), arg, Message.Type.USER));
                        }
                    }
                    case Protocol.PROMPT -> {
                        if (currentRoom == null) {
                            out.println(Protocol.ERR + " Not in a room");
                        } else if (!(currentRoom instanceof AIRoom aiRoom)) {
                            out.println(Protocol.ERR + " PROMPT is only available in AI rooms");
                        } else if (arg.isBlank()) {
                            out.println(Protocol.ERR + " Usage: PROMPT <text>");
                        } else {
                            aiRoom.prompt(session.getUsername(), arg);
                            out.println(Protocol.OK + " Prompt sent");
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
                        if (currentRoom != null) {
                            currentRoom.leave(session);
                            session.setCurrentRoom(null);
                        }
                        sessionManager.expireSession(session.getToken());
                        out.println(Protocol.OK + " Logged out");
                        return;
                    }
                    case Protocol.PING -> out.println(Protocol.OK + " pong");
                    default -> out.println(Protocol.ERR + " Unknown command");
                }
            }
        } catch (Exception e) {
            // Client disconnected. The session is kept so TOKEN reconnection can resume it.
        } finally {
            if (session != null && conn != null) {
                session.unbindConnection(conn);
            }
        }
    }

    private Room resumePreviousRoom(Session session, PrintWriter out, boolean announceNoRoom) {
        String prevRoom = session.getCurrentRoom();
        if (prevRoom == null) {
            if (announceNoRoom) {
                out.println(Protocol.OK + " Session resumed, not in any room");
            }
            return null;
        }

        Room room = roomManager.getRoom(prevRoom);
        if (room == null) {
            session.setCurrentRoom(null);
            out.println(Protocol.OK + " Session resumed, but previous room not found");
            return null;
        }

        long lastSeen = session.getLastSeenMessageId(room.getName());
        room.resume(session);
        out.println(Protocol.OK + " Session resumed in room " + room.getName());
        deliverHistory(session, room, room.getMessagesAfter(lastSeen));
        return room;
    }

    private void deliverHistory(Session session, Room room, List<Message> messages) {
        for (Message message : messages) {
            session.deliverHistory(message, room.getName());
        }
    }

    private String[] parseAIRoomArgs(String arg) {
        int separator = arg.indexOf('|');
        if (separator < 0) return null;

        String roomName = arg.substring(0, separator).trim();
        String prompt = arg.substring(separator + 1).trim();
        if (roomName.isEmpty() || prompt.isEmpty()) return null;

        return new String[] { roomName, prompt };
    }
}
