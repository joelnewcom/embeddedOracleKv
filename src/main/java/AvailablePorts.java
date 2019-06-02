import java.io.IOException;
import java.net.ServerSocket;

public class AvailablePorts {


    private int port;
    private int adminPortLow;
    private int adminPortHigh;

    public AvailablePorts() throws IOException {
        port = findRandomOpenPortOnAllLocalInterfaces(0);
        setPortRange(2);
    }

    private void setPortRange(int i) throws IOException {
        boolean found = true;
        while (found) {
            adminPortLow = findRandomOpenPortOnAllLocalInterfaces(0);
            try {
                adminPortHigh = findRandomOpenPortOnAllLocalInterfaces(adminPortLow + 1);
                found = false;
            } catch (IOException ex) {
                // next try
            }
        }
    }

    public int getPort() {
        return port;
    }

    public int getAdminPortLow() {
        return adminPortLow;
    }

    public int getAdminPortHigh() {
        return adminPortHigh;
    }

    private static Integer findRandomOpenPortOnAllLocalInterfaces(int port) throws IOException {
        try (ServerSocket socket = new ServerSocket(port)) {
            return socket.getLocalPort();
        }
    }

}
