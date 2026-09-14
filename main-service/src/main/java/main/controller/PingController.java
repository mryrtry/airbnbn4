package main.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;


@RestController
@RequestMapping("/api")
public class PingController {

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of("service", "main-service", "status", "ok", "timestamp", Instant.now().toString());
	}

}