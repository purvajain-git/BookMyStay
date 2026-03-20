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

        // Booking service
        BookingService bookingService = new BookingService(inventory, roomTypes);

        // Booking history
        BookingHistory bookingHistory = new BookingHistory();

        // Process bookings and record history
        Map<String, Reservation> confirmedReservations = bookingService.processBookings(bookingQueue, bookingHistory);

        // Generate booking report
        BookingReportService reportService = new BookingReportService(bookingHistory);
        reportService.displayAllBookings();
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

    public double getPricePerNight() { return pricePerNight; }
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

    public Map<String, Reservation> processBookings(BookingRequestQueue bookingQueue, BookingHistory history) {
        Map<String, Reservation> confirmed = new HashMap<>();

        while (!bookingQueue.isEmpty()) {
            Reservation request = bookingQueue.pollRequest();
            String type = request.getRoomType();

            if (inventory.isAvailable(type)) {
                String roomId = type.substring(0,1).toUpperCase() + roomCounter++;
                allocatedRoomIds.putIfAbsent(type, new HashSet<>());
                allocatedRoomIds.get(type).add(roomId);

                inventory.decrementAvailability(type);
                request.setReservationId(roomId);
                request.setRoom(roomTypes.get(type));

                confirmed.put(roomId, request);
                history.addReservation(request);

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

// Booking History
class BookingHistory {
    private List<Reservation> history = new ArrayList<>();
    public void addReservation(Reservation r) { history.add(r); }
    public List<Reservation> getAllReservations() { return history; }
}

// Reporting Service
class BookingReportService {
    private BookingHistory history;
    public BookingReportService(BookingHistory history) { this.history = history; }

    public void displayAllBookings() {
        System.out.println("=== Booking History Report ===");
        for (Reservation r : history.getAllReservations()) {
            System.out.println("Reservation ID: " + r.getReservationId() +
                    ", Guest: " + r.getGuestName() +
                    ", Room Type: " + r.getRoomType());
        }
        System.out.println("==============================");
    }
}