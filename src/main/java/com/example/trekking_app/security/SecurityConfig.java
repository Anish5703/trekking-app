package com.example.trekking_app.security;


import com.example.trekking_app.service.MyUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


    private final MyUserDetailsService userDetailsService;

    private final JwtFilter jwtFilter;

    private final CustomOauth2SuccessHandler oauth2SuccessHandler;
    private final CustomOauth2FailureHandler oauth2FailureHandler;


    private final Logger log;

    public SecurityConfig(MyUserDetailsService userDetailsService,JwtFilter jwtFilter,
                          CustomOauth2SuccessHandler oauth2SuccessHandler,CustomOauth2FailureHandler oauth2FailureHandler )
    {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        log = LoggerFactory.getLogger(SecurityConfig.class);

    }


   @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        http
                .cors(Customizer.withDefaults())
                .csrf(configurer -> configurer.disable())
                .authorizeHttpRequests(request -> request
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .oauth2Login(oauth2 ->
                        oauth2
                                .successHandler(oauth2SuccessHandler)
                                .failureHandler(oauth2FailureHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint()) )
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) )
                .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);

                return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Provide a custom UserDetailsService to load users from DB
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        // Use BCryptPasswordEncoder to hash/verify passwords
        provider.setPasswordEncoder(bCryptPasswordEncoder());

        return provider;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception
    {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint()
    {
        return (request,response,authException) -> {
            String path = request.getServletPath();
            String method = request.getMethod();
            String authHeader = request.getHeader("Authorization");

            // Log everything
            log.error("=== UNAUTHORIZED ENTRYPOINT TRIGGERED ===");
            log.error("Request Path: {}", path);
            log.error("HTTP Method: {}", method);
            log.error("Authorization Header: {}", authHeader);
            log.error("Exception Message: {}", authException.getMessage());
            StringBuilder loginUrl = new StringBuilder();
            loginUrl.append(request.getScheme()).append("://").append(request.getServerName()).append(":").append(request.getServerPort()).append("/api/auth/login");
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized - Please login at \""+loginUrl+"\"}");
        };
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder(10);
    }


  public static final String[] PUBLIC_ENDPOINTS = {
           "/api/v1/auth/**",
          "/api/v1/oauth/**",
          "/api/v1/auth/signup/**",
          "/oauth2/authorization/**",
          "/login/oauth2/**",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/error",


  };



}
