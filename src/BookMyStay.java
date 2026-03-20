import java.util.HashMap;
import java.util.Map;

public class BookMyStay {

    public static void main(String[] args) {
        RoomInventory inventory = new RoomInventory();

        Map<String, Integer> availability = inventory.getRoomAvailability();

        for (String roomType : availability.keySet()) {
            System.out.println(roomType + " Available: " + availability.get(roomType));
        }

        inventory.updateAvailability("Single", 4);

        System.out.println("\nAfter Update:");

        for (String roomType : availability.keySet()) {
            System.out.println(roomType + " Available: " + availability.get(roomType));
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
        roomAvailability.put("Double", 3);
        roomAvailability.put("Suite", 2);
    }

    public Map<String, Integer> getRoomAvailability() {
        return roomAvailability;
    }

    public void updateAvailability(String roomType, int count) {
        roomAvailability.put(roomType, count);
    }
}