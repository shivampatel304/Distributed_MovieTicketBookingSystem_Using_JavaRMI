package Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shivam Patel (40226428)
 * @version 3.0
 */

public class MovieData {
    private String movieName;
    private String movieID;
    private String movieServer;
    private int movieCapacity;
    private String movieDate;
    private String movieTimeSlot;
   // private List<String> registeredCustomer;

    private Map<String,Integer> registeredCustomer;

    public MovieData(String movieID, String movieName, int bookingCapacity){
        this.movieCapacity = bookingCapacity;
        this.movieID = movieID;
        this.movieName = movieName;
        this.movieTimeSlot = detectTimeSlot(movieID);
        this.movieServer = detectMovieServer(movieID);
        this.movieDate = detectMovieDate(movieID);
        //registeredCustomer = new ArrayList<>();
        registeredCustomer = new HashMap<>();
    }

    public static String detectMovieDate(String movieID) {
        return movieID.substring(4, 6) + "/" + movieID.substring(6, 8) + "/20" + movieID.substring(8, 10);
    }

    public static String detectMovieServer(String movieID) {
        if(movieID.substring(0,3).equals("ATW")){
            return "Atwater";
        } else if (movieID.substring(0,3).equals("VER")) {
            return "Verdun";
        }else{
            return "Outremont";
        }
    }

    public static String detectTimeSlot(String movieID) {
        if(movieID.charAt(3) == 'M'){
            return "Morning";
        }else if(movieID.charAt(3) == 'A'){
            return "Afternoon";
        }else {
            return "Evening";
        }
    }

    public  String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getMovieID() {
        return movieID;
    }

    public void setMovieID(String movieID) {
        this.movieID = movieID;
    }

    public String getMovieServer() {
        return movieServer;
    }

    public void setMovieServer(String movieServer) {
        this.movieServer = movieServer;
    }

    public int getMovieCapacity() {
        return movieCapacity;
    }

    public void setMovieCapacity(int movieCapacity) {
        this.movieCapacity = movieCapacity;
    }

    public String getMovieDate() {
        return movieDate;
    }

    public void setMovieDate(String movieDate) {
        this.movieDate = movieDate;
    }

    public String getMovieTimeSlot() {
        return movieTimeSlot;
    }

    public void setMovieTimeSlot(String movieTimeSlot) {
        this.movieTimeSlot = movieTimeSlot;
    }

    public int getMovieRemainingCapacity(){
        int totalBooked = 0;
        for(int i : registeredCustomer.values()){
            totalBooked += i;
        }
        return movieCapacity - totalBooked;
    }

    public boolean isFull(int numberOfTickets){
        if(getMovieRemainingCapacity() >= numberOfTickets)
            return false;
        else
            return true;
    }

    public Map<String, Integer> getRegisteredCustomer() {
        return registeredCustomer;
    }

    public int setRegisteredCustomer(String registeredCustomerID, int numOfTickets) {
        if(!isFull(numOfTickets)){
            if(registeredCustomer.keySet().contains(registeredCustomerID)){
                return -1;
            }else{
                registeredCustomer.put(registeredCustomerID, numOfTickets);
                movieCapacity -= numOfTickets;
                return 1;
            }
        }else{
            return 3;
        }
    }

    public boolean removeRegisteredCustomer(String registeredCustomerID){
         if(registeredCustomer.remove(registeredCustomerID) != null){
            return true;
         }else{
             System.out.println("Customer has not booked any tickets for this show.");
             return false;
         }
    }

    @Override
    public String toString() {
        return " (" + getMovieID() + ") in the " + getMovieTimeSlot() + " of " + getMovieDate() + " Total[Remaining] Capacity: " + getMovieCapacity() + "[" + getMovieRemainingCapacity() + "]";
    }
}
