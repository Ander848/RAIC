package com.checkautos.cars;

import com.checkautos.models.Car;
import com.checkautos.repository.CarRepository;
import com.checkautos.repository.PropietarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarManagerTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private PropietarioRepository propietarioRepository;

    private CarManager carManager;

    @BeforeEach
    void setUp() {
        carManager = new CarManager(carRepository, propietarioRepository);
    }

    @Test
    @DisplayName("Debe rechazar placas inválidas que no correspondan a autos particulares (3 letras + 3 números)")
    void testRechazoPlacasInvalidas() {
        // Formato moto o longitud incorrecta
        String[] placasInvalidas = {"AB123", "ABCD12", "123ABC", "ABC12D", "ABC-1234", "12-34", ""};

        for (String p : placasInvalidas) {
            Car car = new Car();
            car.setMarca("Mazda");
            car.setModelo("CX-30");
            car.setPlaca(p);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                carManager.createAndSaveCar(car);
            }, "Debe lanzar excepción para placa inválida: " + p);

            assertTrue(ex.getMessage().contains("placa") || ex.getMessage().contains("obligatorios"));
        }
    }

    @Test
    @DisplayName("Debe normalizar placa válida de auto al formato estándar ABC-123")
    void testNormalizacionPlacaValida() {
        Car car = new Car();
        car.setMarca("Toyota");
        car.setModelo("Corolla");
        car.setPlaca("xyz456");

        when(carRepository.findByPlacaAndArchivadoFalse("XYZ-456")).thenReturn(Optional.empty());
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Car saved = carManager.createAndSaveCar(car);

        assertNotNull(saved);
        assertEquals("XYZ-456", saved.getPlaca(), "La placa debe normalizarse a mayúsculas con guión");
    }

    @Test
    @DisplayName("Debe rechazar registro si ya existe un auto activo con la misma placa")
    void testRechazoPlacaDuplicadaActiva() {
        Car carNuevo = new Car();
        carNuevo.setMarca("Renault");
        carNuevo.setModelo("Duster");
        carNuevo.setPlaca("KLR-890");

        Car carExistente = new Car();
        carExistente.setId("id_existente_1");
        carExistente.setPlaca("KLR-890");
        carExistente.setArchivado(false);

        when(carRepository.findByPlacaAndArchivadoFalse("KLR-890")).thenReturn(Optional.of(carExistente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(carNuevo);
        });

        assertTrue(ex.getMessage().contains("Ya existe un auto activo registrado con la placa KLR-890"));
        verify(carRepository, never()).save(carNuevo);
    }

    @Test
    @DisplayName("Debe registrar fechaCreacion y fechaModificacion al crear un auto nuevo")
    void testAuditoriaFechasCreacion() {
        Car car = new Car();
        car.setMarca("Chevrolet");
        car.setModelo("Onix");
        car.setPlaca("ONX-123");

        when(carRepository.findByPlacaAndArchivadoFalse("ONX-123")).thenReturn(Optional.empty());
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        Car saved = carManager.createAndSaveCar(car);
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertNotNull(saved.getFechaCreacion(), "fechaCreacion no debe ser nula");
        assertNotNull(saved.getFechaModificacion(), "fechaModificacion no debe ser nula");
        assertTrue(saved.getFechaCreacion().isAfter(antes) && saved.getFechaCreacion().isBefore(despues));
        assertTrue(saved.getFechaModificacion().isAfter(antes) && saved.getFechaModificacion().isBefore(despues));
    }

    @Test
    @DisplayName("Debe actualizar fechaModificacion al cambiar estado de un auto")
    void testAuditoriaFechaAlCambiarEstado() {
        Car car = new Car();
        car.setId("auto-123");
        car.setEstado("Disponible");
        LocalDateTime fechaVieja = LocalDateTime.now().minusDays(2);
        car.setFechaCreacion(fechaVieja);
        car.setFechaModificacion(fechaVieja);

        when(carRepository.findById("auto-123")).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean resultado = carManager.changeCarStatus("auto-123", "Vendido");

        assertTrue(resultado);
        assertEquals("Vendido", car.getEstado());
        assertTrue(car.getFechaModificacion().isAfter(fechaVieja), "fechaModificacion debe ser actualizada al momento actual");
    }

    @Test
    @DisplayName("Debe actualizar fechaModificacion al archivar un auto")
    void testAuditoriaFechaAlArchivar() {
        Car car = new Car();
        car.setId("auto-arch");
        car.setArchivado(false);
        LocalDateTime fechaVieja = LocalDateTime.now().minusDays(5);
        car.setFechaModificacion(fechaVieja);

        when(carRepository.findById("auto-arch")).thenReturn(Optional.of(car));
        when(carRepository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean resultado = carManager.archivarAuto("auto-arch");

        assertTrue(resultado);
        assertTrue(car.isArchivado());
        assertTrue(car.getFechaModificacion().isAfter(fechaVieja));
    }

    @Test
    @DisplayName("Debe rechazar registros con precio negativo")
    void testRechazoPrecioNegativo() {
        Car car = new Car();
        car.setMarca("Kia");
        car.setModelo("Rio");
        car.setPlaca("KIA-123");
        car.setPrecio(-5_000_000);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(car);
        });
        assertTrue(ex.getMessage().contains("precio"));
    }

    @Test
    @DisplayName("Debe rechazar registros con puntajes técnicos o mecánicos negativos")
    void testRechazoPuntajesNegativos() {
        Car car1 = new Car();
        car1.setMarca("Kia");
        car1.setModelo("Rio");
        car1.setPlaca("KIA-123");
        car1.setPuntajeTecnico(-10);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(car1);
        });
        assertTrue(ex1.getMessage().contains("puntajes"));

        Car car2 = new Car();
        car2.setMarca("Kia");
        car2.setModelo("Rio");
        car2.setPlaca("KIA-123");
        car2.setPuntajeMecanico(-5);

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(car2);
        });
        assertTrue(ex2.getMessage().contains("puntajes"));
    }

    @Test
    @DisplayName("Debe rechazar registros con kilometraje negativo")
    void testRechazoKilometrajeNegativo() {
        Car car = new Car();
        car.setMarca("Kia");
        car.setModelo("Rio");
        car.setPlaca("KIA-123");
        car.setKilometraje("-15000");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(car);
        });
        assertTrue(ex.getMessage().contains("kilometraje"));
    }

    @Test
    @DisplayName("Debe rechazar registros con año negativo")
    void testRechazoAnioNegativo() {
        Car car = new Car();
        car.setMarca("Kia");
        car.setModelo("Rio");
        car.setPlaca("KIA-123");
        car.setAño("-2022");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            carManager.createAndSaveCar(car);
        });
        assertTrue(ex.getMessage().contains("año"));
    }
}
