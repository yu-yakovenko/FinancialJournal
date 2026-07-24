package org.tonique.vocal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FinancialJournal {

    public static void main(String[] args) {
        SpringApplication.run(FinancialJournal.class, args);
    }
}