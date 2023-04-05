package ServerImplementation;

import Client.Client;
import Interface.RMI_Interface;
import Model.ClientData;
import Model.MovieData;

import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import Logger.Logger;

/**
 * @author shivam Patel (40226428)
 * @version 3.0
 */

public class ServerImpl extends UnicastRemoteObject implements RMI_Interface {



    public static final int Atwater_Server_Port = 8888;
    public static final int Verdun_Server_Port = 7777;
    public static final int Outremont_Server_Port = 6666;

    // HashMap<movieName, HashMap <movieID, MovieData>>
    private Map<String, Map<String, MovieData>> data;

    // HashMap<CustomerID, HashMap <movieName, Map<MovieID,numOfTickets>>>
    private Map<String, Map<String, Map<String,Integer>>> clientEvents;

    // HashMap<ClientID, Client>
    private Map<String, ClientData> serverClients;



    private String serverID;
    private String serverName;

    public ServerImpl(String serverID, String serverName) throws RemoteException{
        this.serverID = serverID;
        this.serverName = serverName;
        data = new ConcurrentHashMap<>();
        data.put("Avatar",new ConcurrentHashMap<>());
        data.put("Avenger",new ConcurrentHashMap<>());
        data.put("Titanic",new ConcurrentHashMap<>());
        clientEvents = new ConcurrentHashMap<>();
        serverClients = new ConcurrentHashMap<>();
    }

