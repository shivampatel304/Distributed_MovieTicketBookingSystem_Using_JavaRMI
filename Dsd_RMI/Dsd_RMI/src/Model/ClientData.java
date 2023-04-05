package Model;

/**
 * @author shivam Patel (40226428)
 * @version 3.0
 */

public class ClientData {
    private String clientType;
    private String clientID;
    private String clientServer;

    public ClientData(String clientID) {
        this.clientID = clientID;
        this.clientType = detectClientType();
        this.clientServer = detectClientServer();
    }

    private String detectClientServer() {
        if (clientID.substring(0, 3).equalsIgnoreCase("ATW")) {
            return "Atwater";
        } else if (clientID.substring(0, 3).equalsIgnoreCase("VER")) {
            return "Verdun";
        } else {
            return "Outremont";
        }
    }

    private String detectClientType() {
        if (clientID.charAt(4) == 'A') {
            return "Admin";
        } else {
            return "Customer";
        }
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientServer() {
        return clientServer;
    }

    public void setClientServer(String clientServer) {
        this.clientServer = clientServer;
    }

    public String toString() {
        return getClientType() + "(" + getClientID() + ") on " + getClientServer() + " Server.";
    }
}
