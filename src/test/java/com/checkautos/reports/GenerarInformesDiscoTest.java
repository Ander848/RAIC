package com.checkautos.reports;

import com.checkautos.models.Car;
import com.checkautos.models.Propietario;
import com.checkautos.repository.CarRepository;
import com.checkautos.repository.PropietarioRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.mock;

public class GenerarInformesDiscoTest {

    @Test
    public void regenerarTodosLosInformesEnDisco() throws IOException {
        Path informesDir = Paths.get("data", "informes");
        if (!Files.exists(informesDir)) {
            return;
        }

        CarRepository carRepo = mock(CarRepository.class);
        PropietarioRepository propRepo = mock(PropietarioRepository.class);
        InformeService service = new InformeService(carRepo, propRepo, informesDir.toString());

        File[] txtFiles = informesDir.toFile().listFiles((dir, name) -> name.startsWith("informe_") && name.endsWith(".txt"));
        if (txtFiles == null) return;

        for (File txtFile : txtFiles) {
            String content = Files.readString(txtFile.toPath());
            Car car = new Car();
            car.setPlaca(extraerCampo(content, "Placa\\s*:\\s*(.+)"));
            car.setMarca(extraerCampo(content, "Marca\\s*:\\s*(.+)"));
            car.setModelo(extraerCampo(content, "Modelo\\s*:\\s*(.+)"));
            car.setAño(extraerCampo(content, "Año de Fabricación\\s*:\\s*(.+)"));
            car.setColor(extraerCampo(content, "Color\\s*:\\s*(.+)"));

            String km = extraerCampo(content, "Kilometraje\\s*:\\s*([0-9]+)");
            car.setKilometraje(km != null ? km : "0");

            car.setTransmision(extraerCampo(content, "Transmisión\\s*:\\s*(.+)"));
            car.setCombustible(extraerCampo(content, "Combustible\\s*:\\s*(.+)"));

            String precioStr = extraerCampo(content, "Precio Valuado\\s*:\\s*\\$\\s*([0-9\\.]+)\\s*COP");
            if (precioStr != null) {
                precioStr = precioStr.replaceAll("\\.", "");
                try {
                    car.setPrecio((int) Double.parseDouble(precioStr));
                } catch (Exception ignored) {}
            }

            String puntajeTec = extraerCampo(content, "Puntaje Revisión Técnica\\s*:\\s*([0-9]+)");
            if (puntajeTec != null) {
                try { car.setPuntajeTecnico(Integer.parseInt(puntajeTec)); } catch (Exception ignored) {}
            }

            String puntajeMec = extraerCampo(content, "Puntaje Revisión Mecánica\\s*:\\s*([0-9]+)");
            if (puntajeMec != null) {
                try { car.setPuntajeMecanico(Integer.parseInt(puntajeMec)); } catch (Exception ignored) {}
            }

            Propietario prop = new Propietario();
            prop.setNombre(extraerCampo(content, "Nombre Completo\\s*:\\s*(.+)"));
            prop.setCedula(extraerCampo(content, "Cédula / Documento\\s*:\\s*(.+)"));
            prop.setTelefono(extraerCampo(content, "Teléfono de Contacto\\s*:\\s*(.+)"));
            prop.setEmail(extraerCampo(content, "Correo Electrónico\\s*:\\s*(.+)"));

            car.setCedula(prop.getCedula());
            car.setPropietario(prop.getNombre());

            if (car.getPlaca() != null && !car.getPlaca().isBlank()) {
                System.out.println("Regenerando informe para: " + car.getPlaca());
                service.generarArchivosInforme(car, prop);
            }
        }
    }

    private String extraerCampo(String content, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
