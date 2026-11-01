package dev.saisrin.cage;

import java.math.*;
import java.time.*;
import java.util.*;

public final class Rules {
  private Rules() {}

  public static LocalDate gamingDay(Instant instant, String zone, String cutoff) {
    ZoneId z = ZoneId.of(zone);
    LocalTime boundary = LocalTime.parse(cutoff);
    LocalDate date = instant.atZone(z).toLocalDate();
    // Compare actual instants to the local boundary: handles overlaps and gaps consistently.
    Instant start = date.atTime(boundary).atZone(z).toInstant();
    return instant.isBefore(start) ? date.minusDays(1) : date;
  }

  public static boolean ctr(BigDecimal in, BigDecimal out) {
    return in.compareTo(new BigDecimal("10000")) > 0 || out.compareTo(new BigDecimal("10000")) > 0;
  }

  public record TaxResult(
      boolean eligible,
      BigDecimal reportable,
      BigDecimal withholding,
      String rule,
      String referral) {}

  public static TaxResult tax(
      String game, BigDecimal gross, BigDecimal wager, int year, String residency) {
    if (!"RESIDENT".equals(residency))
      return new TaxResult(
          false,
          gross,
          BigDecimal.ZERO,
          "DEMO-" + year,
          "Specialist review: foreign payee rules not implemented");
    if (year != 2026)
      throw new IllegalArgumentException("Only versioned 2026 demonstration rules are enabled");
    BigDecimal net = gross.subtract(wager).max(BigDecimal.ZERO);
    boolean eligible;
    // Deliberately limited demonstration fixtures, NOT a complete IRS determination table.
    switch (game) {
      case "SLOT", "BINGO" -> eligible = gross.compareTo(new BigDecimal("2000")) >= 0;
      case "KENO" -> eligible = net.compareTo(new BigDecimal("2000")) >= 0;
      case "POKER" -> eligible = net.compareTo(new BigDecimal("5000")) > 0;
      case "PROMOTION" -> eligible = gross.compareTo(new BigDecimal("2000")) >= 0;
      case "OTHER" ->
          eligible =
              net.compareTo(new BigDecimal("2000")) >= 0
                  && gross.compareTo(wager.multiply(new BigDecimal("300"))) >= 0
                  && wager.signum() > 0;
      default -> throw new IllegalArgumentException("Unsupported demonstration game");
    }
    BigDecimal withholding =
        Set.of("POKER", "OTHER").contains(game) && net.compareTo(new BigDecimal("5000")) > 0
            ? net.multiply(new BigDecimal("0.24")).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    return new TaxResult(
        eligible,
        Set.of("KENO", "POKER", "OTHER").contains(game) ? net : gross,
        withholding,
        "DEMO-2026-v1",
        eligible ? null : "Below configured demonstration threshold");
  }
}
