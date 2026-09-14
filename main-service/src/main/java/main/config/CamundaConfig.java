package main.config;

import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CamundaConfig {

	@Bean
	public DmnEngine dmnEngine() {
		return DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
	}

}