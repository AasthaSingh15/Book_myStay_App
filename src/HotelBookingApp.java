import java.io.*;
import java.util.*;

// ==========================
// MAIN CLASS
// ==========================
public class HotelBookingApp {

    public static void main(String[] args) {

        System.out.println("Hotel Booking System (UC1 → UC12)\n");

        String filePath = "inventory.txt";

        RoomInventory inventory = new RoomInventory();
        BookingRequestQueue queue = new BookingRequestQueue();
        RoomAllocationService allocation = new RoomAllocationService();
        CancellationService cancelService = new CancellationService();
        FilePersistenceService persistence = new FilePersistenceService();

        // ===== UC12 LOAD =====
        persistence.loadInventory(inventory, filePath);

        // ===== UC5 QUEUE =====
        queue.addRequest(new Reservation("Abhi", "Single"));
        queue.addRequest(new Reservation("Subha", "Double"));
        queue.addRequest(new Reservation("Vanmathi", "Suite"));
        queue.addRequest(new Reservation("Kural", "Single"));

        // ===== UC11 THREADS =====
        Thread t1 = new Thread(new ConcurrentBookingProcessor(queue, inventory, allocation, cancelService));
        Thread t2 = new Thread(new ConcurrentBookingProcessor(queue, inventory, allocation, cancelService));

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }

        // ===== UC10 CANCELLATION =====
        System.out.println("\nCancellation Demo:");
        cancelService.cancelBooking("Single-1", inventory);

        cancelService.showRollbackHistory();

        // ===== FINAL INVENTORY =====
        System.out.println("\nFinal Inventory:");
        for (Map.Entry<String, Integer> entry : inventory.getRoomAvailability().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // ===== UC12 SAVE =====
        persistence.saveInventory(inventory, filePath);
    }
}

// ==========================
// ROOM (UC2)
// ==========================
abstract class Room {
    protected int beds;
    protected int size;
    protected double price;

    public Room(int beds, int size, double price) {
        this.beds = beds;
        this.size = size;
        this.price = price;
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

// ==========================
// INVENTORY (UC3)
// ==========================
class RoomInventory {
    private Map<String, Integer> roomAvailability;

    public RoomInventory() {
        roomAvailability = new HashMap<>();
        roomAvailability.put("Single", 5);
        roomAvailability.put("Double", 3);
        roomAvailability.put("Suite", 2);
    }

    public Map<String, Integer> getRoomAvailability() {
        return roomAvailability;
    }

    public void updateAvailability(String type, int count) {
        roomAvailability.put(type, count);
    }
}

// ==========================
// RESERVATION (UC5)
// ==========================
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

// ==========================
// QUEUE (UC5)
// ==========================
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

// ==========================
// ALLOCATION (UC6)
// ==========================
class RoomAllocationService {

    private Map<String, Integer> counter = new HashMap<>();

    public String allocateRoom(Reservation r, RoomInventory inventory) {

        String type = r.getRoomType();
        Map<String, Integer> map = inventory.getRoomAvailability();

        if (map.get(type) > 0) {

            int count = counter.getOrDefault(type, 0) + 1;
            counter.put(type, count);

            String roomId = type + "-" + count;

            map.put(type, map.get(type) - 1);

            System.out.println("Booked: " + r.getGuestName() + " → " + roomId);

            return roomId;
        }

        System.out.println("No rooms available for " + type);
        return null;
    }
}

// ==========================
// CANCELLATION (UC10)
// ==========================
class CancellationService {

    private Stack<String> stack = new Stack<>();
    private Map<String, String> map = new HashMap<>();

    public void registerBooking(String id, String type) {
        map.put(id, type);
    }

    public void cancelBooking(String id, RoomInventory inventory) {

        if (!map.containsKey(id)) {
            System.out.println("Invalid cancellation");
            return;
        }

        String type = map.get(id);

        Map<String, Integer> inv = inventory.getRoomAvailability();
        inv.put(type, inv.get(type) + 1);

        stack.push(id);
        map.remove(id);

        System.out.println("Cancelled: " + id);
    }

    public void showRollbackHistory() {
        System.out.println("\nRollback History:");
        for (int i = stack.size() - 1; i >= 0; i--) {
            System.out.println(stack.get(i));
        }
    }
}

// ==========================
// CONCURRENCY (UC11)
// ==========================
class ConcurrentBookingProcessor implements Runnable {

    private BookingRequestQueue queue;
    private RoomInventory inventory;
    private RoomAllocationService allocation;
    private CancellationService cancel;

    public ConcurrentBookingProcessor(
            BookingRequestQueue q,
            RoomInventory i,
            RoomAllocationService a,
            CancellationService c) {

        queue = q;
        inventory = i;
        allocation = a;
        cancel = c;
    }

    @Override
    public void run() {

        while (true) {

            Reservation r;

            synchronized (queue) {
                if (!queue.hasPendingRequests()) break;
                r = queue.getNextRequest();
            }

            String id;

            synchronized (inventory) {
                id = allocation.allocateRoom(r, inventory);
            }

            if (id != null) {
                cancel.registerBooking(id, r.getRoomType());
            }
        }
    }
}

// ==========================
// PERSISTENCE (UC12)
// ==========================
class FilePersistenceService {

    public void saveInventory(RoomInventory inventory, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {

            for (Map.Entry<String, Integer> e : inventory.getRoomAvailability().entrySet()) {
                writer.write(e.getKey() + "=" + e.getValue());
                writer.newLine();
            }

            System.out.println("\nInventory saved.");

        } catch (IOException e) {
            System.out.println("Save error");
        }
    }

    public void loadInventory(RoomInventory inventory, String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("No saved data.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = reader.readLine()) != null) {

                String[] p = line.split("=");
                inventory.updateAvailability(p[0], Integer.parseInt(p[1]));
            }

            System.out.println("Inventory restored.");

        } catch (Exception e) {
            System.out.println("Load error");
        }
    }
}