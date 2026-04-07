package com.example.trekking_app.security;



import com.example.trekking_app.service.JwtService;
import com.example.trekking_app.service.MyUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final HandlerExceptionResolver handlerExceptionResolver;

    private final MyUserDetailsService userDetailsService;

    private  final Logger log;

    public JwtFilter(JwtService jwtService,
                     MyUserDetailsService userDetailsService,
                     HandlerExceptionResolver handlerExceptionResolver)
    {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
        log =  LoggerFactory.getLogger(JwtFilter.class);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    @NonNull HttpServletResponse resp,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String email = null;

        log.info("JWT FILTER HIT: {}", req.getServletPath());

        //Authorization header (for API / Postman)
        String authHeader = req.getHeader("Authorization");
        log.info("Authorization Header: {}",authHeader);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        log.info("Jwt Token : {}",token);
        //no header, try COOKIE (OAuth flow)
        if (token == null && req.getCookies() != null) {
            for (var cookie : req.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }

        //Validate & authenticate
       try {
           if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
               email = jwtService.extractEmail(token);

               log.info("Extracted email from token / : {}",email);

               UserDetails userDetails = userDetailsService.loadUserByUsername(email);
               log.info("Extracted UserDetails : {} {}",userDetails.getAuthorities(),userDetails.getUsername());
         boolean isTokenValid = jwtService.validateToken(token,userDetails);
         log.info("isTokenValid : {}",isTokenValid);
               if (isTokenValid) {
                   UsernamePasswordAuthenticationToken authToken =
                           new UsernamePasswordAuthenticationToken(
                                   userDetails,
                                   null,
                                   userDetails.getAuthorities()
                           );


                   authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                   SecurityContextHolder.getContext().setAuthentication(authToken);
                   log.info("GetAuthentication : {}",SecurityContextHolder.getContext().getAuthentication());
               }
           }

           filterChain.doFilter(req, resp);
           log.info("Jwt filtration completed");
       }
       catch(ExpiredJwtException | SignatureException | MalformedJwtException e ) {
           handlerExceptionResolver.resolveException(req,resp,null,e);
       }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/note/")
                || path.startsWith("/video/")
                || path.startsWith("/modelQuestion/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

}

