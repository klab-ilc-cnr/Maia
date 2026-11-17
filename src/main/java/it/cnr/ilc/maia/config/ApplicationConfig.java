package it.cnr.ilc.maia.config;

import it.cnr.ilc.maia.SecurityInterceptor;
import it.cnr.ilc.maia.service.AuthenticationService;
import it.cnr.ilc.maia.utils.SecurityAuditorAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Configuration
public class ApplicationConfig {

    @Autowired
    private AuthenticationService authenticationService;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS")
                        .allowedOrigins("*"); // for /** means all mapping URL, and * for all domain
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new SecurityInterceptor(authenticationService))
                        .addPathPatterns("/api/**")
                        .excludePathPatterns("/api/authentication/**")
                        .addPathPatterns("/texto/**");
//                .excludePathPatterns("/", "/error/**", "/api/authentication/**", "/swagger-ui/**");
            }
        };
    }

    @Bean
    public AuditorAware<Long> auditorAware() {
        return new SecurityAuditorAware();
    }

}
