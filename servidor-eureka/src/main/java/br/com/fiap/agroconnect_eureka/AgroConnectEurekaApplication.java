package br.com.fiap.agroconnect_eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class AgroConnectEurekaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgroConnectEurekaApplication.class, args);
	}

}
