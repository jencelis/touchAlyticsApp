To run this app all importing issues must first be settled.

After that to connect it to the server you will need to change or verify three constants in Constants.java.

    public static final String SERVER_BASE_URL = "10.128.6.180";

    public static final int AUTH_SERVER_PORT = 5000; // Flask app port

    public static final int SERVER_PORT = 7000; // Server db port

Make SERVER_BASE_URL match your ip on the server side, and make sure the two ports match the ports on the server side aswell.
