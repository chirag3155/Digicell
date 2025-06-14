package com.api.digicell.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.google.gson.Gson;
import com.api.digicell.repository.LoginInfoRepo;
import com.api.digicell.utils.Constants;
import com.api.digicell.utils.HelperUtil;
import com.api.digicell.utils.URIConstants;

import io.jsonwebtoken.ExpiredJwtException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final Gson gson;
    private final JwtUtil jwtUtil;
    private final HelperUtil helperUtil;
    private final LoginInfoRepo infoRepo;

    // Logger for logging information
    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private final List<String> emailURIConstants = Arrays.asList(
            URIConstants.VALIDATE_EMAIL,
            URIConstants.FORGOT_PASSWORD,
            URIConstants.CREATE_PASSWORD
    );

    public JwtRequestFilter(Gson gson, JwtUtil jwtUtil, HelperUtil helperUtil, LoginInfoRepo infoRepo) {
        this.gson = gson;
        this.jwtUtil = jwtUtil;
        this.helperUtil = helperUtil;
        this.infoRepo = infoRepo;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Skip filtering for Swagger UI and public endpoints
        if (shouldNotFilter(request)) {
            filterChain.doFilter(request, response);
            logger.info("Request to {} passed without JWT validation", request.getRequestURI());
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");

        if (!handleJwtAuthorization(request, response, authorizationHeader, request.getRequestURI(), request.getRequestURI())) {
            return;
        }

        logger.info("Request to {} passed JWT validation", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/webjars") ||
            path.equals("/swagger-ui.html") ||
            path.startsWith("/api/v1/") ||
            path.equals("/error")) {
            logger.info("Request to {} skipped from JWT validation", path);
            return true;
        }
        return false;
    }

    private boolean handleJwtAuthorization(HttpServletRequest request, HttpServletResponse response,
            String authorizationHeader, String requestUri, String strippedUri) {
        String username = null;
        String jwtToken = null;
        String tokenType = null;

        if (authorizationHeader != null && authorizationHeader.startsWith(Constants.BEARER)) {
            jwtToken = authorizationHeader.substring(7);

            if (jwtToken.isEmpty()) {
                logger.warn("JWT token is empty in the Authorization header");
                setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_JWT_CODE,
                        "JWT token is empty");
                return false;
            }

            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
                tokenType = jwtUtil.getCustomClaimFromToken(jwtToken, Constants.TOKEN_TYPE);

                if (!isValidTokenType(tokenType, requestUri, strippedUri)) {
                    logger.warn("Invalid JWT token type for URI: {}", requestUri);
                    setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_JWT_CODE,
                            Constants.INVALID_JWT_TOKEN);
                    return false;
                }
            } catch (ExpiredJwtException e) {
                logger.warn("Expired JWT token for user: {} at {}", username, requestUri);
                setResponse(response, HttpServletResponse.SC_FORBIDDEN, Constants.EXPIRED_JWT_CODE,
                        Constants.JWT_TOKEN_EXPIRED);
                return false;
            } catch (Exception e) {
                logger.error("Error parsing JWT token: {}", e.getMessage(), e);
                setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_JWT_CODE,
                        Constants.INVALID_JWT_TOKEN);
                return false;
            }
        } else {
            logger.warn("Authorization header is missing or malformed");
            setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_JWT_CODE,
                        "JWT token is missing");
            return false;
        }

        if (!validateToken(username, jwtToken, request, response)) {
            logger.error("Invalid token for user: {}", username);
            return false;
        }

        return validateUserWithLoginInfoAndSourceIp(authorizationHeader, tokenType, request, response);
    }

    private boolean isValidTokenType(String tokenType, String requestUri, String strippedUri) {
        boolean isEmailToken = tokenType.equals(Constants.TYPE_EMAIL_TOKEN)
                && emailURIConstants.stream().anyMatch(strippedUri::contains);
        boolean isRefreshToken = tokenType.equals(Constants.TYPE_REFRESH_TOKEN)
                && strippedUri.contains(URIConstants.REFRESH_TOKEN);

        return isEmailToken || isRefreshToken || !tokenType.equals(Constants.TYPE_REFRESH_TOKEN);
    }

    private boolean validateToken(String userName, String jwtToken, HttpServletRequest request,
            HttpServletResponse response) {
        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = new User(userName, userName, new ArrayList<>());
            if (jwtUtil.validateToken(jwtToken, userName)) {
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
            } else {
                logger.warn("Invalid JWT token for user: {}", userName);
                setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_JWT_CODE,
                        Constants.INVALID_JWT_TOKEN);
                return false;
            }
        }
        return true;
    }

    private boolean validateUserWithLoginInfoAndSourceIp(String authorizationHeader, String tokenType,
            HttpServletRequest request, HttpServletResponse response) {
        if (authorizationHeader != null && authorizationHeader.startsWith(Constants.BEARER)) {
            authorizationHeader = authorizationHeader.substring(7);
        }

        String emailId = jwtUtil.getUsernameFromToken(authorizationHeader);

        if (emailId != null && !tokenType.equals(Constants.TYPE_EMAIL_TOKEN)
                && !tokenType.equals(Constants.TYPE_HE_TOKEN)
                && !tokenType.equals(Constants.TYPE_OTP_TOKEN)) {

            boolean status = helperUtil.validateUserWithDbAndSourceIp(
                    emailId, tokenType, authorizationHeader, request.getRemoteAddr());

            logger.info("source ip: {}", request.getRemoteAddr());

            if (!status) {
                logger.warn("Unauthorized user: {} from IP: {}", emailId, request.getRemoteAddr());
                setResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Constants.UNAUTHORIZED_CODE,
                        Constants.UNAUTHORIZED_REQUEST);
                return false;
            }
        }
        return true;
    }

    private void setResponse(HttpServletResponse response, int status, int errorCode, String messageKey) {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        GenericRes<String> generalResponse = new GenericRes<>(errorCode, Constants.UNSUCCESSFUL, messageKey);

        try {
            String jsonResponse = gson.toJson(generalResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
            response.getWriter().close();
        } catch (IOException e) {
            logger.error("Error writing response: {}", e.getMessage(), e);
        }
    }
}
