package dev.saisrin.cage;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RulesTest {
  @ParameterizedTest
  @CsvSource({
    "10001,0,true",
    "10500,0,true",
    "10000,0,false",
    "7000,6000,false",
    "12000,11000,true",
    "0,10001,true",
    "0,0,false"
  })
  void thresholds(String in, String out, boolean expected) {
    assertEquals(expected, Rules.ctr(new BigDecimal(in), new BigDecimal(out)));
  }

  @ParameterizedTest
  @CsvSource({
    "2026-01-10T10:59:59Z,2026-01-09",
    "2026-01-10T11:00:00Z,2026-01-10",
    "2026-03-08T09:59:59Z,2026-03-07",
    "2026-03-08T10:00:00Z,2026-03-08",
    "2026-11-01T10:59:59Z,2026-10-31",
    "2026-11-01T11:00:00Z,2026-11-01"
  })
  void cutoffAndDST(String at, String expected) {
    assertEquals(
        LocalDate.parse(expected), Rules.gamingDay(Instant.parse(at), "America/New_York", "06:00"));
  }

  @Test
  void overlappingBoundaryUsesFirstOccurrence() {
    assertEquals(
        LocalDate.of(2026, 11, 1),
        Rules.gamingDay(Instant.parse("2026-11-01T06:15:00Z"), "America/New_York", "01:30"));
  }

  @Test
  void springGapResolvesForward() {
    assertEquals(
        LocalDate.of(2026, 3, 7),
        Rules.gamingDay(Instant.parse("2026-03-08T07:15:00Z"), "America/New_York", "02:30"));
  }

  @Test
  void gameSpecificDemoRules() {
    assertTrue(
        Rules.tax("SLOT", new BigDecimal("2000"), BigDecimal.ZERO, 2026, "RESIDENT").eligible());
    assertFalse(
        Rules.tax("POKER", new BigDecimal("5000"), BigDecimal.ZERO, 2026, "RESIDENT").eligible());
    assertEquals(
        new BigDecimal("1440.00"),
        Rules.tax("POKER", new BigDecimal("6000"), BigDecimal.ZERO, 2026, "RESIDENT")
            .withholding());
  }

  @Test
  void foreignPayeeReferral() {
    assertFalse(
        Rules.tax("SLOT", new BigDecimal("12000"), BigDecimal.ZERO, 2026, "NONRESIDENT")
            .eligible());
  }

  @Test
  void unsupportedYearFailsClosed() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Rules.tax("SLOT", BigDecimal.TEN, BigDecimal.ZERO, 2025, "RESIDENT"));
  }
}
