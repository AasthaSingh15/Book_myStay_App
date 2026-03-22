import java.util.*;

// ===== MAIN CLASS =====
public class HotelBookingApp {

    public static void main(String[] args) {

        System.out.println("Booking Cancellation\n");

        RoomInventory inventory = new RoomInventory();
        BookingRequestQueue queue = new BookingRequestQueue();

        queue.addRequest(new Reservation("Abhi", "Single"));
        queue.addRequest(new Reservation("Subha", "Double"));
        queue.addRequest(new Reservation("Vanmathi", "Suite"));

        RoomAllocationService allocationService = new RoomAllocationService();
        CancellationService cancellationService = new CancellationService();

        List<String> reservationIds = new ArrayList<>();

        // ===== ALLOCATION =====
        while (queue.hasPendingRequests()) {
            Reservation r = queue.getNextRequest();
            String roomId = allocationService.allocateRoom(r, inventory);

            if (roomId != null) {
                reservationIds.add(roomId);

                // register booking for cancellation
                cancellationService.registerBooking(roomId, r.getRoomType());
            }
        }

        // ===== CANCELLATION =====
        if (!reservationIds.isEmpty()) {
            String cancelId = reservationIds.get(0);

            cancellationService.cancelBooking(cancelId, inventory);
        }

        // ===== SHOW ROLLBACK =====
        cancellationService.showRollbackHistory();

        System.out.println("\nUpdated Single Room Availability: "
                + inventory.getRoomAvailability().get("Single"));
    }
}

// ===== ROOM =====
abstract class Room {
    protected int numberOfBeds;
    protected int squareFeet;
    protected double pricePerNight;

    public Room(int beds, int size, double price) {
        numberOfBeds = beds;
        squareFeet = size;
        pricePerNight = price;
    }
}

class SingleRoom extends Room {
    public SingleRoom() { super(1, 250, 1500); }
}

class DoubleRoom extends Room {
    public DoubleRoom() { super(2, 400, 2500); }
}

class SuiteRoom extends Room {
    public SuiteRoom() { super(3, 750, 5000); }
}

// ===== INVENTORY =====
class RoomInventory {

    private Map<String, Integer> availability;

    public RoomInventory() {
        availability = new HashMap<>();
        availability.put("Single", 5);
        availability.put("Double", 3);
        availability.put("Suite", 2);
    }

    public Map<String, Integer> getRoomAvailability() {
        return availability;
    }
}

// ===== RESERVATION =====
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

// ===== QUEUE =====
class BookingRequestQueue {

    private Queue<Reservation> queue = new LinkedList<>();

    public void addRequest(Reservation r) {
        queue.offer(r);
    }

    public Reservation getNextRequest() {
        return queue.poll();
    }

    public boolean hasPendingRequests() {
        return !queue.isEmpty();
    }
}

// ===== ALLOCATION =====
class RoomAllocationService {

    private Map<String, Integer> counter = new HashMap<>();

    public String allocateRoom(Reservation r, RoomInventory inventory) {

        Map<String, Integer> map = inventory.getRoomAvailability();
        String type = r.getRoomType();

        if (map.get(type) > 0) {

            int count = counter.getOrDefault(type, 0) + 1;
            counter.put(type, count);

            String roomId = type + "-" + count;

            map.put(type, map.get(type) - 1);

            System.out.println("Booking confirmed for Guest: "
                    + r.getGuestName() + ", Room ID: " + roomId);

            return roomId;
        }

        return null;
    }
}

// ===== UC10 CANCELLATION =====
class CancellationService {

    private Stack<String> releasedRoomIds;
    private Map<String, String> reservationRoomTypeMap;

    public CancellationService() {
        releasedRoomIds = new Stack<>();
        reservationRoomTypeMap = new HashMap<>();
    }

    public void registerBooking(String reservationId, String roomType) {
        reservationRoomTypeMap.put(reservationId, roomType);
    }

    public void cancelBooking(String reservationId, RoomInventory inventory) {

        if (!reservationRoomTypeMap.containsKey(reservationId)) {
            System.out.println("Invalid cancellation request.");
            return;
        }

        String roomType = reservationRoomTypeMap.get(reservationId);

        // restore inventory
        Map<String, Integer> availability = inventory.getRoomAvailability();
        availability.put(roomType, availability.get(roomType) + 1);

        // track rollback
        releasedRoomIds.push(reservationId);

        // remove from active bookings
        reservationRoomTypeMap.remove(reservationId);

        System.out.println("Booking cancelled successfully. Inventory restored for room type: " + roomType);
    }

    public void showRollbackHistory() {

        System.out.println("\nRollback History (Most Recent First):");

        for (int i = releasedRoomIds.size() - 1; i >= 0; i--) {
            System.out.println("Released Reservation ID: " + releasedRoomIds.get(i));
        }
    }
}