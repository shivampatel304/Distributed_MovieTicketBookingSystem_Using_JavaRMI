package Server;

import Client.Client;
import ServerImplementation.ServerImpl;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Logger.Logger;

/**
 * @author shivam Patel (40226428)
 * @version 3.0
 */

public class ServerInstance {

    private String serverID;
    private String serverName;
    private int serverRegistryPort;
    private int serverUdpPort;

    public ServerInstance(String serverID) throws Exception{
        this.serverID = serverID;
        switch(serverID){
            case "ATW":
                serverName = "Atwater";
                serverRegistryPort = Client.SERVER_ATWATER;
                serverUdpPort = ServerImpl.Atwater_Server_Port;
                break;
            case "OUT":
                serverName = "Outremont";
                serverRegistryPort = Client.SERVER_OUTREMONT;
                serverUdpPort = ServerImpl.Outremont_Server_Port;
                break;
            case "VER":
                serverName = "Verdun";
                serverRegistryPort = Client.SERVER_VERDUN;
                serverUdpPort = ServerImpl.Verdun_Server_Port;
                break;
        }

        ServerImpl remoteObject = new ServerImpl(serverID,serverName);
        Registry registry = LocateRegistry.createRegistry(serverRegistryPort);
        registry.bind("DMTBS",remoteObject);
        System.out.println(serverName + "Server is up and running properly. ");
        Logger.serverLog(serverID, " Server is Up & Running");

        Runnable task = () -> {
            listenForRequest(remoteObject, serverUdpPort, serverName, serverID);
        };

        Thread thread = new Thread(task);
        thread.start();
    }

    private void listenForRequest(ServerImpl remoteObject, int serverUdpPort, String serverName, String serverID) {
        DatagramSocket aSocket = null;
        String sendingResult ="";
        try{
            aSocket = new DatagramSocket(serverUdpPort);
            byte[] buffer =new byte[1000];
            System.out.println(serverName + " UDP Server Started at port " + aSocket.getLocalPort() + " ............");
            Logger.serverLog(serverID, " UDP Server Started at port " + aSocket.getLocalPort());
            while(true){
                DatagramPacket request = new DatagramPacket(buffer,buffer.length);
                aSocket.receive(request);
                String sentence = new String(request.getData(),0,request.getLength());
                String[] parts = sentence.split(";");
                String method = parts[0];
                String customerID = parts[1];
                String movieName = parts[2];
                String movieID = parts[3];
                int numberOfTickets = Integer.parseInt(parts[4]);
                if(method.equalsIgnoreCase("bookMovie")){
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " eventType: " + movieName + " ", " ...");
                    String result = remoteObject.bookMovieTickets(customerID,movieID,movieName,numberOfTickets);
                    sendingResult = result + ";";
                }else if(method.equalsIgnoreCase("listMovieAvailability")){
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieType: " + movieName + " ", " ...");
                    String result = remoteObject.listMovieShowsAvailabilityUDP(movieName);
                    sendingResult = result + ";";
                }else if(method.equalsIgnoreCase(("removeMovie"))){
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieType: " + movieName + " ", " ...");
                    String result = remoteObject.removeMovieSlots(movieID,movieName);
                    sendingResult = result + ";";
                }else if(method.equalsIgnoreCase("cancelMovie")){
                    Logger.serverLog(serverID, customerID, " UDP request received " + method + " ", " movieID: " + movieID + " movieType: " + movieName + " ", " ...");
                    String result = remoteObject.cancelMovieTickets(customerID,movieID,movieName,numberOfTickets);
                    sendingResult = result + ";";
                }
                byte[] sendData =sendingResult.getBytes();
                DatagramPacket reply = new DatagramPacket(sendData,sendingResult.length(),request.getAddress(),
                        request.getPort());
                aSocket.send(reply);
                Logger.serverLog(serverID, customerID, " UDP reply sent " + method + " ", " movieID: " + movieID + " movieType: " + movieName + " ", sendingResult);
            }

        }catch (SocketException e) {
            System.out.println("SocketException: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }finally {
            if(aSocket != null){
                aSocket.close();
            }
        }
    }

}
