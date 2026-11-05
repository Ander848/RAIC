package com.checkautos.reports;

import com.checkautos.dto.InformeDetalleDTO;
import com.checkautos.models.Car;
import com.checkautos.models.Propietario;
import com.checkautos.repository.CarRepository;
import com.checkautos.repository.PropietarioRepository;
import com.checkautos.utils.AppUtils;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@Service
public class InformeService {

    private static final Logger log = LoggerFactory.getLogger(InformeService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale LOCALE_CO = new Locale("es", "CO");

    private final CarRepository carRepository;
    private final PropietarioRepository propietarioRepository;
    private final Path reportsDirectory;

    public InformeService(CarRepository carRepository,
                          PropietarioRepository propietarioRepository,
                          @Value("${app.informes.dir:data/informes}") String dirName) {
        this.carRepository = carRepository;
        this.propietarioRepository = propietarioRepository;
        this.reportsDirectory = Paths.get(dirName).toAbsolutePath().normalize();
        inicializarDirectorio();
    }

    private void inicializarDirectorio() {
        try {
            if (!Files.exists(reportsDirectory)) {
                Files.createDirectories(reportsDirectory);
                log.info("Directorio de informes inicializado en: {}", reportsDirectory);
            }
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de informes: {}", reportsDirectory, e);
        }
    }

    public Path getReportsDirectory() {
        return reportsDirectory;
    }

    /**
     * Genera tanto el archivo TXT como el archivo PDF del informe vehicular.
     */
    public synchronized void generarArchivosInforme(Car car, Propietario propietario) {
        if (car == null || car.getPlaca() == null || car.getPlaca().isBlank()) {
            log.warn("No se puede generar informe para un auto sin placa");
            return;
        }

        inicializarDirectorio();

        Propietario prop = propietario;
        if (prop == null && car.getCedula() != null && !car.getCedula().isBlank()) {
            prop = propietarioRepository.findByCedula(car.getCedula().trim()).orElse(null);
        }

        try {
            generarArchivoTxt(car, prop);
            generarArchivoPdf(car, prop);
            log.info("Archivos de informe (PDF y TXT) generados con éxito para placa {}", car.getPlaca());
        } catch (Exception e) {
            log.error("Error al generar archivos de informe para placa {}", car.getPlaca(), e);
        }
    }

    /**
     * Genera el archivo en texto plano (.txt) con la ficha completa del vehículo y dueño.
     */
    public Path generarArchivoTxt(Car car, Propietario prop) throws IOException {
        String placaLimpia = sanitizarPlacaParaArchivo(car.getPlaca());
        Path txtPath = reportsDirectory.resolve("informe_" + placaLimpia + ".txt");

        String nombreDueño = prop != null && prop.getNombre() != null ? prop.getNombre() :
                (car.getPropietario() != null ? car.getPropietario() : "No registrado");
        String cedulaDueño = prop != null && prop.getCedula() != null ? prop.getCedula() :
                (car.getCedula() != null ? car.getCedula() : "No registrada");
        String telefonoDueño = prop != null && prop.getTelefono() != null && !prop.getTelefono().isBlank() ?
                prop.getTelefono() : "No registrado";
        String emailDueño = prop != null && prop.getEmail() != null && !prop.getEmail().isBlank() ?
                prop.getEmail() : "No registrado";

        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CO);
        String precioFmt = "$ " + nf.format(car.getPrecio()) + " COP";
        String kmFmt = car.getKilometraje() != null && !car.getKilometraje().isBlank() ?
                car.getKilometraje() + " km" : "No especificado";

        String ahora = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("          RAIC — CERTIFICADO OFICIAL DE INSPECCIÓN Y VALUACIÓN VEHICULAR         \n");
        sb.append("================================================================================\n");
        sb.append("Placa Vehicular:      ").append(car.getPlaca()).append("\n");
        sb.append("Fecha de Expedición:  ").append(ahora).append("\n");
        sb.append("Estado en Sistema:    ").append(car.getEstado() != null ? car.getEstado() : "Disponible").append("\n");
        sb.append("ID de Registro:       ").append(car.getId() != null ? car.getId() : "N/A").append("\n");
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("1. INFORMACIÓN DEL VEHÍCULO\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-24s: %s\n", "Placa", car.getPlaca()));
        sb.append(String.format("%-24s: %s\n", "Marca", valorO(car.getMarca(), "No especificada")));
        sb.append(String.format("%-24s: %s\n", "Modelo", valorO(car.getModelo(), "No especificado")));
        sb.append(String.format("%-24s: %s\n", "Año de Fabricación", valorO(car.getAño(), "No especificado")));
        sb.append(String.format("%-24s: %s\n", "Color", valorO(car.getColor(), "No especificado")));
        sb.append(String.format("%-24s: %s\n", "Kilometraje", kmFmt));
        sb.append(String.format("%-24s: %s\n", "Transmisión", valorO(car.getTransmision(), "No especificada")));
        sb.append(String.format("%-24s: %s\n", "Combustible", valorO(car.getCombustible(), "No especificado")));
        sb.append(String.format("%-24s: %s\n", "Precio Valuado", precioFmt));
        sb.append(String.format("%-24s: %s\n", "N° Propietarios Previos", valorO(car.getNPropietarios(), "1")));
        sb.append(String.format("%-24s: %s\n", "Fecha de Registro", valorO(car.getFechaRegistro(), ahora)));
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("2. INFORMACIÓN DEL PROPIETARIO\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-24s: %s\n", "Nombre Completo", nombreDueño));
        sb.append(String.format("%-24s: %s\n", "Cédula / Documento", cedulaDueño));
        sb.append(String.format("%-24s: %s\n", "Teléfono de Contacto", telefonoDueño));
        sb.append(String.format("%-24s: %s\n", "Correo Electrónico", emailDueño));
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("3. RESULTADOS DE REVISIÓN TÉCNICO-MECÁNICA\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Revisión Técnica", car.getPuntajeTecnico()));
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Revisión Mecánica", car.getPuntajeMecanico()));
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Promedio General", car.getPuntajePromedio()));
        sb.append(String.format("%-32s: %s\n", "Concepto Calificación", evaluarConcepto(car.getPuntajePromedio())));
        sb.append("\n");
        sb.append("Observaciones de Revisión Técnica (Documental y Estética):\n");
        sb.append(car.getObservacionRevision() != null && !car.getObservacionRevision().isBlank() ?
                car.getObservacionRevision() : "Sin observaciones técnicas registradas.").append("\n\n");
        sb.append("Observaciones de Revisión Mecánica (Motor, Frenos, Suspensión):\n");
        sb.append(car.getObservacionMecanica() != null && !car.getObservacionMecanica().isBlank() ?
                car.getObservacionMecanica() : "Sin observaciones mecánicas registradas.").append("\n");
        sb.append("\n");
        sb.append("================================================================================\n");
        sb.append("Documento emitido electrónicamente por RAIC — Sistema de Gestión Vehicular.\n");
        sb.append("================================================================================\n");

        Files.writeString(txtPath, sb.toString(), StandardCharsets.UTF_8);
        return txtPath;
    }

    /**
     * Genera el archivo PDF profesional usando OpenPDF con membrete, tablas y colores corporativos.
     */
    public Path generarArchivoPdf(Car car, Propietario prop) throws DocumentException, IOException {
        String placaLimpia = sanitizarPlacaParaArchivo(car.getPlaca());
        Path pdfPath = reportsDirectory.resolve("informe_" + placaLimpia + ".pdf");

        String nombreDueño = prop != null && prop.getNombre() != null ? prop.getNombre() :
                (car.getPropietario() != null ? car.getPropietario() : "No registrado");
        String cedulaDueño = prop != null && prop.getCedula() != null ? prop.getCedula() :
                (car.getCedula() != null ? car.getCedula() : "No registrada");
        String telefonoDueño = prop != null && prop.getTelefono() != null && !prop.getTelefono().isBlank() ?
                prop.getTelefono() : "No registrado";
        String emailDueño = prop != null && prop.getEmail() != null && !prop.getEmail().isBlank() ?
                prop.getEmail() : "No registrado";

        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_CO);
        String precioFmt = "$ " + nf.format(car.getPrecio()) + " COP";
        String kmFmt = car.getKilometraje() != null && !car.getKilometraje().isBlank() ?
                car.getKilometraje() + " km" : "No especificado";

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(pdfPath.toFile()));

        document.addTitle("Informe Vehicular - " + car.getPlaca());
        document.addSubject("Certificado de Revisión y Valuación");
        document.addAuthor("RAIC - Sistema de Gestión Vehicular");
        document.addCreator("RAIC System");

        document.open();

        // Fuentes
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
        Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(203, 213, 225));
        Font fontSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(51, 65, 85));
        Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(15, 23, 42));
        Font fontPie = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 116, 139));

        // 1. Encabezado principal (Banner)
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);

        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(new Color(15, 23, 42)); // Azul marino oscuro
        headerCell.setPadding(14);
        headerCell.setBorder(Rectangle.NO_BORDER);

        Paragraph pBrand = new Paragraph("RAIC  |  GESTIÓN Y REVISIÓN VEHICULAR", fontSubtitulo);
        pBrand.setSpacingAfter(4);
        headerCell.addElement(pBrand);

        Paragraph pTitulo = new Paragraph("CERTIFICADO OFICIAL DE INSPECCIÓN Y VALUACIÓN", fontTitulo);
        pTitulo.setSpacingAfter(4);
        headerCell.addElement(pTitulo);

        Paragraph pMeta = new Paragraph("Placa: " + car.getPlaca() + "   ·   Fecha: " +
                LocalDateTime.now().format(DATE_FORMATTER) + "   ·   Estado: " +
                (car.getEstado() != null ? car.getEstado() : "Disponible"), fontSubtitulo);
        headerCell.addElement(pMeta);

        headerTable.addCell(headerCell);
        document.add(headerTable);

        document.add(new Paragraph(" "));

        // 2. Sección: Datos del Vehículo
        document.add(crearTituloSeccion("1. INFORMACIÓN DEL VEHÍCULO", fontSeccion));
        PdfPTable carTable = new PdfPTable(4);
        carTable.setWidthPercentage(100);
        carTable.setWidths(new float[]{22f, 28f, 22f, 28f});

        agregarFila2(carTable, "Placa:", car.getPlaca(), "Marca:", valorO(car.getMarca(), "N/A"), fontLabel, fontValue);
        agregarFila2(carTable, "Modelo:", valorO(car.getModelo(), "N/A"), "Año:", valorO(car.getAño(), "N/A"), fontLabel, fontValue);
        agregarFila2(carTable, "Color:", valorO(car.getColor(), "N/A"), "Kilometraje:", kmFmt, fontLabel, fontValue);
        agregarFila2(carTable, "Transmisión:", valorO(car.getTransmision(), "N/A"), "Combustible:", valorO(car.getCombustible(), "N/A"), fontLabel, fontValue);
        agregarFila2(carTable, "Precio Valuado:", precioFmt, "N° Propietarios:", valorO(car.getNPropietarios(), "1"), fontLabel, fontValue);

        document.add(carTable);
        document.add(new Paragraph(" "));

        // 3. Sección: Datos del Propietario
        document.add(crearTituloSeccion("2. INFORMACIÓN DEL PROPIETARIO", fontSeccion));
        PdfPTable propTable = new PdfPTable(4);
        propTable.setWidthPercentage(100);
        propTable.setWidths(new float[]{22f, 28f, 22f, 28f});

        agregarFila2(propTable, "Nombre Completo:", nombreDueño, "Cédula:", cedulaDueño, fontLabel, fontValue);
        agregarFila2(propTable, "Teléfono:", telefonoDueño, "Correo Electrónico:", emailDueño, fontLabel, fontValue);

        document.add(propTable);
        document.add(new Paragraph(" "));

        // 4. Sección: Revisión Técnico-Mecánica
        document.add(crearTituloSeccion("3. RESULTADOS DE LA REVISIÓN TÉCNICO-MECÁNICA", fontSeccion));

        PdfPTable scoreTable = new PdfPTable(3);
        scoreTable.setWidthPercentage(100);
        scoreTable.setWidths(new float[]{33.3f, 33.3f, 33.4f});

        agregarScoreCard(scoreTable, "Puntaje Técnico", car.getPuntajeTecnico() + " / 100", new Color(240, 249, 255), new Color(2, 132, 199));
        agregarScoreCard(scoreTable, "Puntaje Mecánico", car.getPuntajeMecanico() + " / 100", new Color(240, 253, 244), new Color(22, 163, 74));
        agregarScoreCard(scoreTable, "Promedio Global", car.getPuntajePromedio() + " / 100", new Color(254, 242, 242), new Color(225, 29, 72));

        document.add(scoreTable);
        document.add(new Paragraph(" "));

        // Observaciones
        PdfPTable obsTable = new PdfPTable(1);
        obsTable.setWidthPercentage(100);

        PdfPCell obsTecCell = new PdfPCell();
        obsTecCell.setBackgroundColor(new Color(248, 250, 252));
        obsTecCell.setPadding(8);
        obsTecCell.setBorderColor(new Color(226, 232, 240));
        Paragraph pObsTecTitulo = new Paragraph("Observaciones de Revisión Técnica (Documentos y Estado):", fontLabel);
        Paragraph pObsTecCont = new Paragraph(car.getObservacionRevision() != null && !car.getObservacionRevision().isBlank() ?
                car.getObservacionRevision() : "Sin observaciones técnicas registradas.", fontValue);
        obsTecCell.addElement(pObsTecTitulo);
        obsTecCell.addElement(pObsTecCont);
        obsTable.addCell(obsTecCell);

        PdfPCell obsMecCell = new PdfPCell();
        obsMecCell.setBackgroundColor(new Color(248, 250, 252));
        obsMecCell.setPadding(8);
        obsMecCell.setBorderColor(new Color(226, 232, 240));
        Paragraph pObsMecTitulo = new Paragraph("Observaciones de Revisión Mecánica (Motor, Frenos, Suspensión):", fontLabel);
        Paragraph pObsMecCont = new Paragraph(car.getObservacionMecanica() != null && !car.getObservacionMecanica().isBlank() ?
                car.getObservacionMecanica() : "Sin observaciones mecánicas registradas.", fontValue);
        obsMecCell.addElement(pObsMecTitulo);
        obsMecCell.addElement(pObsMecCont);
        obsTable.addCell(obsMecCell);

        document.add(obsTable);

        // Footer
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));
        Paragraph footerP = new Paragraph("Certificado generado automáticamente por RAIC (Sistema de Gestión Vehicular). " +
                "Verificado y firmado electrónicamente. Válido a nivel informativo y técnico.", fontPie);
        footerP.setAlignment(Element.ALIGN_CENTER);
        document.add(footerP);

        document.close();
        return pdfPath;
    }

    private Paragraph crearTituloSeccion(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setSpacingBefore(4);
        p.setSpacingAfter(4);
        return p;
    }

    private void agregarFila2(PdfPTable table, String l1, String v1, String l2, String v2, Font fLabel, Font fValue) {
        PdfPCell cL1 = new PdfPCell(new Phrase(l1, fLabel));
        cL1.setBackgroundColor(new Color(241, 245, 249));
        cL1.setPadding(5);
        cL1.setBorderColor(new Color(226, 232, 240));
        table.addCell(cL1);

        PdfPCell cV1 = new PdfPCell(new Phrase(v1, fValue));
        cV1.setPadding(5);
        cV1.setBorderColor(new Color(226, 232, 240));
        table.addCell(cV1);

        PdfPCell cL2 = new PdfPCell(new Phrase(l2, fLabel));
        cL2.setBackgroundColor(new Color(241, 245, 249));
        cL2.setPadding(5);
        cL2.setBorderColor(new Color(226, 232, 240));
        table.addCell(cL2);

        PdfPCell cV2 = new PdfPCell(new Phrase(v2, fValue));
        cV2.setPadding(5);
        cV2.setBorderColor(new Color(226, 232, 240));
        table.addCell(cV2);
    }

    private void agregarScoreCard(PdfPTable table, String label, String score, Color bg, Color textColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(10);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font fL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textColor);
        Font fS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, textColor);

        Paragraph pL = new Paragraph(label, fL);
        pL.setAlignment(Element.ALIGN_CENTER);
        Paragraph pS = new Paragraph(score, fS);
        pS.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(pL);
        cell.addElement(pS);
        table.addCell(cell);
    }

    private String evaluarConcepto(int puntaje) {
        if (puntaje >= 90) return "Excelente estado técnico y mecánico (Aprobado)";
        if (puntaje >= 75) return "Buen estado general (Aprobado)";
        if (puntaje >= 60) return "Aceptable con observaciones (Condicional)";
        return "Requiere intervención o reparaciones inmediatas (No aprobado)";
    }

    private String sanitizarPlacaParaArchivo(String placa) {
        return placa != null ? placa.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "") : "DESCONOCIDO";
    }

    private String valorO(String v, String def) {
        return (v != null && !v.isBlank()) ? v : def;
    }

    /**
     * Consulta el informe consolidado por placa (normalizada o directa).
     */
    public InformeDetalleDTO obtenerInformePorPlaca(String placaRaw) {
        if (placaRaw == null || placaRaw.isBlank()) {
            return null;
        }

        String placaBuscada = placaRaw.trim().toUpperCase();
        if (AppUtils.isValidPlate(placaBuscada)) {
            placaBuscada = AppUtils.formatPlate(placaBuscada);
        }

        Optional<Car> carOpt = carRepository.findByPlacaAndArchivadoFalse(placaBuscada);
        if (carOpt.isEmpty()) {
            // Intentar sin guión o con guión alternativo si la placa no fue formateada
            carOpt = carRepository.findByArchivadoFalseOrderByIdDesc().stream()
                    .filter(c -> c.getPlaca() != null && c.getPlaca().replaceAll("-", "").equalsIgnoreCase(placaRaw.replaceAll("-", "").trim()))
                    .findFirst();
        }

        if (carOpt.isEmpty()) {
            return null;
        }

        Car car = carOpt.get();
        Propietario prop = null;
        if (car.getCedula() != null && !car.getCedula().isBlank()) {
            prop = propietarioRepository.findByCedula(car.getCedula().trim()).orElse(null);
        }

        // Asegurar que los archivos existan
        String placaLimpia = sanitizarPlacaParaArchivo(car.getPlaca());
        Path pdfPath = reportsDirectory.resolve("informe_" + placaLimpia + ".pdf");
        Path txtPath = reportsDirectory.resolve("informe_" + placaLimpia + ".txt");

        if (!Files.exists(pdfPath) || !Files.exists(txtPath)) {
            generarArchivosInforme(car, prop);
        }

        InformeDetalleDTO dto = new InformeDetalleDTO();
        dto.setPlaca(car.getPlaca());
        dto.setMarca(car.getMarca());
        dto.setModelo(car.getModelo());
        dto.setAño(car.getAño());
        dto.setColor(car.getColor());
        dto.setKilometraje(car.getKilometraje());
        dto.setTransmision(car.getTransmision());
        dto.setCombustible(car.getCombustible());
        dto.setPrecio(car.getPrecio());
        dto.setEstado(car.getEstado());
        dto.setFechaRegistro(car.getFechaRegistro());
        dto.setNPropietarios(car.getNPropietarios());

        if (prop != null) {
            dto.setPropietarioNombre(prop.getNombre());
            dto.setPropietarioCedula(prop.getCedula());
            dto.setPropietarioTelefono(prop.getTelefono());
            dto.setPropietarioEmail(prop.getEmail());
        } else {
            dto.setPropietarioNombre(car.getPropietario());
            dto.setPropietarioCedula(car.getCedula());
        }

        dto.setPuntajeTecnico(car.getPuntajeTecnico());
        dto.setPuntajeMecanico(car.getPuntajeMecanico());
        dto.setPuntajePromedio(car.getPuntajePromedio());
        dto.setObservacionRevision(car.getObservacionRevision());
        dto.setObservacionMecanica(car.getObservacionMecanica());

        dto.setArchivoPdfExiste(Files.exists(pdfPath));
        dto.setArchivoTxtExiste(Files.exists(txtPath));
        dto.setFechaGeneracion(car.getFechaModificacion() != null ? car.getFechaModificacion() : car.getFechaCreacion());

        return dto;
    }

    /**
     * Obtiene los bytes del archivo para descarga directa.
     */
    public byte[] obtenerBytesArchivo(String placaRaw, String formato) throws IOException {
        InformeDetalleDTO dto = obtenerInformePorPlaca(placaRaw);
        if (dto == null) {
            return null;
        }

        String placaLimpia = sanitizarPlacaParaArchivo(dto.getPlaca());
        String ext = "pdf".equalsIgnoreCase(formato) ? ".pdf" : ".txt";
        Path filePath = reportsDirectory.resolve("informe_" + placaLimpia + ext);

        if (!Files.exists(filePath)) {
            // Reintentar generación
            Car car = carRepository.findByPlacaAndArchivadoFalse(dto.getPlaca()).orElse(null);
            if (car != null) {
                generarArchivosInforme(car, null);
            }
        }

        if (Files.exists(filePath)) {
            return Files.readAllBytes(filePath);
        }

        return null;
    }

    /**
     * Lista los últimos informes disponibles a partir de los autos registrados.
     */
    public List<InformeDetalleDTO> listarInformesRecientes(int limit) {
        List<Car> cars = carRepository.findByArchivadoFalseOrderByIdDesc();
        List<InformeDetalleDTO> result = new ArrayList<>();
        int count = 0;
        for (Car car : cars) {
            if (count >= limit) break;
            InformeDetalleDTO dto = obtenerInformePorPlaca(car.getPlaca());
            if (dto != null) {
                result.add(dto);
                count++;
            }
        }
        return result;
    }
}
