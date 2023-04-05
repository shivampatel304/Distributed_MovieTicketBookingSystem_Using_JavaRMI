package Interface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMI_Interface extends Remote {

    // Only for Admin use
    String addMovieSlots(String movieID,String movieName, int bookingCapacity) throws RemoteException;

    String removeMovieSlots(String movieID, String movieName) throws RemoteException;

    String listMovieShowsAvailability(String movieName) throws RemoteException;

    // For both Admin and Customer


    String bookMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets) throws  RemoteException;

    String cancelMovieTickets(String customerID, String movieID, String movieName, int numberOfTickets) throws RemoteException;

    String getBookingSchedule(String customerID) throws RemoteException;

}


