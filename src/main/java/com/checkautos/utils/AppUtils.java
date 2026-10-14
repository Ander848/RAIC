package com.checkautos.utils;

import java.text.NumberFormat;
import java.util.Locale;

public class AppUtils {
    private static final Locale COLOMBIAN_LOCALE = new Locale("es", "CO");


    public static String formatCurrency(int amount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(COLOMBIAN_LOCALE);
        return currencyFormat.format(amount);
    }


    public static String formatNumber(long number) {
        NumberFormat numberFormat = NumberFormat.getInstance(COLOMBIAN_LOCALE);
        return numberFormat.format(number);
    }


    public static String formatKilometers(String kilometers) {
        if (kilometers == null || kilometers.trim().isEmpty()) {
            return "—";
        }
        try {
            long km = Long.parseLong(kilometers);
            return formatNumber(km) + " km";
        } catch (NumberFormatException e) {
            return kilometers + " km";
        }
    }


    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }


    public static boolean isValidUsername(String username) {
        return username != null && !username.trim().isEmpty();
    }


    /**
     * Valida que la placa corresponda a un auto particular (3 letras y 3 números).
     * Formatos válidos: ABC123 o ABC-123 (insensible a mayúsculas/minúsculas).
     */
    public static boolean isValidPlate(String placa) {
        if (placa == null) return false;
        String clean = placa.trim().toUpperCase();
        return clean.matches("^[A-Z]{3}-?\\d{3}$");
    }

    /**
     * Normaliza la placa al formato estándar automotriz: AAA-123.
     */
    public static String formatPlate(String placa) {
        if (placa == null) return "";
        String clean = placa.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (clean.length() == 6 && clean.substring(0, 3).matches("[A-Z]{3}") && clean.substring(3).matches("\\d{3}")) {
            return clean.substring(0, 3) + "-" + clean.substring(3);
        }
        return placa.trim().toUpperCase();
    }


    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }


    public static boolean isValidCedula(String cedula) {
        return cedula != null && !cedula.trim().isEmpty();
    }


    public static boolean isValidPhone(String phone) {
        return phone != null && !phone.trim().isEmpty();
    }


    public static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }


    public static String cleanWhitespace(String text) {
        if (text == null) return "";
        return text.trim().replaceAll("\\s+", " ");
    }


    public static String toUpperCase(String text) {
        return text != null ? text.toUpperCase() : "";
    }


    public static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String cleaned = name.trim().toUpperCase();
        return cleaned.length() >= 2 ? cleaned.substring(0, 2) : cleaned;
    }


    public static String buildVehicleName(String marca, String modelo, String año) {
        StringBuilder sb = new StringBuilder();
        if (marca != null && !marca.isEmpty()) sb.append(marca);
        if (modelo != null && !modelo.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(modelo);
        }
        if (año != null && !año.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(año);
        }
        return sb.length() > 0 ? sb.toString() : "—";
    }


    public static boolean validateRequiredFields(String... fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }


    public static String getMonthNameSpanish(int month) {
        String[] months = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
                           "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        if (month >= 1 && month <= 12) {
            return months[month - 1];
        }
        return "";
    }
}