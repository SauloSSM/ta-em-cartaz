package br.com.elitedevticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EliteDevTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(EliteDevTicketApplication.class, args);
    }
}
