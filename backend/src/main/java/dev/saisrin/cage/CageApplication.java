package dev.saisrin.cage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(
    info =
        @io.swagger.v3.oas.annotations.info.Info(
            title = "CageLedger Demonstration API",
            version = "1.0.0",
            description =
                "Synthetic casino cage and compliance portfolio API. Filings are simulations; tax"
                    + " rules are limited demonstration fixtures."))
@SpringBootApplication
public class CageApplication {
  public static void main(String[] args) {
    SpringApplication.run(CageApplication.class, args);
  }
}
