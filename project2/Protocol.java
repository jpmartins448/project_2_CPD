package project2;

public class Protocol {
    // Client -> Server
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String TOKEN = "TOKEN";
    public static final String LIST_ROOMS = "LIST_ROOMS";
    public static final String JOIN = "JOIN";
    public static final String CREATE = "CREATE";
    public static final String CREATE_AI = "CREATE_AI";
    public static final String MSG = "MSG";
    public static final String LEAVE = "LEAVE";
    public static final String LOGOUT = "LOGOUT";
    public static final String PING = "PING";

    // Server -> Client
    public static final String OK = "OK";
    public static final String ERR = "ERR";
    public static final String TOKEN_RESP = "TOKEN";
    public static final String ROOMS = "ROOMS";
    public static final String DELIVER = "DELIVER";
    public static final String SYSTEM = "SYSTEM";

    // Helper for parsing protocol lines
    public static String[] splitArgs(String line) {
        return line.trim().split(" ", 2);
    }
}
