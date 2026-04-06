package com.example.school_library_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SchoolLibrarySystemApplication {

	public static void main(String[] args) {
		// In ra BCrypt hash đúng của "123456" — copy hash này vào SSMS UPDATE
		String hash = new BCryptPasswordEncoder().encode("123456");
		System.out.println("====================================");
		System.out.println("BCRYPT HASH CUA '123456': " + hash);
		System.out.println("====================================");

		SpringApplication.run(SchoolLibrarySystemApplication.class, args);
	}

}