    private static int getServerPort(String branchAcronym) {
        if (branchAcronym.equalsIgnoreCase("ATW")) {
            return Atwater_Server_Port;
        } else if (branchAcronym.equalsIgnoreCase("VER")) {
            return Verdun_Server_Port;
        } else if (branchAcronym.equalsIgnoreCase("OUT")) {
            return Outremont_Server_Port;
        }
        return 1;
    }
    @Override
    public String addMovieSlots(String movieID, String movieName, int bookingCapacity) throws RemoteException {
        String response;
        if(data.get(movieName).containsKey(movieID)){
            if(data.get(movieName).get(movieID).getMovieCapacity() < bookingCapacity) {
                data.get(movieName).get(movieID).setMovieCapacity(bookingCapacity);
                response = "Success: Movie " + movieID + " Capacity increased to " + bookingCapacity;
                try{
                    Logger.serverLog(serverID, "null", " RMI addMovie ", " movieID: " + movieID + " movieType: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                }
                catch (Exception e)
                {
                    System.out.println(e);
                }
                return response;
            }else{
                response = "Failed: Movie Already Exists, Cannot Decrease Booking Capacity";
                try {
                    Logger.serverLog(serverID, "null", " RMI addMovie ", " movieID: " + movieID + " movieType: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
        if(MovieData.detectMovieServer(movieID).equals(serverName)){
            MovieData newData = new MovieData(movieID,movieName,bookingCapacity);
            Map<String, MovieData> movieHashMap = data.get(movieName);
            movieHashMap.put(movieID,newData);
            data.put(movieID,movieHashMap);
            response = "Success: Movie " + movieID + " added successfully";
            try {
                Logger.serverLog(serverID, "null", " RMI addMovie ", " movieID: " + movieID + " movieType: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }else{
            response = "Failed: Cannot Add Movie to servers other than " + serverName;
            try {
                Logger.serverLog(serverID, "null", " RMI addMovie ", " movieID: " + movieID + " movieType: " + movieName + " bookingCapacity " + bookingCapacity + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    public String removeMovieSlots(String movieID,String movieName) throws RemoteException{
        String response;
        if(MovieData.detectMovieServer(movieID).equals(serverName)){
            if(data.get(movieName).containsKey(movieID)){
                Map<String,Integer> registeredCustomer = data.get(movieName).get(movieID).getRegisteredCustomer();
                data.get(movieName).remove(movieID);
                addCustomerToNextSameMovie(movieID,movieName,registeredCustomer);
                response = "Success: Movie Removed Successfully";
                try {
                    Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }else{
                response = "Failed: Movie " + movieID + " Does Not Exist";
                try {
                    Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }else{
            response = "Failed: Cannot Remove Movie from servers other than " + serverName;
            try {
                Logger.serverLog(serverID, "null", " RMI removeMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
    }

    private void addCustomerToNextSameMovie(String oldEventID, String movieName, Map<String,Integer> registeredCustomer) {
        for(String customerID : registeredCustomer.keySet()){
            if(customerID.substring(0,3).equals(serverID)){
                clientEvents.get(customerID).get(movieName).remove(oldEventID);
                String nextSameEventResult = getNextSameEvent(data.get(movieName).keySet(),movieName,oldEventID, registeredCustomer.get(customerID));
                if(nextSameEventResult.equals("Failed")){
                    return;
                }else{
                    bookMovieTickets(customerID,nextSameEventResult,movieName,registeredCustomer.get(customerID));
                }
            }else{
                sendUDPMessage(getServerPort(customerID.substring(0, 3)), "removeMovie", customerID, movieName, oldEventID,registeredCustomer.get(customerID));
            }
        }
    }

    private String getNextSameEvent(Set<String> keySet, String movieName, String oldMovieID,int numOfTickets){
        List<String> sortedIDs = new ArrayList<String>(keySet);
        sortedIDs.add(oldMovieID);
        Collections.sort(sortedIDs, new Comparator<String>(){
            public int compare(String id1, String id2){
                Integer timeSlot1 = 0;
                switch(id1.substring(3,4).toUpperCase()){
                    case "M":
                        timeSlot1 = 1;
                        break;
                    case "A":
                        timeSlot1 = 2;
                        break;
                    case "E":
                        timeSlot1 = 3;
                        break;
                }
                Integer timeSlot2 = 0;
                switch (id2.substring(3, 4).toUpperCase()) {
                    case "M":
                        timeSlot2 = 1;
                        break;
                    case "A":
                        timeSlot2 = 2;
                        break;
                    case "E":
                        timeSlot2 = 3;
                        break;
                }
                Integer date1 = Integer.parseInt(id1.substring(8, 10) + id1.substring(6, 8) + id1.substring(4, 6));
                Integer date2 = Integer.parseInt(id2.substring(8, 10) + id2.substring(6, 8) + id2.substring(4, 6));
                int dateCompare = date1.compareTo(date2);
                int timeslotCompare = timeSlot1.compareTo(timeSlot2);
                if(dateCompare == 0){
                    return ((timeslotCompare == 0) ? dateCompare : timeslotCompare);
                }else{
                    return dateCompare;
                }
            }
        });
        int index = sortedIDs.indexOf(oldMovieID) + 1;
        for (int i = index; i < sortedIDs.size(); i++) {
            if (!data.get(movieName).get(sortedIDs.get(i)).isFull(numOfTickets)) {
                return sortedIDs.get(i);
            }
        }
        return "Failed";
    }

    public String bookMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets){
        String response;
        
        if (!serverClients.containsKey(customerID)) {
            addNewCustomerToClients(customerID);
           
        }
 
        if(MovieData.detectMovieServer(movieID).equals(serverName)){
            MovieData movieData = data.get(movieName).get(movieID);
            if(!movieData.isFull(numberOfTickets)){
                if(clientEvents.containsKey(customerID)){
                    if (clientEvents.get(customerID).containsKey(movieName)) {
                        if (!clientEvents.get(customerID).get(movieName).keySet().contains(movieID)) {
                            clientEvents.get(customerID).get(movieName).put(movieID,numberOfTickets);
                        } else {
                            response = "Failed: Movie " + movieID + " Already Booked";
                            try {
                                Logger.serverLog(serverID, customerID, " RMI bookMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                        }else{
                        Map<String,Integer> temp = new ConcurrentHashMap<>();
                        temp.put(movieID,numberOfTickets);
                        clientEvents.get(customerID).put(movieName,temp);
                    }
                }else{

                    Map<String, Integer> temp1 = new ConcurrentHashMap<>();
                    Map<String, Map<String,Integer>> temp2 = new ConcurrentHashMap<>();
                    temp1.put(movieID,numberOfTickets);
                    temp2.put(movieName,temp1);
                    clientEvents.put(customerID,temp2);
                }if(data.get(movieName).get(movieID).setRegisteredCustomer(customerID,numberOfTickets) == 1){
                    response = "Success: Movie " + movieID + " Booked Successfully";
                }else if(data.get(movieName).get(movieID).setRegisteredCustomer(customerID,numberOfTickets) == 3){
                    response = "Failed: Movie " + movieID + " is Full";
                }else{
                    response = "Failed: Cannot Add You To Movie " + movieID;
                }
                System.out.println(clientEvents.toString());
                try {
                    Logger.serverLog(serverID, customerID, " RMI bookMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }else{
                response = "Failed: Movie " + movieID + " is Full";
                try {
                    Logger.serverLog(serverID, customerID, " RMI bookMovie", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }else{
            if(!exceedWeeklyLimit(customerID, movieID.substring(4))){
                String serverResponse = sendUDPMessage(getServerPort(movieID.substring(0, 3)), "bookMovie", customerID, movieName, movieID,numberOfTickets);
                if(serverResponse.startsWith("Success:")){
                    if(clientEvents.get(customerID).keySet().contains(movieName)){
                        clientEvents.get(customerID).get(movieName).put(movieID,numberOfTickets);
                    }else{
                        Map<String,Integer> temp = new ConcurrentHashMap<>();
                        temp.put(movieID,numberOfTickets);
                        clientEvents.get(customerID).put(movieName,temp);
                    }
                }
                try {
                    Logger.serverLog(serverID, customerID, " RMI bookMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return serverResponse;
            } else{
                response = "Failed: You Cannot Book Movie in Other Servers For This Week(Max Weekly Limit = 3)";
                try {
                    Logger.serverLog(serverID, customerID, " RMI bookMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        }
    }

    private String sendUDPMessage(int serverPort, String method, String customerID, String movieName, String movieID, int numOfTickets){
        DatagramSocket aSocket = null;
        String result = "";
        String dataFromClient = method + ";" + customerID + ";" + movieName + ";" + movieID + ";" + Integer.toString(numOfTickets);
        try {
            Logger.serverLog(serverID, customerID, " UDP request sent " + method + " ", " movieID: " + movieID + " movieType: " + movieName + " ", " ... ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try{
            aSocket = new DatagramSocket();
            byte[] message = dataFromClient.getBytes();
            InetAddress aHost = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(message,dataFromClient.length(),aHost,serverPort);
            aSocket.send(request);

            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer,buffer.length);

            aSocket.receive(reply);
            result = new String(reply.getData());
            String[] parts = result.split(";");
            result = parts[0];


        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("IO: " + e.getMessage());
        }finally {
            if(aSocket != null){
                aSocket.close();
            }
        }
        try {
            Logger.serverLog(serverID, customerID, " UDP reply received" + method + " ", " movieID: " + movieID + " movieType: " + movieName + " ", result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private boolean exceedWeeklyLimit(String customerID, String eventDate) {
        int limit = 0;
        for(int i=0;i<3;i++){
            List<String> registeredIDs = null;
            switch(i){
                case 0:
                    if(clientEvents.get(customerID).containsKey("Avatar")){
                        registeredIDs = new ArrayList<>(clientEvents.get(customerID).get("Avatar").keySet());
                        System.out.println("Hello");
                    }
                    break;
                case 1:
                    if(clientEvents.get(customerID).containsKey("Titanic")){
                        registeredIDs = new ArrayList<>(clientEvents.get(customerID).get("Titanic").keySet());
                    }
                    break;
                case 2:
                    if(clientEvents.get(customerID).containsKey("Avenger")){
                        registeredIDs = new ArrayList<>(clientEvents.get(customerID).get("Avenger").keySet());
                    }
                    break;
            }
            System.out.println(registeredIDs);
            if(registeredIDs != null){
                for (String eventID : registeredIDs) {
                    if (eventID.substring(6, 8).equals(eventDate.substring(2, 4)) && eventID.substring(8, 10).equals(eventDate.substring(4, 6))) {
                        int week1 = Integer.parseInt(eventID.substring(4, 6)) / 7;
                        int week2 = Integer.parseInt(eventDate.substring(0, 2)) / 7;
                        if (week1 == week2) {
                            limit++;
                        }
                    }
                    if (limit == 3)
                        return true;
                }
            }
        }
        return false;
    }

    public void addNewCustomerToClients(String customerID) {
        ClientData newCustomer = new ClientData(customerID);
        serverClients.put(newCustomer.getClientID(), newCustomer);
        clientEvents.put(newCustomer.getClientID(), new ConcurrentHashMap<>());
    }

    public String listMovieShowsAvailability(String movieName) throws RemoteException{
        String response;
        Map<String, MovieData> movieDataMap = data.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ": \n");
        if(movieDataMap.size() == 0){
            builder.append("No Movies of the name " + movieName);
        }else{
            for(MovieData md : movieDataMap.values()){
                builder.append(md.toString() + " || ");
            }
            builder.append("\n=====================================\n");
        }
        String otherServer1, otherServer2;
        if(serverID.equals("ATW")){
            otherServer1 = sendUDPMessage(Verdun_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
            otherServer2 = sendUDPMessage(Outremont_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
        }else if (serverID.equals("OUT")) {
            otherServer1 = sendUDPMessage(Verdun_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
            otherServer2 = sendUDPMessage(Atwater_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
        }else{
            otherServer1 = sendUDPMessage(Atwater_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
            otherServer2 = sendUDPMessage(Outremont_Server_Port, "listMovieAvailability", "null", movieName, "null",0);
        }
        builder.append(otherServer1).append(otherServer2);
        response =builder.toString();
        try {
            Logger.serverLog(serverID, "null", " RMI listMovieAvailability ", " movieType: " + movieName + " ", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    public String listMovieShowsAvailabilityUDP(String movieName) throws RemoteException{
        Map<String, MovieData> dataMap = data.get(movieName);
        StringBuilder builder = new StringBuilder();
        builder.append(serverName + " Server " + movieName + ":\n");
        if (dataMap.size() == 0) {
            builder.append("No Movies of the name " + movieName);
        } else {
            for (MovieData md :
                    dataMap.values()) {
                builder.append(md.toString() + " || ");
            }
        }
        builder.append("\n=====================================\n");
        return builder.toString();
    }

    public String cancelMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets) throws RemoteException{
        String response;

        if(MovieData.detectMovieServer(movieID).equalsIgnoreCase(serverName)){
            if(customerID.substring(0,3).equals(serverID)){
                if(!serverClients.containsKey(customerID)){
                    addNewCustomerToClients(customerID);
                    response ="Failed: Customer " + customerID + "Are Not Registered in " + movieID;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }else{
                    if(!clientEvents.get(customerID).get(movieName).containsKey(movieID)){
                        response ="Failed: Customer " + customerID + "Are Not Registered in " + movieID;
                        try {
                            Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return response;
                    }else{
                        int tickets =  clientEvents.get(customerID).get(movieName).get(movieID);
                        if(numberOfTickets == tickets){
                            clientEvents.get(customerID).get(movieName).remove(movieID);
                            data.get(movieName).get(movieID).removeRegisteredCustomer(customerID);
                            response = "Success: Movie " + movieID + " Canceled for " + customerID;
                            try {
                                Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }else if(numberOfTickets > tickets){
                            response = "Failed : Customer have only booked " + tickets + "so you cannot cancel " + numberOfTickets;
                            try {
                                Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }else{
                            int updatedTickets = tickets - numberOfTickets;
                            clientEvents.get(customerID).get(movieName).put(movieID,updatedTickets);
                            data.get(movieName).get(movieID).setRegisteredCustomer(customerID,updatedTickets);
                            response = "Success: Movie " + movieID + " Canceled for " + customerID;
                            try {
                                Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return response;
                        }
                    }
                }
            }else{
                int tickets =  clientEvents.get(customerID).get(movieName).get(movieID);
                if(numberOfTickets == tickets){
                    clientEvents.get(customerID).get(movieName).remove(movieID);
                    data.get(movieName).get(movieID).removeRegisteredCustomer(customerID);
                    response = "Success: Movie " + movieID + " Canceled for " + customerID;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }else if(numberOfTickets > tickets){
                    response = "Failed : Customer have only booked " + tickets + "so you cannot cancel " + numberOfTickets;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }else{
                    int updatedTickets = tickets - numberOfTickets;
                    clientEvents.get(customerID).get(movieName).put(movieID,updatedTickets);
                    data.get(movieName).get(movieID).setRegisteredCustomer(customerID,updatedTickets);
                    response = "Success: Movie " + movieID + " Canceled for " + customerID;
                    try {
                        Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", response);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return response;
                }
            }
        }else{
            if(customerID.substring(0,3).equals(serverID)){
                if(!serverClients.containsKey(customerID)){
                    addNewCustomerToClients(customerID);
                }else{
                    int tickets =  clientEvents.get(customerID).get(movieName).get(movieID);
                    if(numberOfTickets == tickets){
                        clientEvents.get(customerID).get(movieName).remove(movieID);
                        response = sendUDPMessage(getServerPort(movieID.substring(0,3)),"cancelMovie",customerID,movieName,movieID,numberOfTickets) ;
                    }else if(numberOfTickets > tickets){
                        response = sendUDPMessage(getServerPort(movieID.substring(0,3)),"cancelMovie",customerID,movieName,movieID,numberOfTickets) ;;
                        return response;
                    }else{
                        int updatedTickets = tickets - numberOfTickets;
                        clientEvents.get(customerID).get(movieName).put(movieID,updatedTickets);
                        response = sendUDPMessage(getServerPort(movieID.substring(0,3)),"cancelMovie",customerID,movieName,movieID,numberOfTickets) ;;
                    }
                    return response;
                }
            }
            try {
                Logger.serverLog(serverID, customerID, " RMI cancelMovie ", " movieID: " + movieID + " movieType: " + movieName + " ", "Failed: You " + customerID + " Are Not Registered in " + movieID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Failed: You " + customerID + " Are Not Registered in " + movieID;
        }
    }
    public String getBookingSchedule(String customerID) throws RemoteException{
        String response;
        if(!serverClients.containsKey(customerID)){
            addNewCustomerToClients(customerID);
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        Map<String,Map<String,Integer>> movies = clientEvents.get(customerID);
        if(movies.size() == 0){
            response = "Booking Schedule Empty For " + customerID;
            try {
                Logger.serverLog(serverID, customerID, " RMI getBookingSchedule ", "null", response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        StringBuilder builder = new StringBuilder();
        for(String movieName : movies.keySet()){
            builder.append(movieName + ":\n");
            for(String movieID : movies.get(movieName).keySet()){
                builder.append(movieID + " || ");
            }
            builder.append("\n=====================================\n");
        }
        response = builder.toString();
        try {
            Logger.serverLog(serverID, customerID, " RMI getBookingSchedule ", "null", response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

}
