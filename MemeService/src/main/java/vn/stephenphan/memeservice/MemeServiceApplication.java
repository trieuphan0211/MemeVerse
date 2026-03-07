package vn.stephenphan.memeservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MemeServiceApplication.class, args);
    }
}
