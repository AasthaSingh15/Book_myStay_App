import java.util.*;

// ===== MAIN CLASS =====
public class HotelBookingApp {

    public static void main(String[] args) {

        System.out.println("Hotel Booking System (UC1 → UC11)\n");

        // Shared resources
        RoomInventory inventory = new RoomInventory();
        BookingRequestQueue queue = new BookingRequestQueue();
        RoomAllocationService allocationService = new RoomAllocationService();
        CancellationService cancellationService = new CancellationService();

        // ===== ADD BOOKINGS =====
        queue.addRequest(new Reservation("Abhi", "Single"));
        queue.addRequest(new Reservation("Subha", "Double"));
        queue.addRequest(new Reservation("Vanmathi", "Suite"));
        queue.addRequest(new Reservation("Kural", "Single"));

        // ===== UC11 THREADS =====
        Thread t1 = new Thread(
                new ConcurrentBookingProcessor(queue, inventory, allocationService, cancellationService)
        );

        Thread t2 = new Thread(
                new ConcurrentBookingProcessor(queue, inventory, allocationService, cancellationService)
        );

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted");
        }

        // ===== UC10 CANCELLATION =====
        System.out.println("\nCancellation Demo:\n");

        cancellationService.cancelBooking("Single-1", inventory);

        cancellationService.showRollbackHistory();

        // ===== FINAL INVENTORY =====
        System.out.println("\nFinal Inventory:");
        Map<String, Integer> map = inventory.getRoomAvailability();

        System.out.println("Single: " + map.get("Single"));
        System.out.println("Double: " + map.get("Double"));
        System.out.println("Suite: " + map.get("Suite"));
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

    private Stack<String> releasedRoomIds = new Stack<>();
    private Map<String, String> reservationRoomTypeMap = new HashMap<>();

    public void registerBooking(String reservationId, String roomType) {
        reservationRoomTypeMap.put(reservationId, roomType);
    }

    public void cancelBooking(String reservationId, RoomInventory inventory) {

        if (!reservationRoomTypeMap.containsKey(reservationId)) {
            System.out.println("Invalid cancellation request.");
            return;
        }

        String roomType = reservationRoomTypeMap.get(reservationId);

        Map<String, Integer> availability = inventory.getRoomAvailability();
        availability.put(roomType, availability.get(roomType) + 1);

        releasedRoomIds.push(reservationId);
        reservationRoomTypeMap.remove(reservationId);

        System.out.println("Booking cancelled successfully for room: " + reservationId);
    }

    public void showRollbackHistory() {
        System.out.println("\nRollback History:");

        for (int i = releasedRoomIds.size() - 1; i >= 0; i--) {
            System.out.println(releasedRoomIds.get(i));
        }
    }
}

// ===== UC11 CONCURRENT PROCESSOR =====
class ConcurrentBookingProcessor implements Runnable {

    private BookingRequestQueue queue;
    private RoomInventory inventory;
    private RoomAllocationService allocationService;
    private CancellationService cancellationService;

    public ConcurrentBookingProcessor(
            BookingRequestQueue queue,
            RoomInventory inventory,
            RoomAllocationService allocationService,
            CancellationService cancellationService) {

        this.queue = queue;
        this.inventory = inventory;
        this.allocationService = allocationService;
        this.cancellationService = cancellationService;
    }

    @Override
    public void run() {

        while (true) {

            Reservation r;

            // 🔒 queue sync
            synchronized (queue) {
                if (!queue.hasPendingRequests()) break;
                r = queue.getNextRequest();
            }

            String roomId;

            // 🔒 inventory sync
            synchronized (inventory) {
                roomId = allocationService.allocateRoom(r, inventory);
            }

            if (roomId != null) {
                cancellationService.registerBooking(roomId, r.getRoomType());
            }
        }
    }
}