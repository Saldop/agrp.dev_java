package dev.agrp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AgrpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgrpApplication.class, args);
    }
}
