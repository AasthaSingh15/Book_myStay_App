import java.util.*;

// ===== MAIN CLASS =====
public class HotelBookingApp {

    public static void main(String[] args) {

        System.out.println("Welcome to the Hotel Booking Management System\n");

        RoomInventory inventory = new RoomInventory();

        Room single = new SingleRoom();
        Room doubleRoom = new DoubleRoom();
        Room suite = new SuiteRoom();

        // ===== UC4 SEARCH =====
        RoomSearchService searchService = new RoomSearchService();
        searchService.searchAvailableRooms(inventory, single, doubleRoom, suite);

        // ===== UC5 QUEUE =====
        BookingRequestQueue queue = new BookingRequestQueue();
        queue.addRequest(new Reservation("Abhi", "Single"));
        queue.addRequest(new Reservation("Subha", "Double"));
        queue.addRequest(new Reservation("Vanmathi", "Suite"));

        // ===== UC6 ALLOCATION =====
        RoomAllocationService allocationService = new RoomAllocationService();

        // ===== UC8 HISTORY =====
        BookingHistory history = new BookingHistory();

        System.out.println("\nRoom Allocation:\n");

        while (queue.hasPendingRequests()) {
            Reservation r = queue.getNextRequest();
            String roomId = allocationService.allocateRoom(r, inventory);

            if (roomId != null) {
                history.addReservation(r); // store confirmed booking
            }
        }

        // ===== UC8 REPORT =====
        System.out.println("\nBooking History and Reporting\n");

        BookingReportService reportService = new BookingReportService();
        reportService.generateReport(history);
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

    public void displayRoomDetails() {
        System.out.println("Beds: " + numberOfBeds);
        System.out.println("Size: " + squareFeet + " sqft");
        System.out.println("Price per night: " + pricePerNight);
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

    public void updateAvailability(String type, int count) {
        availability.put(type, count);
    }
}

// ===== SEARCH =====
class RoomSearchService {

    public void searchAvailableRooms(RoomInventory inventory,
                                     Room single,
                                     Room doubleRoom,
                                     Room suite) {

        Map<String, Integer> map = inventory.getRoomAvailability();

        System.out.println("Room Search\n");

        if (map.get("Single") > 0) {
            System.out.println("Single Room:");
            single.displayRoomDetails();
            System.out.println("Available: " + map.get("Single") + "\n");
        }

        if (map.get("Double") > 0) {
            System.out.println("Double Room:");
            doubleRoom.displayRoomDetails();
            System.out.println("Available: " + map.get("Double") + "\n");
        }

        if (map.get("Suite") > 0) {
            System.out.println("Suite Room:");
            suite.displayRoomDetails();
            System.out.println("Available: " + map.get("Suite"));
        }
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

    private Set<String> allocatedIds = new HashSet<>();
    private Map<String, Integer> counters = new HashMap<>();

    public String allocateRoom(Reservation r, RoomInventory inventory) {

        String type = r.getRoomType();
        Map<String, Integer> map = inventory.getRoomAvailability();

        if (map.get(type) > 0) {

            String id = generateRoomId(type);

            allocatedIds.add(id);
            map.put(type, map.get(type) - 1);

            System.out.println("Booking confirmed for Guest: "
                    + r.getGuestName() + ", Room ID: " + id);

            return id;
        } else {
            System.out.println("Booking failed for " + r.getGuestName());
            return null;
        }
    }

    private String generateRoomId(String type) {
        int count = counters.getOrDefault(type, 0) + 1;
        counters.put(type, count);
        return type + "-" + count;
    }
}

// ===== UC7 ADD-ON (optional kept) =====
class AddOnService {
    String name;
    double price;

    public AddOnService(String name, double price) {
        this.name = name;
        this.price = price;
    }
}

// ===== UC8 BOOKING HISTORY =====
class BookingHistory {

    private List<Reservation> confirmedReservations;

    public BookingHistory() {
        confirmedReservations = new ArrayList<>();
    }

    public void addReservation(Reservation reservation) {
        confirmedReservations.add(reservation);
    }

    public List<Reservation> getConfirmedReservations() {
        return confirmedReservations;
    }
}

// ===== UC8 REPORT =====
class BookingReportService {

    public void generateReport(BookingHistory history) {

        System.out.println("Booking History Report\n");

        for (Reservation r : history.getConfirmedReservations()) {
            System.out.println("Guest: " + r.getGuestName()
                    + ", Room Type: " + r.getRoomType());
        }
    }
}