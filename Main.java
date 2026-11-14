import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static TripManager manager = new TripManager();

    public static void main(String[] args) {
        int choice;
        do {
            System.out.println();
            Ui.title("BIYAHEBUDGET");
            Ui.row("1. Compute and Save Trip");
            Ui.row("2. View Trip History");
            Ui.row("3. Set Budget");
            Ui.row("4. Check Budget");
            Ui.row("5. Exit");
            Ui.line();
            choice = (int) readNumber("Choose (1-5): ");

            switch (choice) {
                case 1: addTrip(); break;
                case 2: manager.viewTrips(); break;
                case 3: setBudget(); break;
                case 4: manager.checkBudget(); break;
                case 5: System.out.println("Thank you for using BiyaheBudget!"); break;
                default: System.out.println("Please choose 1 to 5 only.");
            }
        } while (choice != 5);
    }

    static void addTrip() {
        System.out.println();
        Ui.title("COMPUTE TRIP");
        System.out.print("Starting point (where everyone rode): ");
        String origin = sc.nextLine().trim();
        Transport transport = readTransport();

        // Ask first: Individual, Duo, or Group
        int r = readRideType();
        String rideType;
        int total;
        if (r == 1) {
            rideType = "Individual";
            total = 1;
        } else if (r == 2) {
            rideType = "Duo";
            total = 2;
        } else {
            rideType = "Group";
            total = readBetween("How many passengers in the group (3 or more)? ", 3, Integer.MAX_VALUE);
        }

        Trip trip = new Trip(origin, transport, rideType);

        // Keep asking for stops until everyone has gotten off
        int remaining = total;
        int stopNo = 1;
        while (remaining > 0) {
            System.out.println();
            Ui.row("STOP " + stopNo, remaining + " passenger(s) still on board");
            Ui.line();
            System.out.print("Place where they get off: ");
            String place = sc.nextLine().trim();
            double km = readPositive("Distance from the starting point (km): ");

            int count = 1;
            if (remaining > 1) {
                count = readBetween("How many passengers get off here? ", 1, remaining);
            }

            Stop stop = new Stop(place, km);
            for (int j = 1; j <= count; j++) {
                stop.addPassenger(readPassenger(total - remaining + j));
            }
            trip.addStop(stop);

            remaining -= count;
            stopNo++;
        }

        manager.addTrip(trip);
        trip.displayReceipt();
    }

    static int readRideType() {
        return readBetween("Ride type (1 = Individual, 2 = Duo, 3 = Group): ", 1, 3);
    }

    // Keeps asking until the user types a whole number from min to max
    static int readBetween(String message, int min, int max) {
        while (true) {
            double n = readNumber(message);
            if (n == Math.floor(n) && n >= min && n <= max) {
                return (int) n;
            }
            if (max == Integer.MAX_VALUE) {
                System.out.println("Please enter a whole number of " + min + " or more.");
            } else {
                System.out.println("Please enter a whole number from " + min + " to " + max + ".");
            }
        }
    }

    static Transport readTransport() {
        while (true) {
            int t = (int) readNumber("Transport (1 = Jeepney, 2 = Tricycle): ");
            if (t == 1) {
                return new Jeepney();
            } else if (t == 2) {
                return new Tricycle();
            }
            System.out.println("Please choose 1 or 2 only.");
        }
    }

    static Passenger readPassenger(int number) {
        while (true) {
            int p = (int) readNumber("Passenger " + number + " (1 = Regular, 2 = Student): ");
            switch (p) {
                case 1: return new RegularPassenger();
                case 2: return new StudentPassenger();
                default: System.out.println("Please choose 1 or 2 only.");
            }
        }
    }

    static void setBudget() {
        try {
            System.out.println();
            Ui.title("SET BUDGET");
            manager.setBudget(readNumber("Enter your budget (PHP): "));
            System.out.println("Budget saved.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    // Keeps asking until the number is more than 0
    static double readPositive(String message) {
        while (true) {
            double n = readNumber(message);
            if (n > 0) {
                return n;
            }
            System.out.println("The number must be more than 0.");
        }
    }

    // Keeps asking until the user types a valid number (Exception Handling)
    static double readNumber(String message) {
        while (true) {
            System.out.print(message);
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter numbers only.");
            }
        }
    }
}
