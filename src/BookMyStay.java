import java.util.*;

public class BookMyStay {

    public static void main(String[] args) {
        // Initialize inventory
        RoomInventory inventory = new RoomInventory();

        // Initialize rooms
        Map<String, Room> roomTypes = new HashMap<>();
        roomTypes.put("Single", new SingleRoom());
        roomTypes.put("Double", new DoubleRoom());
        roomTypes.put("Suite", new SuiteRoom());

        // Initialize booking queue and add requests
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice", "Single"));
        bookingQueue.addRequest(new Reservation("Bob", "Double"));
        bookingQueue.addRequest(new Reservation("Charlie", "Suite"));
        bookingQueue.addRequest(new Reservation("David", "Single"));
        bookingQueue.addRequest(new Reservation("Eve", "Suite"));

        // Process bookings
        BookingService bookingService = new BookingService(inventory, roomTypes);
        bookingService.processBookings(bookingQueue);
    }
}

// Domain classes
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
}

class SingleRoom extends Room { public SingleRoom() { super(1, 200, 1000); } }
class DoubleRoom extends Room { public DoubleRoom() { super(2, 350, 1800); } }
class SuiteRoom extends Room { public SuiteRoom() { super(3, 600, 3500); } }

// Inventory management
class RoomInventory {
    private Map<String, Integer> roomAvailability;

    public RoomInventory() {
        roomAvailability = new HashMap<>();
        initializeInventory();
    }

    private void initializeInventory() {
        roomAvailability.put("Single", 2);
        roomAvailability.put("Double", 1);
        roomAvailability.put("Suite", 2);
    }

    public Map<String, Integer> getRoomAvailability() { return roomAvailability; }

    public boolean isAvailable(String roomType) {
        return roomAvailability.getOrDefault(roomType, 0) > 0;
    }

    public void decrementAvailability(String roomType) {
        roomAvailability.put(roomType, roomAvailability.get(roomType) - 1);
    }
}

// Booking request queue
class Reservation {
    private String guestName;
    private String roomType;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() { return guestName; }
    public String getRoomType() { return roomType; }
}

class BookingRequestQueue {
    private Queue<Reservation> queue = new LinkedList<>();

    public void addRequest(Reservation reservation) { queue.offer(reservation); }
    public Reservation pollRequest() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}

// Booking service
class BookingService {
    private RoomInventory inventory;
    private Map<String, Room> roomTypes;
    private Map<String, Set<String>> allocatedRoomIds = new HashMap<>();
    private int roomCounter = 1;

    public BookingService(RoomInventory inventory, Map<String, Room> roomTypes) {
        this.inventory = inventory;
        this.roomTypes = roomTypes;
    }

    public void processBookings(BookingRequestQueue bookingQueue) {
        while (!bookingQueue.isEmpty()) {
            Reservation request = bookingQueue.pollRequest();
            String type = request.getRoomType();

            if (inventory.isAvailable(type)) {
                // Generate unique room ID
                String roomId = type.substring(0, 1).toUpperCase() + roomCounter++;

                // Assign room ID
                allocatedRoomIds.putIfAbsent(type, new HashSet<>());
                allocatedRoomIds.get(type).add(roomId);

                // Update inventory
                inventory.decrementAvailability(type);

                // Confirm reservation
                System.out.println("Reservation Confirmed for " + request.getGuestName());
                System.out.println("Room Type: " + type + ", Room ID: " + roomId);
                roomTypes.get(type).displayRoomDetails();
                System.out.println();
            } else {
                System.out.println("Sorry " + request.getGuestName() + ", no " + type + " rooms available.\n");
            }
        }
    }
}