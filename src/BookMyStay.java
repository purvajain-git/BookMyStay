import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Main Application with Persistence
public class BookMyStay {

    private static final String INVENTORY_FILE = "inventory.ser";
    private static final String HISTORY_FILE = "bookingHistory.ser";

    public static void main(String[] args) {
        RoomInventory inventory = loadInventory();
        BookingHistory bookingHistory = loadBookingHistory();

        Map<String, Room> roomTypes = new HashMap<>();
        roomTypes.put("Single", new SingleRoom());
        roomTypes.put("Double", new DoubleRoom());
        roomTypes.put("Suite", new SuiteRoom());

        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice", "Single"));
        bookingQueue.addRequest(new Reservation("Bob", "Double"));
        bookingQueue.addRequest(new Reservation("Charlie", "Suite"));

        BookingService bookingService = new BookingService(inventory, roomTypes);

        // Process all bookings sequentially for simplicity
        Reservation request;
        while ((request = bookingQueue.pollRequestThreadSafe()) != null) {
            bookingService.processSingleBooking(request, bookingHistory);
        }

        // Display booking report
        BookingReportService reportService = new BookingReportService(bookingHistory);
        reportService.displayAllBookings();

        // Save state before shutdown
        saveInventory(inventory);
        saveBookingHistory(bookingHistory);
    }

    // Persistence Methods
    private static void saveInventory(RoomInventory inventory) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(INVENTORY_FILE))) {
            oos.writeObject(inventory.getAvailabilitySnapshot());
        } catch (IOException e) {
            System.out.println("Error saving inventory: " + e.getMessage());
        }
    }

    private static RoomInventory loadInventory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(INVENTORY_FILE))) {
            Map<String, Integer> savedAvailability = (Map<String, Integer>) ois.readObject();
            return new RoomInventory(savedAvailability);
        } catch (Exception e) {
            return new RoomInventory(); // default inventory if no file
        }
    }

    private static void saveBookingHistory(BookingHistory history) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(history.getAllReservations());
        } catch (IOException e) {
            System.out.println("Error saving booking history: " + e.getMessage());
        }
    }

    private static BookingHistory loadBookingHistory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HISTORY_FILE))) {
            List<Reservation> savedReservations = (List<Reservation>) ois.readObject();
            return new BookingHistory(savedReservations);
        } catch (Exception e) {
            return new BookingHistory(); // empty history if no file
        }
    }
}

// --------------------- Room Classes ---------------------
abstract class Room implements Serializable {
    protected int numberOfBeds;
    protected int squareFeet;
    protected double pricePerNight;

    public Room(int numberOfBeds, int squareFeet, double pricePerNight) {
        this.numberOfBeds = numberOfBeds;
        this.squareFeet = squareFeet;
        this.pricePerNight = pricePerNight;
    }

    public void displayRoomDetails() {
        System.out.println("Beds: " + numberOfBeds + ", Size: " + squareFeet + " sq.ft, Price: " + pricePerNight);
    }
}

class SingleRoom extends Room { public SingleRoom() { super(1, 200, 1000); } }
class DoubleRoom extends Room { public DoubleRoom() { super(2, 350, 1800); } }
class SuiteRoom extends Room { public SuiteRoom() { super(3, 600, 3500); } }

// --------------------- Inventory ---------------------
class RoomInventory implements Serializable {
    private Map<String, Integer> roomAvailability = new ConcurrentHashMap<>();

    public RoomInventory() {
        roomAvailability.put("Single", 2);
        roomAvailability.put("Double", 1);
        roomAvailability.put("Suite", 2);
    }

    public RoomInventory(Map<String, Integer> availability) {
        roomAvailability.putAll(availability);
    }

    public synchronized boolean allocateRoom(String roomType) {
        int available = roomAvailability.getOrDefault(roomType, 0);
        if (available > 0) {
            roomAvailability.put(roomType, available - 1);
            return true;
        }
        return false;
    }

    public synchronized void releaseRoom(String roomType) {
        roomAvailability.put(roomType, roomAvailability.getOrDefault(roomType, 0) + 1);
    }

    public Map<String, Integer> getAvailabilitySnapshot() {
        return new HashMap<>(roomAvailability);
    }
}

// --------------------- Reservation & Queue ---------------------
class Reservation implements Serializable {
    private String guestName;
    private String roomType;
    private String reservationId;
    private transient Room room; // Room object not serialized

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
    private final Queue<Reservation> queue = new ConcurrentLinkedQueue<>();
    public void addRequest(Reservation r) { queue.offer(r); }
    public Reservation pollRequestThreadSafe() { return queue.poll(); }
}

// --------------------- Booking Service ---------------------
class BookingService {
    private final RoomInventory inventory;
    private final Map<String, Room> roomTypes;
    private final Map<String, Set<String>> allocatedRoomIds = new ConcurrentHashMap<>();
    private final AtomicInteger roomCounter = new AtomicInteger(1);

    public BookingService(RoomInventory inventory, Map<String, Room> roomTypes) {
        this.inventory = inventory;
        this.roomTypes = roomTypes;
    }

    public void processSingleBooking(Reservation request, BookingHistory history) {
        String type = request.getRoomType();
        synchronized (inventory) {
            if (inventory.allocateRoom(type)) {
                String roomId = type.substring(0,1).toUpperCase() + roomCounter.getAndIncrement();
                allocatedRoomIds.putIfAbsent(type, ConcurrentHashMap.newKeySet());
                allocatedRoomIds.get(type).add(roomId);

                request.setReservationId(roomId);
                request.setRoom(roomTypes.get(type));

                history.addReservation(request);

                System.out.println("Reservation Confirmed: " + request.getGuestName() +
                        ", Room Type: " + type + ", Room ID: " + roomId);
                request.getRoom().displayRoomDetails();
            } else {
                System.out.println("Sorry " + request.getGuestName() + ", no " + type + " rooms available.");
            }
        }
    }
}

// --------------------- Booking History ---------------------
class BookingHistory implements Serializable {
    private final List<Reservation> history;

    public BookingHistory() {
        history = Collections.synchronizedList(new ArrayList<>());
    }

    public BookingHistory(List<Reservation> savedReservations) {
        history = Collections.synchronizedList(new ArrayList<>(savedReservations));
    }

    public void addReservation(Reservation r) { history.add(r); }
    public List<Reservation> getAllReservations() { return history; }
}

// --------------------- Reporting ---------------------
class BookingReportService {
    private final BookingHistory history;

    public BookingReportService(BookingHistory history) { this.history = history; }

    public void displayAllBookings() {
        System.out.println("\n=== Booking History ===");
        synchronized(history.getAllReservations()) {
            for (Reservation r : history.getAllReservations()) {
                System.out.println("Reservation ID: " + r.getReservationId() +
                        ", Guest: " + r.getGuestName() +
                        ", Room Type: " + r.getRoomType());
            }
        }
        System.out.println("======================");
    }
}