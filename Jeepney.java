// Inheritance: Jeepney extends Transport
public class Jeepney extends Transport {
    private static final double FIRST_KM = 4;      // base fare covers first 4 km
    private static final double EXTRA_PER_KM = 1.80;

    public Jeepney() {
        super("Jeepney", 13.00);
    }

    @Override
    public double computeFare(double distanceKm) {
        if (distanceKm <= FIRST_KM) {
            return getBaseFare();
        }
        return getBaseFare() + (distanceKm - FIRST_KM) * EXTRA_PER_KM;
    }
}
