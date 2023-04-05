package Client;

import Interface.RMI_Interface;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import Logger.Logger;

/**
 * @author shivam Patel (40226428)
 * @version 3.0
 */
public class Client {

    public static final int SERVER_ATWATER = 2964;
    public static final int SERVER_VERDUN = 2965;
    public static final int SERVER_OUTREMONT = 2966;

    static Scanner sc;
    public static void main(String[] args) throws IOException, NotBoundException {
        init();
    }

    public static void init() throws IOException, NotBoundException {
        sc = new Scanner(System.in);
        String userID;
        System.out.println("Enter Your User ID : ");
        userID = sc.next().trim().toUpperCase();
        Logger.clientLog(userID, " login attempt");
        if(valid(userID)){
            if(userID.charAt(3) == 'A'){
                //perform admin task
                try{
                    admin(userID, getServerPort(userID.substring(0,3)));
                    Logger.clientLog(userID, " Customer Login successful");
                }
                catch (Exception e)
                {
                    System.out.println(e);
                }

            }else{
                //perform customer task
                try{
                    client(userID , getServerPort(userID.substring(0,3)));
                    Logger.clientLog(userID, " Manager Login successful");
                }
                catch (Exception e)
                {
                    System.out.println(e);
                }

            }
        }else{
            System.out.println("UserID is not correct ! (: ");
            Logger.clientLog(userID, " UserID is not in correct format");
            Logger.deleteALogFile(userID);
        }
    }

    private static int getServerPort(String string) {
        if(string.equals("ATW")){
            return SERVER_ATWATER;
        }else if(string.equals("VER")){
            return SERVER_VERDUN;
        }else if(string.equals("OUT")){
            return SERVER_OUTREMONT;
        }
        return 1;
    }

