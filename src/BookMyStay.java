


import java.util.HashMap;
import java.util.Map;

public class BookMyStay {

    public static void main(String[] args) {
        RoomInventory inventory = new RoomInventory();

        Map<String, Room> roomTypes = new HashMap<>();
        roomTypes.put("Single", new SingleRoom());
        roomTypes.put("Double", new DoubleRoom());
        roomTypes.put("Suite", new SuiteRoom());

        SearchService searchService = new SearchService(inventory, roomTypes);
        searchService.displayAvailableRooms();
    }
}

class SearchService {

    private RoomInventory inventory;
    private Map<String, Room> roomTypes;

    public SearchService(RoomInventory inventory, Map<String, Room> roomTypes) {
        this.inventory = inventory;
        this.roomTypes = roomTypes;
    }

    public void displayAvailableRooms() {
        Map<String, Integer> availability = inventory.getRoomAvailability();

        for (String type : availability.keySet()) {
            int count = availability.get(type);

            if (count > 0 && roomTypes.containsKey(type)) {
                System.out.println(type + " Room:");
                roomTypes.get(type).displayRoomDetails();
                System.out.println("Available: " + count);
                System.out.println();
            }
        }
    }
}

class RoomInventory {

    private Map<String, Integer> roomAvailability;

    public RoomInventory() {
        roomAvailability = new HashMap<>();
        initializeInventory();
    }

    private void initializeInventory() {
        roomAvailability.put("Single", 5);
        roomAvailability.put("Double", 0);
        roomAvailability.put("Suite", 2);
    }

    public Map<String, Integer> getRoomAvailability() {
        return roomAvailability;
    }

    public void updateAvailability(String roomType, int count) {
        roomAvailability.put(roomType, count);
    }
}

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

class SingleRoom extends Room {
    public SingleRoom() {
        super(1, 200, 1000);
    }
}

class DoubleRoom extends Room {
    public DoubleRoom() {
        super(2, 350, 1800);
    }
}

class SuiteRoom extends Room {
    public SuiteRoom() {
        super(3, 600, 3500);
    }
}