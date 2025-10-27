package t4m.toy_store.auth.util;

import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT filter for public paths, static resources, and public HTML pages
        return path.startsWith("/api/auth/") 
            || path.startsWith("/error")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.equals("/") 
            || path.equals("/index")
            || path.equals("/login")
            || path.equals("/register")
            || path.equals("/forgot-password")
            || path.equals("/reset-password")
            || path.equals("/verify-otp")
            || path.equals("/products")
            || path.startsWith("/products/")
            || path.startsWith("/product/")
            || path.equals("/test-search");
        // For /admin/**, /cart, /profile, /orders etc. - JWT filter WILL run
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String requestUri = request.getRequestURI();
        boolean isApiRequest = requestUri.startsWith("/api/");

        // If no authorization header
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            // For API requests, this is an error (except public APIs)
            if (isApiRequest && 
                !requestUri.startsWith("/api/auth/") && 
                !requestUri.startsWith("/api/products") &&
                !requestUri.startsWith("/api/chatbot/") &&
                !requestUri.startsWith("/api/payment/vnpay/") &&  // VNPay callbacks
                !requestUri.startsWith("/api/orders/public/")) {  // Public order view after payment
                logger.warn("No JWT token in API request to: {}", requestUri);
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing JWT token");
                return;
            }
            // For HTML pages, let Spring Security handle it (will redirect to login if needed)
            chain.doFilter(request, response);
            return;
        }

        String jwt = authorizationHeader.substring(BEARER_PREFIX.length());

        try {
            String username = jwtUtil.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtUtil.isTokenValid(jwt, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Authenticated user: {} with roles: {}", username, userDetails.getAuthorities());
                } else {
                    logger.warn("Invalid JWT token for user: {}", username);
                    if (isApiRequest) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT token");
                        return;
                    }
                }
            }
        } catch (JwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            if (isApiRequest) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT token");
                return;
            }
        } catch (Exception e) {
            logger.error("Unexpected error in JWT filter: {}", e.getMessage());
            if (isApiRequest) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "JWT processing failed");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}