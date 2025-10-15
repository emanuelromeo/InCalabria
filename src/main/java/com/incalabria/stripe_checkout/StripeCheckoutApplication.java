package com.incalabria.stripe_checkout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StripeCheckoutApplication {

	public static void main(String[] args) {
		SpringApplication.run(StripeCheckoutApplication.class, args);
	}

}
