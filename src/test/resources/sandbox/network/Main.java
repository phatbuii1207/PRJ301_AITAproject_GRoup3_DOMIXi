import java.net.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        // Numeric IP avoids confusing DNS failure with actual outbound isolation.
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("1.1.1.1", 443), 1000);
            throw new AssertionError("Outbound TCP unexpectedly succeeded");
        } catch (IOException expected) {
            System.out.println("NETWORK_BLOCKED");
        }
        if (System.getenv("MSSQL_SA_PASSWORD") != null) throw new AssertionError("Host secret exposed");
    }
}
