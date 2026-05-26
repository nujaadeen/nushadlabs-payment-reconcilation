package com.nushadlabs.payment_reconcilation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.nushadlabs.payment_reconcilation", "com.recon"})
public class PaymentReconcilationApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentReconcilationApplication.class, args);
	}

}
