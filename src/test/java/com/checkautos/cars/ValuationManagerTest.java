package com.checkautos.cars;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ValuationManagerTest {

    private ValuationManager valuationManager;

    @BeforeEach
    void setUp() {
        valuationManager = new ValuationManager();
    }

    @Test
    @DisplayName("Debe calcular puntaje técnico correctamente con items buenos, regulares y malos")
    void testCalculateTechnicalScore() {
        // 5 buenos (5*10=50), 3 regulares (3*5=15), 2 malos (2*0=0) -> total 65 / (10*10) * 100 = 65%
        int score = valuationManager.calculateTechnicalScore(5, 3, 2);
        assertEquals(65, score);

        // Sin items debe dar 0
        assertEquals(0, valuationManager.calculateTechnicalScore(0, 0, 0));

        // Todos buenos -> 100
        assertEquals(100, valuationManager.calculateTechnicalScore(10, 0, 0));

        // Todos malos -> 0
        assertEquals(0, valuationManager.calculateTechnicalScore(0, 0, 10));
    }

    @Test
    @DisplayName("Debe calcular puntaje combinado promediando técnico y mecánico")
    void testCalculateCombinedScore() {
        int comb = valuationManager.calculateCombinedScore(80, 60);
        assertEquals(70, comb);
    }

    @Test
    @DisplayName("Debe retornar los porcentajes de descuento correctos según el puntaje combinado")
    void testGetDiscountPercentage() {
        assertEquals(0, valuationManager.getDiscountPercentage(95));
        assertEquals(5, valuationManager.getDiscountPercentage(85));
        assertEquals(12, valuationManager.getDiscountPercentage(75));
        assertEquals(20, valuationManager.getDiscountPercentage(65));
        assertEquals(30, valuationManager.getDiscountPercentage(55));
        assertEquals(42, valuationManager.getDiscountPercentage(45));
        assertEquals(55, valuationManager.getDiscountPercentage(35));
        assertEquals(65, valuationManager.getDiscountPercentage(30));
    }

    @Test
    @DisplayName("Debe calcular el precio base depreciando por año pero respetando el precio mínimo de 20 millones")
    void testCalculateBasePrice() {
        int currentYear = LocalDate.now().getYear();

        // Auto del año actual (edad 0): 90.000.000
        int precioActual = valuationManager.calculateBasePrice(String.valueOf(currentYear));
        assertEquals(90_000_000, precioActual);

        // Auto muy antiguo (por ejemplo, 1990): debe dar el piso mínimo de 20.000.000
        int precioAntiguo = valuationManager.calculateBasePrice("1990");
        assertEquals(20_000_000, precioAntiguo);

        // Entrada no numérica debe dar el valor mínimo por defecto
        int precioInvalido = valuationManager.calculateBasePrice("invalido");
        assertEquals(20_000_000, precioInvalido);
    }

    @Test
    @DisplayName("Debe calcular el precio final aplicando el descuento sobre el precio base")
    void testCalculateFinalPrice() {
        int base = 80_000_000;
        int descuentoPct = 10;
        int finalPrice = valuationManager.calculateFinalPrice(base, descuentoPct);
        assertEquals(72_000_000, finalPrice);
    }
}
