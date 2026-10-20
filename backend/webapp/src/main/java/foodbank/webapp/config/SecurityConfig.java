package foodbank.webapp.config;

import foodbank.webapp.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            "{\"error\":\"Unauthorized\"}"
                                    );
                                }
                        )
                        .accessDeniedHandler(
                                (request, response, exception) -> {
                                    response.setStatus(403);
                                    response.setContentType(
                                            "application/json"
                                    );
                                    response.getWriter().write(
                                            "{\"error\":\"Forbidden\"}"
                                    );
                                }
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Allow frontend CORS preflight requests.
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public pages and Swagger.
                        .requestMatchers(
                                "/",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Public authentication endpoints.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/register-recipients",
                                "/api/auth/register-donors",
                                "/api/auth/register-both"
                        ).permitAll()

                        // Public listing browsing endpoints.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/listings/**"
                        ).permitAll()

                        // Publicly accessible uploaded files.
                        .requestMatchers("/uploads/**").permitAll()
                        // Admin management and global settings require an Admin JWT.
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/settings/**"
                        ).hasRole("ADMIN")

                        // Admin account lists.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/donors",
                                "/api/recipients"
                        ).hasRole("ADMIN")

                        // Report submission requires a Donor or Recipient JWT.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/reports"
                        ).hasAnyRole(
                                "DONOR",
                                "RECIPIENT"
                        )

                        // A recipient's saved location (used for listing distances).
                        .requestMatchers(
                                "/api/recipients/me/**"
                        ).hasRole("RECIPIENT")

                        // Only donors can access pickup approval endpoints.
                        .requestMatchers("/api/donors/*/requests", "/api/donors/*/requests/**")
                        .hasRole("DONOR")

                        // Pickup requests are made and viewed by recipients.
                        .requestMatchers(
                                "/api/requests",
                                "/api/requests/**"
                        ).hasRole("RECIPIENT")

                        // suggestions for searching
                        .requestMatchers("/api/suggestions/*").permitAll()

                        // Logout requires a valid JWT.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/logout"
                        ).authenticated()

                        // Donor listing management requires donor role.
                        .requestMatchers(
                                "/api/donors/*/listings/**",
                                "/api/donors/*/reports",
                                "/api/donors/*/collection-history"
                        ).hasRole("DONOR")
                        // Donors and recipients can switch between their own profiles.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/account/switch-role",
                                "/api/account/add-role"
                        ).hasAnyRole(
                                "DONOR",
                                "RECIPIENT"
                        )
                        // Future APIs are protected by default.
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
