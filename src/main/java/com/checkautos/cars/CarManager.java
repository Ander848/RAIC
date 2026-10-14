package com.checkautos.cars;

import com.checkautos.models.Car;
import com.checkautos.models.Propietario;
import com.checkautos.repository.CarRepository;
import com.checkautos.repository.PropietarioRepository;
import com.checkautos.utils.AppUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarManager {

    private final CarRepository        carRepository;
    private final PropietarioRepository propietarioRepository;
    private Car currentCar;

    public CarManager(CarRepository carRepository, PropietarioRepository propietarioRepository) {
        this.carRepository         = carRepository;
        this.propietarioRepository = propietarioRepository;
        this.currentCar            = new Car();
    }

    public void startNewCarRegistration() { this.currentCar = new Car(); }
    public Car  getCurrentCar()           { return currentCar; }
    public void clearCurrentCar()         { startNewCarRegistration(); }

    /**
     * Guarda un auto de forma atómica y sin estado (stateless REST), con validación
     * de placa particular (ABC-123), detección de duplicados activos y auditoría.
     */
    public Car createAndSaveCar(Car car) {
        if (!validateCarData(car)) {
            throw new IllegalArgumentException("Marca, modelo y placa son campos obligatorios");
        }

        // Validación de no negatividad en valores numéricos
        if (car.getPrecio() < 0) {
            throw new IllegalArgumentException("El precio del vehículo no puede ser negativo");
        }
        if (car.getPuntajeTecnico() < 0 || car.getPuntajeMecanico() < 0) {
            throw new IllegalArgumentException("Los puntajes de revisión no pueden ser negativos");
        }
        if (car.getKilometraje() != null && !car.getKilometraje().isBlank()) {
            try {
                double km = Double.parseDouble(car.getKilometraje().trim().replaceAll("[^\\d.-]", ""));
                if (km < 0) {
                    throw new IllegalArgumentException("El kilometraje no puede ser negativo");
                }
            } catch (NumberFormatException ignored) {}
        }
        if (car.getAño() != null && !car.getAño().isBlank()) {
            try {
                int anio = Integer.parseInt(car.getAño().trim());
                if (anio < 0) {
                    throw new IllegalArgumentException("El año del vehículo no puede ser negativo");
                }
            } catch (NumberFormatException ignored) {}
        }

        // Validación y normalización estricta de placa (Condición 2: autos particulares 3 letras + 3 números)
        if (!AppUtils.isValidPlate(car.getPlaca())) {
            throw new IllegalArgumentException("Formato de placa inválido. Debe contener 3 letras y 3 números (ejemplo: ABC-123)");
        }
        String placaNormalizada = AppUtils.formatPlate(car.getPlaca());
        car.setPlaca(placaNormalizada);

        // Validación amigable de placa duplicada activa (Condición 3)
        Optional<Car> existenteConPlaca = carRepository.findByPlacaAndArchivadoFalse(placaNormalizada);
        if (existenteConPlaca.isPresent()) {
            Car otro = existenteConPlaca.get();
            if (car.getId() == null || !car.getId().equals(otro.getId())) {
                throw new IllegalArgumentException("Ya existe un auto activo registrado con la placa " + placaNormalizada);
            }
        }

        // Auditoría de fechas (Condición 4)
        LocalDateTime ahora = LocalDateTime.now();
        if (car.getFechaCreacion() == null) {
            car.setFechaCreacion(ahora);
        }
        car.setFechaModificacion(ahora);

        if (car.getCedula() != null && !car.getCedula().isBlank()) {
            String cedula = car.getCedula().trim();
            Optional<Propietario> existente = propietarioRepository.findByCedula(cedula);

            Propietario p;
            if (existente.isPresent()) {
                p = existente.get();
                if (car.getPropietario() != null && !car.getPropietario().isBlank()) {
                    p.setNombre(car.getPropietario().trim());
                }
            } else {
                p = new Propietario(
                    car.getPropietario() != null ? car.getPropietario().trim() : "",
                    cedula,
                    null,
                    null
                );
            }
            propietarioRepository.save(p);
            car.setPropietarioId(p.getId());
        }

        return carRepository.save(car);
    }

    public boolean saveAndRegisterCar() {
        if (!validateCarData(currentCar)) return false;
        createAndSaveCar(currentCar);
        startNewCarRegistration();
        return true;
    }

    private boolean validateCarData(Car car) {
        return car.getMarca()  != null && !car.getMarca().isBlank() &&
               car.getModelo() != null && !car.getModelo().isBlank() &&
               car.getPlaca()  != null && !car.getPlaca().isBlank();
    }


    public List<Car> getAllCars() {
        return carRepository.findByArchivadoFalseOrderByIdDesc();
    }

    public Car getCarById(String id) {
        return carRepository.findById(id).orElse(null);
    }

    public List<Car> searchCars(String term) {
        return carRepository
            .findByArchivadoFalseAndMarcaContainingIgnoreCaseOrArchivadoFalseAndModeloContainingIgnoreCaseOrArchivadoFalseAndPlacaContainingIgnoreCase(
                term, term, term);
    }


    public boolean changeCarStatus(String carId, String newStatus) {
        Optional<Car> found = carRepository.findById(carId);
        if (found.isEmpty()) return false;
        Car car = found.get();
        car.setEstado(newStatus);
        car.setFechaModificacion(LocalDateTime.now());
        carRepository.save(car);
        return true;
    }


    public boolean archivarAuto(String carId) {
        Optional<Car> found = carRepository.findById(carId);
        if (found.isEmpty()) return false;
        Car car = found.get();
        car.setArchivado(true);
        car.setFechaModificacion(LocalDateTime.now());
        carRepository.save(car);
        return true;
    }


    public boolean restaurarAuto(String carId) {
        Optional<Car> found = carRepository.findById(carId);
        if (found.isEmpty()) return false;
        Car car = found.get();
        car.setArchivado(false);
        car.setFechaModificacion(LocalDateTime.now());
        carRepository.save(car);
        return true;
    }


    public List<Car> getAutosArchivados() {
        return carRepository.findByArchivadoTrueOrderByIdDesc();
    }


    public List<Car> getAutosPorPropietario(String propietarioId) {
        return carRepository.findByPropietarioIdAndArchivadoFalse(propietarioId);
    }

    public int getTotalCars()      { return (int) carRepository.countByArchivadoFalse(); }
    public int getAvailableCars()  { return (int) carRepository.countByEstadoAndArchivadoFalse("Disponible"); }
    public int getSoldCars()       { return (int) carRepository.countByEstadoAndArchivadoFalse("Vendido"); }
    public int getCarsInReview()   { return (int) carRepository.countByEstadoAndArchivadoFalse("En revisión"); }

    public List<Car> getRecentCars(int limit) {
        List<Car> all = carRepository.findByArchivadoFalseOrderByIdDesc();
        return all.subList(0, Math.min(limit, all.size()));
    }
}