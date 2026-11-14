// Inheritance: Tricycle extends Transport
public class Tricycle extends Transport {
    private static final double FIRST_KM = 2;      // base fare covers first 2 km
    private static final double EXTRA_PER_KM = 5.00;

    public Tricycle() {
        super("Tricycle", 20.00);
    }

    @Override
    public double computeFare(double distanceKm) {
        if (distanceKm <= FIRST_KM) {
            return getBaseFare();
        }
        return getBaseFare() + (distanceKm - FIRST_KM) * EXTRA_PER_KM;
    }
}
