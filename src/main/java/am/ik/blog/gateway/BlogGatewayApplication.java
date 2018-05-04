package am.ik.blog.gateway;

import io.micrometer.core.instrument.config.MeterFilter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@SpringBootApplication
public class BlogGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogGatewayApplication.class, args);
	}

	@Bean
	public SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) {
		return http //
				.httpBasic() //
				.and() //
				.authorizeExchange() //
				.matchers(EndpointRequest.to("health", "info")).permitAll() //
				.matchers(EndpointRequest.toAnyEndpoint()).hasRole("ACTUATOR") //
				.anyExchange().permitAll() //
				.and() //
				.build();
	}

	@Bean
	public MeterRegistryCustomizer meterRegistryCustomizer() {
		return registry -> registry.config() //
				.meterFilter(MeterFilter.deny(id -> {
					String uri = id.getTag("uri");
					return uri != null && uri.startsWith("/actuator");
				}));
	}
}
