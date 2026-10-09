 import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;



enum VehicleType {
    BIKE,
    CAR,
    TRUCK
}

enum SpotType {
    BIKE,
    CAR,
    TRUCK
}

enum SpotStatus {
    AVAILABLE,
    OCCUPIED
}


abstract class Vehicle {

    private final String number;
    private final VehicleType type;

    public Vehicle(String number, VehicleType type) {
        this.number = number;
        this.type = type;
    }

    public String getNumber() {
        return number;
    }

    public VehicleType getType() {
        return type;
    }
}

class Bike extends Vehicle {

    public Bike(String number) {
        super(number, VehicleType.BIKE);
    }
}

class Car extends Vehicle {

    public Car(String number) {
        super(number, VehicleType.CAR);
    }
}

class Truck extends Vehicle {

    public Truck(String number) {
        super(number, VehicleType.TRUCK);
    }
}



class ParkingSpot {

    private final String id;
    private final SpotType type;
    private final int floorNumber;

    private SpotStatus status;
    private Vehicle vehicle;

    public ParkingSpot(String id, SpotType type, int floorNumber) {
        this.id = id;
        this.type = type;
        this.floorNumber = floorNumber;
        this.status = SpotStatus.AVAILABLE;
    }

    public String getId() {
        return id;
    }
    public SpotType getType() {
        return type; 
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public synchronized SpotStatus getStatus() {
        return status;
    }

    public synchronized boolean canFit(Vehicle vehicle) {

        if (vehicle.getType() == VehicleType.BIKE
                && type == SpotType.BIKE) {
            return true;
        }

        if (vehicle.getType() == VehicleType.CAR
                && type == SpotType.CAR) {
            return true;
        }

        if (vehicle.getType() == VehicleType.TRUCK
                && type == SpotType.TRUCK) {
            return true;
        }

        return false;
    }

    public synchronized boolean tryOccupy(Vehicle vehicle) {

        if (status != SpotStatus.AVAILABLE) {
            return false;
        }

        if (!canFit(vehicle)) {
            return false;
        }

        status = SpotStatus.OCCUPIED;
        this.vehicle = vehicle;

        return true;
    }

    public synchronized void release() {

        status = SpotStatus.AVAILABLE;
        vehicle = null;
    }

    public synchronized Vehicle getVehicle() {
        return vehicle;
    }

    @Override
    public String toString() {

        return "Spot{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", floor=" + floorNumber +
                ", status=" + status +
                '}';
    }
}



class Floor {

    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public Floor(int floorNumber) {

        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    public List<ParkingSpot> getSpots() {
        return Collections.unmodifiableList(spots);
    }
}



class Ticket {

    private final String ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private final LocalDateTime inTime;

    private LocalDateTime outTime;

    public Ticket(
            Vehicle vehicle,
            ParkingSpot spot
    ) {

        this.ticketId = UUID.randomUUID().toString();
        this.vehicle = vehicle;
        this.spot = spot;
        this.inTime = LocalDateTime.now();
    }

    public String getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    public LocalDateTime getInTime() {
        return inTime;
    }

    public synchronized void close() {

        if (outTime != null) {
            throw new IllegalStateException(
                    "Ticket already closed"
            );
        }

        outTime = LocalDateTime.now();
    }

    public synchronized LocalDateTime getOutTime() {
        return outTime;
    }
}


interface SpotAllocationStrategy {

    ParkingSpot allocateSpot(
            Vehicle vehicle,
            List<Floor> floors
    );
}


class FirstAvailableSpotStrategy
        implements SpotAllocationStrategy {

    @Override
    public ParkingSpot allocateSpot(
            Vehicle vehicle,
            List<Floor> floors
    ) {

        for (Floor floor : floors) {

            for (ParkingSpot spot : floor.getSpots()) {

               

                if (spot.tryOccupy(vehicle)) {
                    return spot;
                }
            }
        }

        return null;
    }
}


interface PricingStrategy {

