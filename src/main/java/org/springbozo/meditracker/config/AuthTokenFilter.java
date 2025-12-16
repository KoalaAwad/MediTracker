package org.springbozo.meditracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springbozo.meditracker.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@Component
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    public static final String BEARER_ = "Bearer ";
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try{

            String jwt = parseJwt(request);
            if (jwt != null && jwtUtil.ValidateJwtToken(jwt)) {
                final String username = jwtUtil.getUserFromToken(jwt);

                final var userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails,
                                null,
                                userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource()
                        .buildDetails(request));
                SecurityContextHolder.getContext()
                        .setAuthentication(authenticationToken);
            }

        }catch (Exception err){
            log.error("Cannot set user authentication", err);
        }
        filterChain.doFilter(request, response);
    }

private String parseJwt(HttpServletRequest request){
    String headerAuth = request.getHeader("Authorization");

    if (headerAuth != null && headerAuth.startsWith(BEARER_)) {
        return headerAuth.substring(BEARER_.length());
    }
    return null;
}}

