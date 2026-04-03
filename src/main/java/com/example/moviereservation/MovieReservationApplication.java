package com.example.moviereservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MovieReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieReservationApplication.class, args);
    }

}
