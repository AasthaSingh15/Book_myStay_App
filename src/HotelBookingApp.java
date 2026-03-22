import java.util.*;

// ===== MAIN CLASS =====
public class HotelBookingApp {

    public static void main(String[] args) {

        System.out.println("Booking Validation\n");

        Scanner scanner = new Scanner(System.in);

        RoomInventory inventory = new RoomInventory();
        ReservationValidator validator = new ReservationValidator();
        BookingRequestQueue queue = new BookingRequestQueue();

        try {
            System.out.print("Enter guest name: ");
            String name = scanner.nextLine();

            System.out.print("Enter room type (Single/Double/Suite): ");
            String type = scanner.nextLine();

            // ===== VALIDATION (UC9) =====
            validator.validate(name, type, inventory);

            // if valid → continue flow
            Reservation r = new Reservation(name, type);
            queue.addRequest(r);

            RoomAllocationService allocationService = new RoomAllocationService();
            BookingHistory history = new BookingHistory();

            while (queue.hasPendingRequests()) {
                Reservation res = queue.getNextRequest();
                String roomId = allocationService.allocateRoom(res, inventory);

                if (roomId != null) {
                    history.addReservation(res);
                }
            }

            System.out.println("\nBooking History Report\n");
            BookingReportService report = new BookingReportService();
            report.generateReport(history);

        } catch (InvalidBookingException e) {
            System.out.println("Booking failed: " + e.getMessage());
        } finally {
            scanner.close();
        }
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

// ===== UC8 HISTORY =====
class BookingHistory {

    private List<Reservation> list = new ArrayList<>();

    public void addReservation(Reservation r) {
        list.add(r);
    }

    public List<Reservation> getConfirmedReservations() {
        return list;
    }
}

// ===== UC8 REPORT =====
class BookingReportService {

    public void generateReport(BookingHistory history) {

        for (Reservation r : history.getConfirmedReservations()) {
            System.out.println("Guest: " + r.getGuestName()
                    + ", Room Type: " + r.getRoomType());
        }
    }
}

// ===== UC9 EXCEPTION =====
class InvalidBookingException extends Exception {

    public InvalidBookingException(String message) {
        super(message);
    }
}

// ===== UC9 VALIDATOR =====
class ReservationValidator {

    public void validate(String guestName,
                         String roomType,
                         RoomInventory inventory)
            throws InvalidBookingException {

        if (guestName == null || guestName.trim().isEmpty()) {
            throw new InvalidBookingException("Guest name cannot be empty.");
        }

        if (!roomType.equals("Single") &&
            !roomType.equals("Double") &&
            !roomType.equals("Suite")) {

            throw new InvalidBookingException("Invalid room type selected.");
        }

        Map<String, Integer> availability = inventory.getRoomAvailability();

        if (!availability.containsKey(roomType)) {
            throw new InvalidBookingException("Room type does not exist.");
        }

        if (availability.get(roomType) <= 0) {
            throw new InvalidBookingException("No rooms available.");
        }
    }
}