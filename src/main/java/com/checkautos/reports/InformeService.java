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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
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
     * Carga el archivo del logo oficial RAIC en bytes para incluirlo en el PDF.
     */
    private byte[] obtenerLogoBytes() {
        try {
            // 1. Intentar desde classpath
            try (var is = getClass().getResourceAsStream("/static/img/logoRAIC.png")) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    if (data.length > 0) return data;
                }
            }
            // 2. Intentar rutas de archivo en el proyecto
            Path[] candidatePaths = new Path[]{
                    Paths.get("src/main/resources/static/img/logoRAIC.png"),
                    Paths.get("target/classes/static/img/logoRAIC.png"),
                    Paths.get("assets/images/logoRAIC_master_3334.png"),
                    Paths.get("logoRAIC.png")
            };
            for (Path p : candidatePaths) {
                if (Files.exists(p)) {
                    byte[] data = Files.readAllBytes(p);
                    if (data.length > 0) return data;
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo para el informe PDF: {}", e.getMessage());
        }
        return null;
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
     * Genera el archivo en texto plano (.txt) con membrete estructurado y ficha técnica.
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
        String folio = generarFolio(car.getPlaca());
        String dictamen = evaluarDictamen(car.getPuntajePromedio());
        String concepto = evaluarConcepto(car.getPuntajePromedio());
        String hashCert = generarHashCertificado(car.getPlaca(), ahora, car.getPuntajePromedio());

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("            RAIC — SISTEMA INTELIGENTE DE GESTIÓN VEHICULAR                     \n");
        sb.append("         CERTIFICADO OFICIAL DE INSPECCIÓN, PERITAJE Y VALUACIÓN                \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Folio Oficial:        %s\n", folio));
        sb.append(String.format("Placa Vehicular:      %s\n", car.getPlaca()));
        sb.append(String.format("Fecha de Expedición:  %s\n", ahora));
        sb.append(String.format("Estado en Sistema:    %s\n", car.getEstado() != null ? car.getEstado() : "Disponible"));
        sb.append(String.format("ID de Registro:       %s\n", car.getId() != null ? car.getId() : "N/A"));
        sb.append(String.format("Dictamen Técnico:     %s (%s)\n", dictamen, concepto));
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("1. INFORMACIÓN DEL VEHÍCULO\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-26s: %s\n", "Placa", car.getPlaca()));
        sb.append(String.format("%-26s: %s\n", "Marca", valorO(car.getMarca(), "No especificada")));
        sb.append(String.format("%-26s: %s\n", "Modelo", valorO(car.getModelo(), "No especificado")));
        sb.append(String.format("%-26s: %s\n", "Año de Fabricación", valorO(car.getAño(), "No especificado")));
        sb.append(String.format("%-26s: %s\n", "Color", valorO(car.getColor(), "No especificado")));
        sb.append(String.format("%-26s: %s\n", "Kilometraje", kmFmt));
        sb.append(String.format("%-26s: %s\n", "Transmisión", valorO(car.getTransmision(), "No especificada")));
        sb.append(String.format("%-26s: %s\n", "Combustible", valorO(car.getCombustible(), "No especificado")));
        sb.append(String.format("%-26s: %s\n", "Precio Valuado", precioFmt));
        sb.append(String.format("%-26s: %s\n", "N° Propietarios Previos", valorO(car.getNPropietarios(), "1")));
        sb.append(String.format("%-26s: %s\n", "Fecha de Registro", valorO(car.getFechaRegistro(), ahora)));
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("2. INFORMACIÓN DEL PROPIETARIO\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-26s: %s\n", "Nombre Completo", nombreDueño));
        sb.append(String.format("%-26s: %s\n", "Cédula / Documento", cedulaDueño));
        sb.append(String.format("%-26s: %s\n", "Teléfono de Contacto", telefonoDueño));
        sb.append(String.format("%-26s: %s\n", "Correo Electrónico", emailDueño));
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("3. RESULTADOS DE REVISIÓN TÉCNICO-MECÁNICA\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Revisión Técnica", car.getPuntajeTecnico()));
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Revisión Mecánica", car.getPuntajeMecanico()));
        sb.append(String.format("%-32s: %d / 100\n", "Puntaje Promedio General", car.getPuntajePromedio()));
        sb.append(String.format("%-32s: %s\n", "Dictamen y Concepto", dictamen + " - " + concepto));
        sb.append("\n");
        sb.append("Observaciones de Revisión Técnica (Documental y Estética):\n");
        sb.append(car.getObservacionRevision() != null && !car.getObservacionRevision().isBlank() ?
                car.getObservacionRevision() : "Sin observaciones técnicas registradas.").append("\n\n");
        sb.append("Observaciones de Revisión Mecánica (Motor, Frenos, Suspensión):\n");
        sb.append(car.getObservacionMecanica() != null && !car.getObservacionMecanica().isBlank() ?
                car.getObservacionMecanica() : "Sin observaciones mecánicas registradas.").append("\n");
        sb.append("\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("4. VALIDACIÓN DE AUTENTICIDAD Y FIRMA DIGITAL\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Hash SHA-256 de Validación: %s\n", hashCert));
        sb.append("Firma Electrónica Autorizada: RAIC-SECURE-AUTOMATED-STAMP\n");
        sb.append("================================================================================\n");
        sb.append("Documento emitido electrónicamente por RAIC — Sistema de Gestión Vehicular.\n");
        sb.append("Válido como soporte técnico de inspección y valuación vehicular.\n");
        sb.append("================================================================================\n");

        Files.writeString(txtPath, sb.toString(), StandardCharsets.UTF_8);
        return txtPath;
    }

    /**
     * Genera el archivo PDF profesional con membrete oficial, logo RAIC, badge de placa,
     * dictamen técnico, scorecards y validación digital.
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

        String ahora = LocalDateTime.now().format(DATE_FORMATTER);
        String folio = generarFolio(car.getPlaca());
        String dictamen = evaluarDictamen(car.getPuntajePromedio());
        String concepto = evaluarConcepto(car.getPuntajePromedio());
        String hashCert = generarHashCertificado(car.getPlaca(), ahora, car.getPuntajePromedio());

        Document document = new Document(PageSize.A4, 32, 32, 28, 28);
        PdfWriter.getInstance(document, new FileOutputStream(pdfPath.toFile()));

        document.addTitle("Informe Vehicular - " + car.getPlaca());
        document.addSubject("Certificado Oficial de Inspección y Valuación");
        document.addAuthor("RAIC - Sistema de Gestión Vehicular");
        document.addCreator("RAIC System");

        document.open();

        // ── Paleta de Colores Corporativa ──────────────────────────────
        Color colorNavy = new Color(11, 19, 41);        // Fondo encabezado
        Color colorCyan = new Color(0, 212, 255);       // Acento primario RAIC
        Color colorSlateText = new Color(15, 23, 42);   // Texto principal
        Color colorBorder = new Color(226, 232, 240);   // Bordes suaves
        Color colorMuted = new Color(100, 116, 139);    // Texto secundario

        // ── Fuentes ────────────────────────────────────────────────────
        Font fontBrand = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, Color.WHITE);
        Font fontTagline = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, colorCyan);
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(203, 213, 225));
        Font fontCertTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color.WHITE);

        Font fontPlacaHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, new Color(148, 163, 184));
        Font fontPlacaText = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.WHITE);
        Font fontMeta = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, new Color(203, 213, 225));
        Font fontMetaBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE);
        Font fontMetaFolio = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, new Color(56, 189, 248));

        Font fontSection = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, colorNavy);
        Font fontLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, new Color(51, 65, 85));
        Font fontValue = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, colorSlateText);
        Font fontValueBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, colorSlateText);
        Font fontValueHighlight = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(2, 132, 199));
        Font fontPie = FontFactory.getFont(FontFactory.HELVETICA, 7f, colorMuted);

        // ── 1. Encabezado Oficial con Logo RAIC ────────────────────────
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{64f, 36f});

        // Columna Izquierda: Logo + Identidad RAIC
        PdfPCell cellLeft = new PdfPCell();
        cellLeft.setBackgroundColor(colorNavy);
        cellLeft.setPadding(10);
        cellLeft.setBorder(Rectangle.NO_BORDER);

        PdfPTable brandInner = new PdfPTable(2);
        brandInner.setWidthPercentage(100);
        brandInner.setWidths(new float[]{22f, 78f});

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        byte[] logoBytes = obtenerLogoBytes();
        if (logoBytes != null) {
            try {
                Image logoImg = Image.getInstance(logoBytes);
                logoImg.scaleToFit(48, 48);
                logoCell.addElement(logoImg);
            } catch (Exception ex) {
                log.warn("Error al incrustar imagen de logo en PDF: {}", ex.getMessage());
                logoCell.addElement(new Paragraph("RAIC", fontBrand));
            }
        } else {
            logoCell.addElement(new Paragraph("RAIC", fontBrand));
        }
        brandInner.addCell(logoCell);

        PdfPCell textBrandCell = new PdfPCell();
        textBrandCell.setBorder(Rectangle.NO_BORDER);
        textBrandCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pBrandName = new Paragraph("RAIC", fontBrand);
        pBrandName.setSpacingAfter(1);
        textBrandCell.addElement(pBrandName);

        Paragraph pBrandTag = new Paragraph("SISTEMA DE GESTIÓN VEHICULAR", fontTagline);
        pBrandTag.setSpacingAfter(2);
        textBrandCell.addElement(pBrandTag);

        Paragraph pBrandDesc = new Paragraph("Peritaje, Inspección Técnica y Valuación Oficial", fontSub);
        textBrandCell.addElement(pBrandDesc);

        brandInner.addCell(textBrandCell);
        cellLeft.addElement(brandInner);

        Paragraph pCert = new Paragraph("CERTIFICADO OFICIAL DE INSPECCIÓN Y VALUACIÓN", fontCertTitle);
        pCert.setSpacingBefore(5);
        cellLeft.addElement(pCert);

        headerTable.addCell(cellLeft);

        // Columna Derecha: Badge de Placa + Folio Oficial
        PdfPCell cellRight = new PdfPCell();
        cellRight.setBackgroundColor(colorNavy);
        cellRight.setPadding(10);
        cellRight.setBorder(Rectangle.NO_BORDER);
        cellRight.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable placaBadge = new PdfPTable(1);
        placaBadge.setWidthPercentage(75);
        placaBadge.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell placaCell = new PdfPCell();
        placaCell.setBackgroundColor(new Color(30, 41, 59)); // Slate 800
        placaCell.setBorderColor(new Color(71, 85, 105));
        placaCell.setBorderWidth(1);
        placaCell.setPadding(4);
        placaCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pCol = new Paragraph("COLOMBIA", fontPlacaHeader);
        pCol.setAlignment(Element.ALIGN_CENTER);
        placaCell.addElement(pCol);

        Paragraph pPlaca = new Paragraph(car.getPlaca(), fontPlacaText);
        pPlaca.setAlignment(Element.ALIGN_CENTER);
        placaCell.addElement(pPlaca);
        placaBadge.addCell(placaCell);

        cellRight.addElement(placaBadge);

        Paragraph pFolio = new Paragraph("FOLIO: " + folio, fontMetaFolio);
        pFolio.setAlignment(Element.ALIGN_RIGHT);
        pFolio.setSpacingBefore(4);
        cellRight.addElement(pFolio);

        Paragraph pFecha = new Paragraph("Emisión: " + ahora, fontMeta);
        pFecha.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(pFecha);

        Paragraph pEstado = new Paragraph("Estado: " + (car.getEstado() != null ? car.getEstado().toUpperCase() : "DISPONIBLE"), fontMetaBold);
        pEstado.setAlignment(Element.ALIGN_RIGHT);
        cellRight.addElement(pEstado);

        headerTable.addCell(cellRight);
        document.add(headerTable);

        // ── 2. Banner de Dictamen Técnico Oficial ──────────────────────
        Color dictamenBg;
        Color dictamenBorder;
        Color dictamenTextColor;
        int puntaje = car.getPuntajePromedio();
        if (puntaje >= 70) {
            dictamenBg = new Color(240, 253, 244);       // Verde suave
            dictamenBorder = new Color(34, 197, 94);     // Borde verde
            dictamenTextColor = new Color(21, 128, 61);  // Texto verde oscuro
        } else if (puntaje >= 50) {
            dictamenBg = new Color(255, 251, 235);       // Amarillo suave
            dictamenBorder = new Color(245, 158, 11);    // Borde ámbar
            dictamenTextColor = new Color(180, 83, 9);   // Texto ámbar oscuro
        } else {
            dictamenBg = new Color(254, 242, 242);       // Rojo suave
            dictamenBorder = new Color(239, 68, 68);     // Borde rojo
            dictamenTextColor = new Color(185, 28, 28);  // Texto rojo oscuro
        }

        PdfPTable dictamenTable = new PdfPTable(1);
        dictamenTable.setWidthPercentage(100);
        dictamenTable.setSpacingBefore(6);
        dictamenTable.setSpacingAfter(6);

        PdfPCell dictamenCell = new PdfPCell();
        dictamenCell.setBackgroundColor(dictamenBg);
        dictamenCell.setBorderColor(dictamenBorder);
        dictamenCell.setBorderWidth(1.2f);
        dictamenCell.setPadding(6);
        dictamenCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font fontDictamen = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, dictamenTextColor);
        Paragraph pDictamen = new Paragraph("DICTAMEN TÉCNICO OFICIAL: " + dictamen + "  —  " + concepto.toUpperCase(), fontDictamen);
        pDictamen.setAlignment(Element.ALIGN_CENTER);
        dictamenCell.addElement(pDictamen);
        dictamenTable.addCell(dictamenCell);

        document.add(dictamenTable);

        // ── 3. Sección: Información del Vehículo ───────────────────────
        document.add(crearTituloSeccion("1. ESPECIFICACIONES DEL VEHÍCULO EVALUADO", fontSection, colorCyan));

        PdfPTable carTable = new PdfPTable(4);
        carTable.setWidthPercentage(100);
        carTable.setWidths(new float[]{22f, 28f, 22f, 28f});

        agregarFilaZebra(carTable, "Placa:", car.getPlaca(), "Marca:", valorO(car.getMarca(), "N/A"), fontLabel, fontValueBold, 0);
        agregarFilaZebra(carTable, "Modelo:", valorO(car.getModelo(), "N/A"), "Año Fabricación:", valorO(car.getAño(), "N/A"), fontLabel, fontValue, 1);
        agregarFilaZebra(carTable, "Color:", valorO(car.getColor(), "N/A"), "Kilometraje:", kmFmt, fontLabel, fontValue, 0);
        agregarFilaZebra(carTable, "Transmisión:", valorO(car.getTransmision(), "N/A"), "Combustible:", valorO(car.getCombustible(), "N/A"), fontLabel, fontValue, 1);
        agregarFilaZebra(carTable, "Precio Valuado:", precioFmt, "N° Propietarios:", valorO(car.getNPropietarios(), "1"), fontLabel, fontValueHighlight, 0);
        agregarFilaZebra(carTable, "Estado Actual:", valorO(car.getEstado(), "Disponible"), "Fecha Registro:", valorO(car.getFechaRegistro(), ahora), fontLabel, fontValue, 1);

        document.add(carTable);

        // ── 4. Sección: Información del Propietario ───────────────────
        document.add(crearTituloSeccion("2. INFORMACIÓN DEL PROPIETARIO REGISTRADO", fontSection, colorCyan));

        PdfPTable propTable = new PdfPTable(4);
        propTable.setWidthPercentage(100);
        propTable.setWidths(new float[]{22f, 28f, 22f, 28f});

        agregarFilaZebra(propTable, "Nombre Completo:", nombreDueño, "Cédula / Doc:", cedulaDueño, fontLabel, fontValueBold, 0);
        agregarFilaZebra(propTable, "Teléfono Contacto:", telefonoDueño, "Correo Electrónico:", emailDueño, fontLabel, fontValue, 1);

        document.add(propTable);

        // ── 5. Sección: Calificación Técnico-Mecánica ─────────────────
        document.add(crearTituloSeccion("3. RESULTADOS DE LA REVISIÓN TÉCNICO-MECÁNICA", fontSection, colorCyan));

        PdfPTable scoreTable = new PdfPTable(3);
        scoreTable.setWidthPercentage(100);
        scoreTable.setWidths(new float[]{33.3f, 33.3f, 33.4f});

        agregarScoreCardModerno(scoreTable, "PUNTAJE TÉCNICO", car.getPuntajeTecnico() + " / 100",
                "Carrocería, Documentos y Estética",
                new Color(240, 249, 255), new Color(186, 230, 253), new Color(2, 132, 199));

        agregarScoreCardModerno(scoreTable, "PUNTAJE MECÁNICO", car.getPuntajeMecanico() + " / 100",
                "Motor, Frenos, Suspensión y Fluidos",
                new Color(240, 253, 244), new Color(187, 247, 208), new Color(22, 163, 74));

        agregarScoreCardModerno(scoreTable, "CALIFICACIÓN PROMEDIO", car.getPuntajePromedio() + " / 100",
                "Puntaje Ponderado Certificado",
                dictamenBg, dictamenBorder, dictamenTextColor);

        document.add(scoreTable);

        // Observaciones Detalladas
        PdfPTable obsTable = new PdfPTable(1);
        obsTable.setWidthPercentage(100);
        obsTable.setSpacingBefore(4);

        PdfPCell obsTecCell = new PdfPCell();
        obsTecCell.setBackgroundColor(new Color(248, 250, 252));
        obsTecCell.setPadding(6);
        obsTecCell.setBorderColor(colorBorder);
        Paragraph pObsTecTitulo = new Paragraph("Observaciones de Revisión Técnica (Documentos y Estado):", fontLabel);
        Paragraph pObsTecCont = new Paragraph(car.getObservacionRevision() != null && !car.getObservacionRevision().isBlank() ?
                car.getObservacionRevision() : "Sin observaciones técnicas registradas.", fontValue);
        obsTecCell.addElement(pObsTecTitulo);
        obsTecCell.addElement(pObsTecCont);
        obsTable.addCell(obsTecCell);

        PdfPCell obsMecCell = new PdfPCell();
        obsMecCell.setBackgroundColor(new Color(248, 250, 252));
        obsMecCell.setPadding(6);
        obsMecCell.setBorderColor(colorBorder);
        Paragraph pObsMecTitulo = new Paragraph("Observaciones de Revisión Mecánica (Motor, Frenos, Suspensión):", fontLabel);
        Paragraph pObsMecCont = new Paragraph(car.getObservacionMecanica() != null && !car.getObservacionMecanica().isBlank() ?
                car.getObservacionMecanica() : "Sin observaciones mecánicas registradas.", fontValue);
        obsMecCell.addElement(pObsMecTitulo);
        obsMecCell.addElement(pObsMecCont);
        obsTable.addCell(obsMecCell);

        document.add(obsTable);

        // ── 6. Sección: Validación Electrónica y Sello ─────────────────
        document.add(crearTituloSeccion("4. AVAL TÉCNICO Y FIRMA ELECTRÓNICA", fontSection, colorCyan));

        PdfPTable validacionTable = new PdfPTable(2);
        validacionTable.setWidthPercentage(100);
        validacionTable.setWidths(new float[]{60f, 40f});

        PdfPCell valLeft = new PdfPCell();
        valLeft.setBackgroundColor(new Color(248, 250, 252));
        valLeft.setBorderColor(colorBorder);
        valLeft.setPadding(8);

        Paragraph pVal1 = new Paragraph("SELLO DE INTEGRIDAD Y CONTROL ELECTRÓNICO:", fontLabel);
        Paragraph pVal2 = new Paragraph("Hash SHA-256: " + hashCert, fontPie);
        Paragraph pVal3 = new Paragraph("Folio Único: " + folio + "   ·   Algoritmo: RAIC-SEC-v1", fontPie);
        Paragraph pVal4 = new Paragraph("Certificado emitido electrónicamente con validez legal e informativa.", fontPie);
        valLeft.addElement(pVal1);
        valLeft.addElement(pVal2);
        valLeft.addElement(pVal3);
        valLeft.addElement(pVal4);
        validacionTable.addCell(valLeft);

        PdfPCell valRight = new PdfPCell();
        valRight.setBackgroundColor(new Color(248, 250, 252));
        valRight.setBorderColor(colorBorder);
        valRight.setPadding(8);
        valRight.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pSignLine = new Paragraph("__________________________________________", fontPie);
        pSignLine.setAlignment(Element.ALIGN_CENTER);
        Paragraph pSign1 = new Paragraph("Firma Digital Autorizada", fontLabel);
        pSign1.setAlignment(Element.ALIGN_CENTER);
        Paragraph pSign2 = new Paragraph("RAIC — Peritaje y Valuación Automotriz", fontPie);
        pSign2.setAlignment(Element.ALIGN_CENTER);
        valRight.addElement(pSignLine);
        valRight.addElement(pSign1);
        valRight.addElement(pSign2);
        validacionTable.addCell(valRight);

        document.add(validacionTable);

        // ── Pie de Página ─────────────────────────────────────────────
        Paragraph footerP = new Paragraph("Página 1 de 1 · Certificado oficial emitido por RAIC (Sistema de Gestión Vehicular). " +
                "Verificable en línea en la plataforma. Generado el: " + ahora, fontPie);
        footerP.setAlignment(Element.ALIGN_CENTER);
        footerP.setSpacingBefore(6);
        document.add(footerP);

        document.close();
        return pdfPath;
    }

    private PdfPTable crearTituloSeccion(String texto, Font font, Color accentColor) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5);
        table.setSpacingAfter(3);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBorderWidthLeft(3.5f);
        cell.setBorderColorLeft(accentColor);
        cell.setPadding(4);

        Paragraph p = new Paragraph(texto, font);
        cell.addElement(p);
        table.addCell(cell);
        return table;
    }

    private void agregarFilaZebra(PdfPTable table, String l1, String v1, String l2, String v2, Font fLabel, Font fValue, int rowIdx) {
        Color bg = (rowIdx % 2 == 0) ? Color.WHITE : new Color(248, 250, 252);
        Color border = new Color(226, 232, 240);

        PdfPCell cL1 = new PdfPCell(new Phrase(l1, fLabel));
        cL1.setBackgroundColor(bg);
        cL1.setPadding(4);
        cL1.setBorderColor(border);
        table.addCell(cL1);

        PdfPCell cV1 = new PdfPCell(new Phrase(v1, fValue));
        cV1.setBackgroundColor(bg);
        cV1.setPadding(4);
        cV1.setBorderColor(border);
        table.addCell(cV1);

        PdfPCell cL2 = new PdfPCell(new Phrase(l2, fLabel));
        cL2.setBackgroundColor(bg);
        cL2.setPadding(4);
        cL2.setBorderColor(border);
        table.addCell(cL2);

        PdfPCell cV2 = new PdfPCell(new Phrase(v2, fValue));
        cV2.setBackgroundColor(bg);
        cV2.setPadding(4);
        cV2.setBorderColor(border);
        table.addCell(cV2);
    }

    private void agregarScoreCardModerno(PdfPTable table, String label, String score, String desc, Color bg, Color borderColor, Color textColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(borderColor);
        cell.setBorderWidth(1.2f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font fL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, textColor);
        Font fS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13f, textColor);
        Font fD = FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(71, 85, 105));

        Paragraph pL = new Paragraph(label, fL);
        pL.setAlignment(Element.ALIGN_CENTER);
        Paragraph pS = new Paragraph(score, fS);
        pS.setAlignment(Element.ALIGN_CENTER);
        Paragraph pD = new Paragraph(desc, fD);
        pD.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(pL);
        cell.addElement(pS);
        cell.addElement(pD);
        table.addCell(cell);
    }

    private String generarFolio(String placa) {
        String clean = sanitizarPlacaParaArchivo(placa);
        int year = LocalDateTime.now().getYear();
        return "CERT-RAIC-" + year + "-" + clean;
    }

    private String generarHashCertificado(String placa, String fecha, int puntaje) {
        try {
            String data = placa + "|" + fecha + "|" + puntaje + "|RAIC-SEC-SALT";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                String h = Integer.toHexString(0xff & hash[i]);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString().toUpperCase();
        } catch (Exception e) {
            return "A1B2C3D4E5F67890";
        }
    }

    private String evaluarDictamen(int puntaje) {
        if (puntaje >= 70) return "APROBADO";
        if (puntaje >= 50) return "CONDICIONAL";
        return "NO APROBADO";
    }

    private String evaluarConcepto(int puntaje) {
        if (puntaje >= 85) return "Excelente estado técnico y mecánico (Aprobado)";
        if (puntaje >= 70) return "Buen estado técnico-mecánico general (Aprobado)";
        if (puntaje >= 50) return "Aceptable con observaciones (Condicional)";
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

        // Siempre regenerar para asegurar que el informe en disco cuente con la plantilla más actualizada y logo
        generarArchivosInforme(car, prop);

        String placaLimpia = sanitizarPlacaParaArchivo(car.getPlaca());
        Path pdfPath = reportsDirectory.resolve("informe_" + placaLimpia + ".pdf");
        Path txtPath = reportsDirectory.resolve("informe_" + placaLimpia + ".txt");

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

        dto.setFolio(generarFolio(car.getPlaca()));
        dto.setDictamen(evaluarDictamen(car.getPuntajePromedio()));
        int p = car.getPuntajePromedio();
        dto.setDictamenTipo(p >= 70 ? "success" : (p >= 50 ? "warn" : "danger"));
        dto.setConcepto(evaluarConcepto(car.getPuntajePromedio()));
        dto.setFechaExpedicion(LocalDateTime.now().format(DATE_FORMATTER));

        dto.setArchivoPdfExiste(Files.exists(pdfPath));
        dto.setArchivoTxtExiste(Files.exists(txtPath));
        dto.setFechaGeneracion(car.getFechaModificacion() != null ? car.getFechaModificacion() : car.getFechaCreacion());

        return dto;
    }

    /**
     * Obtiene los bytes del archivo para descarga directa.
     * Regenera automáticamente el archivo para garantizar el diseño y logo más recientes.
     */
    public byte[] obtenerBytesArchivo(String placaRaw, String formato) throws IOException {
        InformeDetalleDTO dto = obtenerInformePorPlaca(placaRaw);
        if (dto == null) {
            return null;
        }

        Car car = carRepository.findByPlacaAndArchivadoFalse(dto.getPlaca()).orElse(null);
        if (car != null) {
            Propietario prop = null;
            if (car.getCedula() != null && !car.getCedula().isBlank()) {
                prop = propietarioRepository.findByCedula(car.getCedula().trim()).orElse(null);
            }
            generarArchivosInforme(car, prop);
        }

        String placaLimpia = sanitizarPlacaParaArchivo(dto.getPlaca());
        String ext = "pdf".equalsIgnoreCase(formato) ? ".pdf" : ".txt";
        Path filePath = reportsDirectory.resolve("informe_" + placaLimpia + ext);

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
