import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BookMyStay {

    public static void main(String[] args) throws InterruptedException {
        RoomInventory inventory = new RoomInventory();
        Map<String, Room> roomTypes = new HashMap<>();
        roomTypes.put("Single", new SingleRoom());
        roomTypes.put("Double", new DoubleRoom());
        roomTypes.put("Suite", new SuiteRoom());

        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Alice", "Single"));
        bookingQueue.addRequest(new Reservation("Bob", "Double"));
        bookingQueue.addRequest(new Reservation("Charlie", "Suite"));
        bookingQueue.addRequest(new Reservation("David", "Single"));
        bookingQueue.addRequest(new Reservation("Eve", "Suite"));

        BookingHistory bookingHistory = new BookingHistory();
        BookingService bookingService = new BookingService(inventory, roomTypes);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                while (!bookingQueue.isEmpty()) {
                    Reservation request = bookingQueue.pollRequestThreadSafe();
                    if (request != null) {
                        bookingService.processSingleBooking(request, bookingHistory);
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        BookingReportService reportService = new BookingReportService(bookingHistory);
        reportService.displayAllBookings();
    }
}

abstract class Room {
    protected int beds, size;
    protected double price;

    public Room(int beds, int size, double price) {
        this.beds = beds;
        this.size = size;
        this.price = price;
    }

    public void displayRoomDetails() {
        System.out.println("Beds: " + beds + ", Size: " + size + " sq.ft, Price: " + price);
    }
}

class SingleRoom extends Room { public SingleRoom() { super(1, 200, 1000); } }
class DoubleRoom extends Room { public DoubleRoom() { super(2, 350, 1800); } }
class SuiteRoom extends Room { public SuiteRoom() { super(3, 600, 3500); } }

class RoomInventory {
    private Map<String, Integer> availability = new ConcurrentHashMap<>();
    public RoomInventory() {
        availability.put("Single", 2);
        availability.put("Double", 1);
        availability.put("Suite", 2);
    }

    public synchronized boolean isAvailable(String type) { return availability.getOrDefault(type, 0) > 0; }
    public synchronized boolean allocateRoom(String type) {
        if (isAvailable(type)) {
            availability.put(type, availability.get(type) - 1);
            return true;
        }
        return false;
    }
    public synchronized void releaseRoom(String type) { availability.put(type, availability.getOrDefault(type, 0) + 1); }
}

class Reservation {
    private String guestName, roomType, reservationId;
    private Room room;

    public Reservation(String guestName, String roomType) { this.guestName = guestName; this.roomType = roomType; }
    public String getGuestName() { return guestName; }
    public String getRoomType() { return roomType; }
    public String getReservationId() { return reservationId; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public void setRoom(Room room) { this.room = room; }
    public Room getRoom() { return room; }
}

class BookingRequestQueue {
    private Queue<Reservation> queue = new ConcurrentLinkedQueue<>();
    public void addRequest(Reservation r) { queue.offer(r); }
    public Reservation pollRequestThreadSafe() { return queue.poll(); }
    public boolean isEmpty() { return queue.isEmpty(); }
}

class BookingService {
    private RoomInventory inventory;
    private Map<String, Room> roomTypes;
    private Map<String, Set<String>> allocatedRoomIds = new ConcurrentHashMap<>();
    private AtomicInteger roomCounter = new AtomicInteger(1);

    public BookingService(RoomInventory inventory, Map<String, Room> roomTypes) {
        this.inventory = inventory;
        this.roomTypes = roomTypes;
    }

    public void processSingleBooking(Reservation request, BookingHistory history) {
        String type = request.getRoomType();
        synchronized (inventory) {
            if (!roomTypes.containsKey(type)) {
                System.out.println("Invalid room type for guest " + request.getGuestName());
                return;
            }
            if (inventory.allocateRoom(type)) {
                String roomId = type.substring(0,1).toUpperCase() + roomCounter.getAndIncrement();
                allocatedRoomIds.putIfAbsent(type, ConcurrentHashMap.newKeySet());
                allocatedRoomIds.get(type).add(roomId);

                request.setReservationId(roomId);
                request.setRoom(roomTypes.get(type));

                history.addReservation(request);

                System.out.println("Reservation Confirmed: " + request.getGuestName() +
                        " | Type: " + type + " | ID: " + roomId);
                request.getRoom().displayRoomDetails();
                System.out.println();
            } else {
                System.out.println("Sorry " + request.getGuestName() + ", no " + type + " rooms available.\n");
            }
        }
    }
}

class BookingHistory {
    private List<Reservation> history = Collections.synchronizedList(new ArrayList<>());
    public void addReservation(Reservation r) { history.add(r); }
    public List<Reservation> getAllReservations() { return history; }
}

class BookingReportService {
    private BookingHistory history;
    public BookingReportService(BookingHistory history) { this.history = history; }
    public void displayAllBookings() {
        System.out.println("=== Booking History ===");
        synchronized(history.getAllReservations()) {
            for (Reservation r : history.getAllReservations()) {
                System.out.println("ID: " + r.getReservationId() + ", Guest: " + r.getGuestName() +
                        ", Type: " + r.getRoomType());
            }
        }
        System.out.println("=======================");
    }
}