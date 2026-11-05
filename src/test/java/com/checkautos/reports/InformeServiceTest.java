package com.checkautos.reports;

import com.checkautos.dto.InformeDetalleDTO;
import com.checkautos.models.Car;
import com.checkautos.models.Propietario;
import com.checkautos.repository.CarRepository;
import com.checkautos.repository.PropietarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InformeServiceTest {

    private CarRepository carRepository;
    private PropietarioRepository propietarioRepository;
    private InformeService informeService;

    @TempDir
    Path tempReportsDir;

    @BeforeEach
    void setUp() {
        carRepository = mock(CarRepository.class);
        propietarioRepository = mock(PropietarioRepository.class);
        informeService = new InformeService(carRepository, propietarioRepository, tempReportsDir.toString());
    }

    @Test
    @DisplayName("Debe generar archivo TXT y PDF con la información completa del auto, dueño y revisión")
    void testGenerarArchivosInforme() throws IOException {
        Car car = new Car();
        car.setId("car-1");
        car.setPlaca("ABC-123");
        car.setMarca("Mazda");
        car.setModelo("CX-30");
        car.setAño("2023");
        car.setColor("Rojo");
        car.setKilometraje("15000");
        car.setTransmision("Automática");
        car.setCombustible("Gasolina");
        car.setPrecio(85000000);
        car.setCedula("12345678");
        car.setPuntajeTecnico(90);
        car.setPuntajeMecanico(85);
        car.setObservacionRevision("Documentos al día");
        car.setObservacionMecanica("Frenos y motor en excelente estado");

        Propietario prop = new Propietario("Carlos Pérez", "12345678", "3111234567", "carlos@test.com");

        informeService.generarArchivosInforme(car, prop);

        Path txtFile = tempReportsDir.resolve("informe_ABC-123.txt");
        Path pdfFile = tempReportsDir.resolve("informe_ABC-123.pdf");

        assertTrue(Files.exists(txtFile), "El archivo TXT debe haber sido creado");
        assertTrue(Files.exists(pdfFile), "El archivo PDF debe haber sido creado");

        String txtContent = Files.readString(txtFile);
        assertTrue(txtContent.contains("ABC-123"));
        assertTrue(txtContent.contains("Mazda"));
        assertTrue(txtContent.contains("CX-30"));
        assertTrue(txtContent.contains("Carlos Pérez"));
        assertTrue(txtContent.contains("12345678"));
        assertTrue(txtContent.contains("3111234567"));
        assertTrue(txtContent.contains("carlos@test.com"));
        assertTrue(txtContent.contains("90 / 100"));
        assertTrue(txtContent.contains("85 / 100"));
        assertTrue(txtContent.contains("Documentos al día"));
        assertTrue(txtContent.contains("Frenos y motor en excelente estado"));

        assertTrue(Files.size(pdfFile) > 1000, "El archivo PDF debe tener contenido binario válido");
    }

    @Test
    @DisplayName("Debe consultar informe por placa y recuperar los datos consolidados")
    void testObtenerInformePorPlaca() {
        Car car = new Car();
        car.setId("car-2");
        car.setPlaca("XYZ-789");
        car.setMarca("Renault");
        car.setModelo("Duster");
        car.setCedula("87654321");
        car.setPuntajeTecnico(80);
        car.setPuntajeMecanico(80);

        Propietario prop = new Propietario("Ana Gomez", "87654321", "3009876543", "ana@test.com");

        when(carRepository.findByPlacaAndArchivadoFalse("XYZ-789")).thenReturn(Optional.of(car));
        when(propietarioRepository.findByCedula("87654321")).thenReturn(Optional.of(prop));

        // Consulta normalizada
        InformeDetalleDTO dto = informeService.obtenerInformePorPlaca("xyz789");

        assertNotNull(dto, "El informe no debe ser nulo");
        assertEquals("XYZ-789", dto.getPlaca());
        assertEquals("Renault", dto.getMarca());
        assertEquals("Ana Gomez", dto.getPropietarioNombre());
        assertEquals("3009876543", dto.getPropietarioTelefono());
        assertEquals("ana@test.com", dto.getPropietarioEmail());
        assertTrue(dto.isArchivoPdfExiste());
        assertTrue(dto.isArchivoTxtExiste());
    }

    @Test
    @DisplayName("Debe obtener bytes para descargar en formato PDF y TXT")
    void testObtenerBytesArchivo() throws IOException {
        Car car = new Car();
        car.setId("car-3");
        car.setPlaca("KLR-456");
        car.setMarca("Toyota");
        car.setModelo("Corolla");
        car.setCedula("55555");

        when(carRepository.findByPlacaAndArchivadoFalse("KLR-456")).thenReturn(Optional.of(car));

        byte[] pdfBytes = informeService.obtenerBytesArchivo("KLR-456", "pdf");
        byte[] txtBytes = informeService.obtenerBytesArchivo("KLR-456", "txt");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);

        assertNotNull(txtBytes);
        assertTrue(txtBytes.length > 100);
    }
}
