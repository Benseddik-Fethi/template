package com.benseddik.template;

import com.benseddik.template.config.SpringSecurityAuditorAware;
import com.benseddik.template.repository.AppUserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
    @Bean
    public AuditorAware<String> auditorAware(AppUserRepository userRepository) {
        return new SpringSecurityAuditorAware(userRepository);
    }

}
