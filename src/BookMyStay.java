import java.util.*;

// Main Application
public class BookMyStay {

    public static void main(String[] args) {
        // Initialize inventory
        RoomInventory inventory = new RoomInventory();

        // Initialize rooms
        Map<String, Room> roomTypes = new HashMap<>();
        roomTypes.put("Single", new SingleRoom());
        roomTypes.put("Double", new DoubleRoom());
        roomTypes.put("Suite", new SuiteRoom());

        // Booking queue and requests
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice", "Single"));
        bookingQueue.addRequest(new Reservation("Bob", "Double"));
        bookingQueue.addRequest(new Reservation("Charlie", "Suite"));

        // Process bookings
        BookingService bookingService = new BookingService(inventory, roomTypes);
        Map<String, Reservation> confirmedReservations = bookingService.processBookings(bookingQueue);

        // Add-on services
        AddOnServiceManager serviceManager = new AddOnServiceManager();

        // Guests select add-ons
        serviceManager.addService(confirmedReservations.get("S1"), new Service("Breakfast", 200));
        serviceManager.addService(confirmedReservations.get("S1"), new Service("Airport Pickup", 500));
        serviceManager.addService(confirmedReservations.get("D2"), new Service("Spa", 1000));

        // Display reservations with add-ons
        serviceManager.displayAllServices();
    }
}

// Domain Classes
abstract class Room {
    protected int numberOfBeds;
    protected int squareFeet;
    protected double pricePerNight;

    public Room(int numberOfBeds, int squareFeet, double pricePerNight) {
        this.numberOfBeds = numberOfBeds;
        this.squareFeet = squareFeet;
        this.pricePerNight = pricePerNight;
    }

    public void displayRoomDetails() {
        System.out.println("Beds: " + numberOfBeds);
        System.out.println("Size: " + squareFeet + " sq.ft");
        System.out.println("Price per night: " + pricePerNight);
    }

    public double getPricePerNight() {
        return pricePerNight;
    }
}

class SingleRoom extends Room { public SingleRoom() { super(1, 200, 1000); } }
class DoubleRoom extends Room { public DoubleRoom() { super(2, 350, 1800); } }
class SuiteRoom extends Room { public SuiteRoom() { super(3, 600, 3500); } }

// Inventory
class RoomInventory {
    private Map<String, Integer> roomAvailability;

    public RoomInventory() {
        roomAvailability = new HashMap<>();
        roomAvailability.put("Single", 2);
        roomAvailability.put("Double", 1);
        roomAvailability.put("Suite", 2);
    }

    public boolean isAvailable(String roomType) {
        return roomAvailability.getOrDefault(roomType, 0) > 0;
    }

    public void decrementAvailability(String roomType) {
        roomAvailability.put(roomType, roomAvailability.get(roomType) - 1);
    }
}

// Booking Request & Queue
class Reservation {
    private String guestName;
    private String roomType;
    private String reservationId;
    private Room room;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() { return guestName; }
    public String getRoomType() { return roomType; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public void setRoom(Room room) { this.room = room; }
    public Room getRoom() { return room; }
}

class BookingRequestQueue {
    private Queue<Reservation> queue = new LinkedList<>();
    public void addRequest(Reservation r) { queue.offer(r); }
    public Reservation pollRequest() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}

// Booking Service
class BookingService {
    private RoomInventory inventory;
    private Map<String, Room> roomTypes;
    private Map<String, Set<String>> allocatedRoomIds = new HashMap<>();
    private int roomCounter = 1;

    public BookingService(RoomInventory inventory, Map<String, Room> roomTypes) {
        this.inventory = inventory;
        this.roomTypes = roomTypes;
    }

    public Map<String, Reservation> processBookings(BookingRequestQueue bookingQueue) {
        Map<String, Reservation> confirmed = new HashMap<>();

        while (!bookingQueue.isEmpty()) {
            Reservation request = bookingQueue.pollRequest();
            String type = request.getRoomType();

            if (inventory.isAvailable(type)) {
                String roomId = type.substring(0, 1).toUpperCase() + roomCounter++;
                allocatedRoomIds.putIfAbsent(type, new HashSet<>());
                allocatedRoomIds.get(type).add(roomId);

                inventory.decrementAvailability(type);
                request.setReservationId(roomId);
                request.setRoom(roomTypes.get(type));

                confirmed.put(roomId, request);

                System.out.println("Reservation Confirmed for " + request.getGuestName() +
                        " | Room Type: " + type + " | Room ID: " + roomId);
                request.getRoom().displayRoomDetails();
                System.out.println();
            } else {
                System.out.println("Sorry " + request.getGuestName() + ", no " + type + " rooms available.\n");
            }
        }

        return confirmed;
    }
}

// Add-On Service
class Service {
    private String name;
    private double price;

    public Service(String name, double price) { this.name = name; this.price = price; }
    public String getName() { return name; }
    public double getPrice() { return price; }
}

// Add-On Service Manager
class AddOnServiceManager {
    private Map<String, List<Service>> reservationServices = new HashMap<>();

    public void addService(Reservation reservation, Service service) {
        reservationServices.putIfAbsent(reservation.getReservationId(), new ArrayList<>());
        reservationServices.get(reservation.getReservationId()).add(service);
    }

    public void displayAllServices() {
        System.out.println("Add-On Services for Reservations:");
        for (String resId : reservationServices.keySet()) {
            System.out.println("Reservation ID: " + resId);
            double totalCost = 0;
            for (Service s : reservationServices.get(resId)) {
                System.out.println("- " + s.getName() + " | Price: " + s.getPrice());
                totalCost += s.getPrice();
            }
            System.out.println("Total Add-On Cost: " + totalCost);
            System.out.println();
        }
    }
}