    double calculatePrice(
            Vehicle vehicle,
            long hours
    );
}


class HourlyPricingStrategy
        implements PricingStrategy {

    @Override
    public double calculatePrice(
            Vehicle vehicle,
            long hours
    ) {

        // Minimum 1 hour charge
        hours = Math.max(hours, 1);

        switch (vehicle.getType()) {

            case BIKE:
                return hours * 10;

            case CAR:
                return hours * 20;

            case TRUCK:
                return hours * 30;

            default:
                throw new IllegalArgumentException(
                        "Unknown vehicle type"
                );
        }
    }
}



class EntryGate {

    private final String gateId;

    public EntryGate(String gateId) {
        this.gateId = gateId;
    }

    public String getGateId() {
        return gateId;
    }
}


class ExitGate {

    private final String gateId;

    public ExitGate(String gateId) {
        this.gateId = gateId;
    }

    public String getGateId() {
        return gateId;
    }
}


class ParkingLot {

    private final String name;

    private final List<Floor> floors;

    private final List<EntryGate> entryGates;

    private final List<ExitGate> exitGates;

    private final SpotAllocationStrategy allocationStrategy;

    private final PricingStrategy pricingStrategy;

  
    private final Map<String, Ticket> activeTickets;

 
    private final Map<String, Ticket> vehicleTickets;

    public ParkingLot(
            String name,
            List<Floor> floors,
            SpotAllocationStrategy allocationStrategy,
            PricingStrategy pricingStrategy
    ) {

        this.name = name;
        this.floors = new ArrayList<>(floors);

        this.entryGates = new ArrayList<>();
        this.exitGates = new ArrayList<>();

        this.allocationStrategy = allocationStrategy;
        this.pricingStrategy = pricingStrategy;

        this.activeTickets =
                new ConcurrentHashMap<>();

        this.vehicleTickets =
                new ConcurrentHashMap<>();
    }


    public void addEntryGate(EntryGate gate) {
        entryGates.add(gate);
    }

    public void addExitGate(ExitGate gate) {
        exitGates.add(gate);
    }


    public Ticket parkVehicle(Vehicle vehicle) {

        ParkingSpot spot =
                allocationStrategy.allocateSpot(
                        vehicle,
                        floors
                );

        if (spot == null) {

            throw new IllegalStateException(
                    "Parking lot is full"
            );
        }

        Ticket ticket =
                new Ticket(vehicle, spot);


        Ticket existing =
                vehicleTickets.putIfAbsent(
                        vehicle.getNumber(),
                        ticket
                );

        if (existing != null) {

     
            spot.release();

            throw new IllegalStateException(
                    "Vehicle is already parked"
            );
        }

        activeTickets.put(
                ticket.getTicketId(),
                ticket
        );

        return ticket;
    }

  
    public double unparkVehicle(String ticketId) {

     
        Ticket ticket =
                activeTickets.remove(ticketId);

        if (ticket == null) {

            throw new IllegalArgumentException(
                    "Invalid or already processed ticket"
            );
        }

        ticket.close();

        LocalDateTime in =
                ticket.getInTime();

        LocalDateTime out =
                ticket.getOutTime();

        long hours =
                Duration.between(in, out).toHours();

        double price =
                pricingStrategy.calculatePrice(
                        ticket.getVehicle(),
                        hours
                );

      
        ticket.getSpot().release();

        vehicleTickets.remove(
                ticket.getVehicle().getNumber(),
                ticket
        );

        return price;
    }

   

    public boolean isVehicleParked(String vehicleNumber) {

        return vehicleTickets.containsKey(vehicleNumber);
    }

 

    public void displayAvailableSpots() {

        System.out.println(
                "\nAvailable Parking Spots:"
        );

        for (Floor floor : floors) {

            System.out.println(
                    "Floor " +
                    floor.getFloorNumber()
            );

            for (ParkingSpot spot :
                    floor.getSpots()) {

                if (spot.getStatus()
                        == SpotStatus.AVAILABLE) {

                    System.out.println(
                            spot
                    );
                }
            }
        }
    }
}


public class Parking_lot{

