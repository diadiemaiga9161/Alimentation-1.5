package com.ges.boutique;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import javax.sql.DataSource;

@SpringBootApplication
public class BoutiqueApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoutiqueApplication.class, args);
	}
}