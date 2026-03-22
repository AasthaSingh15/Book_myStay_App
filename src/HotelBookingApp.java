import java.util.*;

// ===== MAIN CLASS =====
public class HotelBookingApp {

    public static void main(String[] args) {

        // ===== UC1 =====
        System.out.println("Welcome to the Hotel Booking Management System");
        System.out.println("Version: 1.0");
        System.out.println("System initialized successfully\n");

        // ===== UC3 INVENTORY =====
        RoomInventory inventory = new RoomInventory();

        // ===== UC2 ROOM OBJECTS =====
        Room single = new SingleRoom();
        Room doubleRoom = new DoubleRoom();
        Room suite = new SuiteRoom();

        // ===== UC4 SEARCH =====
        System.out.println("Room Search\n");

        RoomSearchService searchService = new RoomSearchService();
        searchService.searchAvailableRooms(inventory, single, doubleRoom, suite);

        // ===== UC5 QUEUE =====
        BookingRequestQueue bookingQueue = new BookingRequestQueue();

        bookingQueue.addRequest(new Reservation("Abhi", "Single"));
        bookingQueue.addRequest(new Reservation("Subha", "Double"));
        bookingQueue.addRequest(new Reservation("Vanmathi", "Suite"));
        bookingQueue.addRequest(new Reservation("Kumar", "Single"));
        bookingQueue.addRequest(new Reservation("Ravi", "Single")); // overflow

        // ===== UC6 ALLOCATION =====
        System.out.println("\nRoom Allocation & Confirmation\n");

        RoomAllocationService allocationService = new RoomAllocationService();

        while (bookingQueue.hasPendingRequests()) {
            Reservation r = bookingQueue.getNextRequest();
            allocationService.allocateRoom(r, inventory);
        }
    }
}

// ===== UC2 ROOM =====
abstract class Room {
    protected int numberOfBeds;
    protected int squareFeet;
    protected double pricePerNight;

    public Room(int beds, int size, double price) {
        numberOfBeds = beds;
        squareFeet = size;
        pricePerNight = price;
    }

    public void displayRoomDetails() {
        System.out.println("Beds: " + numberOfBeds);
        System.out.println("Size: " + squareFeet + " sqft");
        System.out.println("Price per night: " + pricePerNight);
    }
}

// ===== ROOM TYPES =====
class SingleRoom extends Room {
    public SingleRoom() {
        super(1, 250, 1500);
    }
}

class DoubleRoom extends Room {
    public DoubleRoom() {
        super(2, 400, 2500);
    }
}

class SuiteRoom extends Room {
    public SuiteRoom() {
        super(3, 750, 5000);
    }
}

// ===== UC3 INVENTORY =====
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

// ===== UC4 SEARCH =====
class RoomSearchService {

    public void searchAvailableRooms(
            RoomInventory inventory,
            Room singleRoom,
            Room doubleRoom,
            Room suiteRoom) {

        Map<String, Integer> availability = inventory.getRoomAvailability();

        if (availability.get("Single") > 0) {
            System.out.println("Single Room:");
            singleRoom.displayRoomDetails();
            System.out.println("Available: " + availability.get("Single") + "\n");
        }

        if (availability.get("Double") > 0) {
            System.out.println("Double Room:");
            doubleRoom.displayRoomDetails();
            System.out.println("Available: " + availability.get("Double") + "\n");
        }

        if (availability.get("Suite") > 0) {
            System.out.println("Suite Room:");
            suiteRoom.displayRoomDetails();
            System.out.println("Available: " + availability.get("Suite"));
        }
    }
}

// ===== UC5 RESERVATION =====
class Reservation {
    private String guestName;
    private String roomType;

    public Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getRoomType() {
        return roomType;
    }
}

// ===== UC5 QUEUE =====
class BookingRequestQueue {

    private Queue<Reservation> requestQueue;

    public BookingRequestQueue() {
        requestQueue = new LinkedList<>();
    }

    public void addRequest(Reservation reservation) {
        requestQueue.offer(reservation);
    }

    public Reservation getNextRequest() {
        return requestQueue.poll();
    }

    public boolean hasPendingRequests() {
        return !requestQueue.isEmpty();
    }
}

// ===== UC6 ALLOCATION =====
class RoomAllocationService {

    private Set<String> allocatedRoomIds;
    private Map<String, Set<String>> assignedRoomsByType;

    public RoomAllocationService() {
        allocatedRoomIds = new HashSet<>();
        assignedRoomsByType = new HashMap<>();
    }

    public void allocateRoom(Reservation reservation, RoomInventory inventory) {

        String roomType = reservation.getRoomType();
        Map<String, Integer> availability = inventory.getRoomAvailability();

        if (availability.containsKey(roomType) && availability.get(roomType) > 0) {

            String roomId = generateRoomId(roomType);

            allocatedRoomIds.add(roomId);

            assignedRoomsByType
                    .computeIfAbsent(roomType, k -> new HashSet<>())
                    .add(roomId);

            availability.put(roomType, availability.get(roomType) - 1);

            System.out.println("Booking CONFIRMED for " + reservation.getGuestName());
            System.out.println("Room Type: " + roomType);
            System.out.println("Allocated Room ID: " + roomId);
            System.out.println("Remaining " + roomType + " rooms: "
                    + availability.get(roomType) + "\n");

        } else {

            System.out.println("Booking FAILED for " + reservation.getGuestName());
            System.out.println("Room Type: " + roomType);
            System.out.println("No rooms available\n");
        }
    }

    private String generateRoomId(String roomType) {
        String prefix = roomType.substring(0, 2).toUpperCase();
        String roomId;

        do {
            roomId = prefix + (int)(Math.random() * 1000);
        } while (allocatedRoomIds.contains(roomId));

        return roomId;
    }
}