    public static void main(String[] args) {

    

        Floor floor1 = new Floor(1);

        floor1.addSpot(
                new ParkingSpot(
                        "F1-B1",
                        SpotType.BIKE,
                        1
                )
        );

        floor1.addSpot(
                new ParkingSpot(
                        "F1-C1",
                        SpotType.CAR,
                        1
                )
        );

        floor1.addSpot(
                new ParkingSpot(
                        "F1-T1",
                        SpotType.TRUCK,
                        1
                )
        );


        Floor floor2 = new Floor(2);

        floor2.addSpot(
                new ParkingSpot(
                        "F2-B1",
                        SpotType.BIKE,
                        2
                )
        );

        floor2.addSpot(
                new ParkingSpot(
                        "F2-C1",
                        SpotType.CAR,
                        2
                )
        );

        floor2.addSpot(
                new ParkingSpot(
                        "F2-T1",
                        SpotType.TRUCK,
                        2
                )
        );

        List<Floor> floors =
                Arrays.asList(
                        floor1,
                        floor2
                );


        SpotAllocationStrategy allocationStrategy =
                new FirstAvailableSpotStrategy();

        PricingStrategy pricingStrategy =
                new HourlyPricingStrategy();

       

        ParkingLot parkingLot =
                new ParkingLot(
                        "BIT Parking",
                        floors,
                        allocationStrategy,
                        pricingStrategy
                );

   

        EntryGate entry1 =
                new EntryGate("ENTRY-1");

        EntryGate entry2 =
                new EntryGate("ENTRY-2");

        parkingLot.addEntryGate(entry1);
        parkingLot.addEntryGate(entry2);

       

        ExitGate exit1 =
                new ExitGate("EXIT-1");

        ExitGate exit2 =
                new ExitGate("EXIT-2");

        parkingLot.addExitGate(exit1);
        parkingLot.addExitGate(exit2);

       

        Vehicle car =
                new Car("JH01AB1234");

        Vehicle bike =
                new Bike("JH01XY5678");

        Vehicle truck =
                new Truck("JH01TR9999");

        Ticket carTicket =
                parkingLot.parkVehicle(car);

        Ticket bikeTicket =
                parkingLot.parkVehicle(bike);

        Ticket truckTicket =
                parkingLot.parkVehicle(truck);

        System.out.println(
                "Car Ticket: " +
                carTicket.getTicketId()
        );

        System.out.println(
                "Bike Ticket: " +
                bikeTicket.getTicketId()
        );

        System.out.println(
                "Truck Ticket: " +
                truckTicket.getTicketId()
        );

        parkingLot.displayAvailableSpots();

     

        System.out.println(
                "\nIs car parked? " +
                parkingLot.isVehicleParked(
                        "JH01AB1234"
                )
        );

      

        double amount =
                parkingLot.unparkVehicle(
                        carTicket.getTicketId()
                );

        System.out.println(
                "\nParking Fee: ₹" +
                amount
        );

        parkingLot.displayAvailableSpots();

        

        System.out.println(
                "\n--- Concurrent Parking Demo ---"
        );

        Vehicle car2 =
                new Car("JH01CAR1111");

        Vehicle car3 =
                new Car("JH01CAR2222");

        Thread entryThread1 =
                new Thread(() -> {

                    try {

                        Ticket ticket =
                                parkingLot.parkVehicle(
                                        car2
                                );

                        System.out.println(
                                "Thread 1 parked car at "
                                + ticket.getSpot().getId()
                        );

                    } catch (Exception e) {

                        System.out.println(
                                "Thread 1: "
                                + e.getMessage()
                        );
                    }
                });

        Thread entryThread2 =
                new Thread(() -> {

                    try {

                        Ticket ticket =
                                parkingLot.parkVehicle(
                                        car3
                                );

                        System.out.println(
                                "Thread 2 parked car at "
                                + ticket.getSpot().getId()
                        );

                    } catch (Exception e) {

                        System.out.println(
                                "Thread 2: "
                                + e.getMessage()
                        );
                    }
                });

        entryThread1.start();
        entryThread2.start();

        try {

            entryThread1.join();
            entryThread2.join();

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }
} 
