package com.tus.campusConnect.campus_connect;

import org.springframework.boot.SpringApplication;

public class TestCampusConnectApplication {

	public static void main(String[] args) {
		SpringApplication.from(CampusConnectApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