    private static void client(String userID , int serverPort) throws IOException, NotBoundException{
        if(serverPort == 1){
            return;
        }

        Registry registry = LocateRegistry.getRegistry(serverPort);
        RMI_Interface remoteObject = (RMI_Interface)registry.lookup("DMTBS");
        boolean repeat = true;
        printMenu("Customer");
        int choice = Integer.parseInt(sc.next());
        String movieID,movieName;
        int bookingCapacity;
        int numOfTickets;
        String serverResponse;

        switch(choice){
            case 1:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                System.out.println("Enter the number of tickets you want to booked. ");
                numOfTickets = Integer.parseInt(sc.next());
                Logger.clientLog(userID, " attempting to bookMovie");
                serverResponse = remoteObject.bookMovieTickets(userID,movieID,movieName,numOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " bookMovie", " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                break;
            case 2:
                Logger.clientLog(userID, " attempting to getBookingSchedule");
                serverResponse = remoteObject.getBookingSchedule(userID);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " bookMovie", " null ", serverResponse);
                break;
            case 3:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                System.out.println("Enter the number of tickets you want to Cancel. ");
                numOfTickets = Integer.parseInt(sc.next());
                Logger.clientLog(userID, " attempting to cancelMovie");
                serverResponse = remoteObject.cancelMovieTickets(userID,movieID,movieName,numOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " bookMovie", " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                break;
            case 4:
                repeat = false;
                Logger.clientLog(userID, " attempting to Logout");
                init();
                break;
        }
        if(repeat){
            client(userID,serverPort);
        }
    }
    private static void admin(String userID,int serverPort) throws IOException, NotBoundException {
        if(serverPort == 1){
            return;
        }

        Registry registry = LocateRegistry.getRegistry(serverPort);
        RMI_Interface remoteObject = (RMI_Interface)registry.lookup("DMTBS");
        boolean repeat = true;
        printMenu("Admin");
        int choice = Integer.parseInt(sc.next());
        String movieID,movieName;
        int bookingCapacity;
        int numOfTickets;
        String serverResponse;
        switch(choice){
            case 1:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                bookingCapacity = promptForBookingCapacity();
                Logger.clientLog(userID, " attempting to addMovie");
                serverResponse = remoteObject.addMovieSlots(movieID,movieName,bookingCapacity);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " addMovie", " movieID: " + movieID + " movieType: " + movieName + " movieCapacity: " + bookingCapacity + " ", serverResponse);
                break;
            case 2:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                Logger.clientLog(userID, " attempting to removeEvent");
                serverResponse = remoteObject.removeMovieSlots(movieID,movieName);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " removeMovie", " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                break;
            case 3:
                movieName = promptForMovie();
                Logger.clientLog(userID, " attempting to listMovieAvailability");
                serverResponse = remoteObject.listMovieShowsAvailability(movieName);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " listMovieAvailability", " movieType: " + movieName + " ", serverResponse);
                break;
            case 4:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                System.out.println("Enter the number of tickets you want to booked. ");
                numOfTickets = Integer.parseInt(sc.next());
                Logger.clientLog(userID, " attempting to bookMovie");
                serverResponse = remoteObject.bookMovieTickets(userID,movieID,movieName,numOfTickets);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " bookMovie", " customerID: " + userID + " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                break;
            case 5:
                Logger.clientLog(userID, " attempting to getBookingSchedule");
                serverResponse = remoteObject.getBookingSchedule(userID);
                System.out.println(serverResponse);
                Logger.clientLog(userID, " getBookingSchedule", " customerID: " + userID + " ", serverResponse);
                break;
            case 6:
                movieName = promptForMovie();
                movieID = promptForMovieID();
                System.out.println("Enter the number of tickets you want to Cancel. ");
                numOfTickets = Integer.parseInt(sc.next());
                Logger.clientLog(userID, " attempting to cancelMovie");
                serverResponse = remoteObject.cancelMovieTickets(userID,movieID,movieName,numOfTickets);
                Logger.clientLog(userID, " cancelMovie", " customerID: " + userID + " movieID: " + movieID + " movieType: " + movieName + " ", serverResponse);
                break;
            case 7:
                repeat = false;
                Logger.clientLog(userID, "attempting to Logout");
                init();
                break;
        }
        if(repeat){
            admin(userID,serverPort);
        }

    }

    private static int promptForBookingCapacity() {
        System.out.println("*************************************");
        System.out.println("Please enter the booking capacity:");
        return sc.nextInt();
    }

    private static String promptForMovieID() {
        System.out.println("*************************************");
        System.out.println("Please enter the MovieID :");
        int flag = 0;
        String movieID = sc.next().trim().toUpperCase();
        if (movieID.length() == 10) {
            if (movieID.substring(0, 3).equalsIgnoreCase("ATW") ||
                    movieID.substring(0, 3).equalsIgnoreCase("OUT") ||
                    movieID.substring(0, 3).equalsIgnoreCase("VER")) {
                if (movieID.substring(3, 4).equalsIgnoreCase("M") ||
                        movieID.substring(3, 4).equalsIgnoreCase("A") ||
                        movieID.substring(3, 4).equalsIgnoreCase("E")) {
                    if(isDigitOnly(movieID.substring(4))){
                        flag = 1;
                        return movieID;
                    }
                }
            }
        }
        if(flag == 0){
            System.out.println("The movie ID is invalid.");
        }
        return promptForMovieID();
    }

    private static String promptForMovie() {
        System.out.println("*************************************");
        System.out.println("Please choose an eventType below:");
        System.out.println("1.Avatar");
        System.out.println("2.Avenger");
        System.out.println("3.Titanic");
        int userInput = Integer.parseInt(sc.next());
        switch (userInput){
            case 1:
                return "Avatar";
            case 2:
                return "Avenger";
            case 3:
                return "Titanic";
            default:
                System.out.println("Please Enter valid Input.");
                return promptForMovie();
        }
    }

    private static void printMenu(String userType) {
        System.out.println("*************************************");
        System.out.println("Please choose an option below:");
        if (userType.equals("Customer")) {
            System.out.println("1.Book Movie Tickets");
            System.out.println("2.Get Booking Schedule");
            System.out.println("3.Cancel Movie Tickets");
            System.out.println("4.Logout");
        } else if (userType.equals("Admin")) {
            System.out.println("1.Add Movie Slots");
            System.out.println("2.Remove Movie Slots");
            System.out.println("3.List Movie Shows Availability");
            System.out.println("4.Book Movie Tickets");
            System.out.println("5.Get Booking Schedule");
            System.out.println("6.Cancel Movie Tickets");
            System.out.println("7.Logout");
        }
    }

    private static boolean valid(String userID) {
        if(userID.length() == 8){
            if(userID.substring(0,3).equals("ATW") || userID.substring(0,3).equals("OUT") || userID.substring(0,3).equals("VER")){
                if(userID.charAt(3) == 'A' || userID.charAt(3) == 'C'){
                    if(isDigitOnly(userID.substring(4))) {
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    return false;
                }
            }else {
                return false;
            }
        }
        else{
            return false;
        }
    }




    public static boolean isDigitOnly(String text){

        boolean isDigit = false;

        if (text.matches("[0-9]+") && text.length() > 2) {
            isDigit = true;
        }else {
            isDigit = false;
        }

        return isDigit;
    }
}
