import java.util.*;
import java.util.concurrent.*;

class FMS{
    public static void main(String[] args) throws InterruptedException {

        Aircraft airbus = new Aircraft("airbus");
        airbus.addseat(100);

        Flight flight = new Flight(0, airbus, "ranchi", "delhi", new Date(), new Date());

        int numberOfThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);

        System.out.println("=== Starting Concurrent Booking Simulation (10 Threads trying to book Seat 0 simultaneously) ===");

        for (int i = 0; i < numberOfThreads; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    Ticket ticket = new Ticket(userId, flight, 0); // All requesting Seat 0
                    Passenger passenger = new Passenger("Passenger-" + userId);
                    Booking booking = new Booking(ticket, passenger);
                    booking.pay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

       
        latch.countDown();

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("=== Simulation Completed ===");
    }
}

enum Seat_status{
    available,
    booked;
}

class Seat{
    int Seat_no;
    Seat_status seat_status;
    public Seat( int seat_no){
        this.seat_status= Seat_status.available;
        this.Seat_no= seat_no;
    }
}

class Aircraft{
    String Name;
    Map<Integer, Seat> Seatplan;

    public Aircraft( String Name){
        this.Name= Name;
        this.Seatplan= new HashMap<>();
    }

    public void addseat(int i){
        for( int k=0; k< i; k++){
        Seatplan.put( k, new Seat(k));
    }
    }

    Seat seat(int i){
        return Seatplan.get(i);
    }
}

class Flight{
    int id;
    Aircraft aircraft;
    String from;
    String to;
    Date DeparDate;
    Date ArrivDate; 
    Map<Integer, Seat> seatplan;
    public Flight( int id, Aircraft aircraft, String from , String to, Date DeparDate, Date ArrivDate){
        this.ArrivDate= ArrivDate;
        this.aircraft= aircraft;
        this.from= from;
        this.to= to;
        this.id= id;
        this.seatplan= new HashMap<>();

        for(Seat s : aircraft.Seatplan.values()){
            seatplan.put(s.Seat_no, new Seat(s.Seat_no));
        }
    }
    



    




}

class Booking{

    Ticket ticket;
    Passenger p;

    Booking (Ticket t, Passenger p){
        this.p= p;
        this.ticket= t;
    }

    boolean pay(){
        
        synchronized (ticket.seat) {
            if( ticket.seat.seat_status == Seat_status.booked){
                System.out.println("[" + Thread.currentThread().getName() + "] -> Seat " + ticket.seat.Seat_no + " NOT AVAILABLE for " + p.name);
                return false;
            }
            else{
                ticket.seat.seat_status = Seat_status.booked;
                ticket.set_status(Ticket_status.confirmed);
                System.out.println("[" + Thread.currentThread().getName() + "] -> PAID SUCCESSFULLY for " + p.name + " (Ticket Confirmed)");
                return true;
            }
        }
    }
}
enum Ticket_status{
    confirmed,
    waiting,

}

class Ticket{
    int id;
    Flight flight;
    Seat seat;
    Ticket_status ticket_status;

    public Ticket( int id, Flight flight, int seat){
        this.id= id;
        this.flight= flight;
        this.seat= flight.seatplan.get(seat);
        this.ticket_status= Ticket_status.waiting;
    }

    Boolean check( ){

        if( seat.seat_status== Seat_status.booked){

            System.out.println("cannot book");
            return false;
        }

        return true;
    }

    void set_status(Ticket_status curr){
        ticket_status= curr;
    }

}


class Passenger{
    String name;

    Passenger( String name){
        this.name= name;
    }


}

