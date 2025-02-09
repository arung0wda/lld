package carrentalsystem;

import carrentalsystem.payment.CreditCardPaymentProcessor;
import carrentalsystem.payment.PaymentProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RentalSystem {
    private static RentalSystem instance;
    private final Map<String, Car> cars;
    private final Map<String, Reservation> reservations;
    private final PaymentProcessor paymentProcessor;

    private RentalSystem() {
        cars = new ConcurrentHashMap<>();
        reservations = new ConcurrentHashMap<>();
        paymentProcessor = new CreditCardPaymentProcessor();
    }

    public static synchronized RentalSystem getInstance() {
        if (instance == null) {
            instance = new RentalSystem();
        }
        return instance;
    }

    public void addCar(Car car) {
        cars.put(car.getLicensePlate(), car);
    }

    public void removeCar(String licensePlate) {
        cars.remove(licensePlate);
    }

    public List<Car> searchCars(String make, String model, LocalDate startDate, LocalDate endDate) {
        return cars.values().stream()
                .filter(car -> car.getMake().equalsIgnoreCase(make))
                .filter(car -> car.getModel().equalsIgnoreCase(model))
                .filter(Car::isAvailable)
                .toList();
    }

    private boolean isCarAvailable(Car car, LocalDate startDate, LocalDate endDate) {
        for (Reservation reservation : reservations.values()) {
            if (reservation.getCar().equals(car)) {
                if (startDate.isBefore(reservation.getEndDate()) && endDate.isAfter(reservation.getStartDate())) {
                    return false;
                }
            }
        }
        return true;
    }

    public synchronized Reservation makeReservation(Customer customer, Car car, LocalDate startDate, LocalDate endDate) {
        if (isCarAvailable(car, startDate, endDate)) {
            String reservationId = generateReservationId();
            Reservation reservation = new Reservation(reservationId, customer, car, startDate, endDate);
            reservations.put(reservationId, reservation);
            car.setAvailable(false);
            return reservation;
        }
        return null;
    }

    public synchronized void cancelReservation(String reservationId) {
        Reservation reservation = reservations.remove(reservationId);
        if (reservation != null) {
            reservation.getCar().setAvailable(true);
        }
    }

    public boolean processPayment(Reservation reservation) {
        return paymentProcessor.processPayment(reservation.getTotalPrice());
    }

    private String generateReservationId() {
        return "RES" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
