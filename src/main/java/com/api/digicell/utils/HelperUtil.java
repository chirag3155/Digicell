package com.api.digicell.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.api.digicell.repository.LoginInfoRepo;
import com.api.digicell.security.JwtUtil;
// import com.api.digicell.utils.CentralLogger;

@Component
public class HelperUtil {

    private final JwtUtil jwtUtil;
    private final LoginInfoRepo loginInfoRepo;

    public HelperUtil(JwtUtil jwtUtil, LoginInfoRepo loginInfoRepo) {
        this.jwtUtil = jwtUtil;
        this.loginInfoRepo = loginInfoRepo;
    }

    /**
     * Get current UTC timestamp
     */
    public LocalDateTime getCurrentUTCTimestamp() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

    /**
     * Get claims from JWT token
     */
    public String getClaims(String token, String claimKey) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith(Constants.BEARER)) {
            token = token.substring(7);
        }
        return jwtUtil.getCustomClaimFromToken(token, claimKey);
    }

    /**
     * Validate user with database and source IP
     */
    public boolean validateUserWithDbAndSourceIp(String userEmailId, String tokenType, String jwtToken, String sourceip) {
        if (userEmailId != null && !tokenType.equals(Constants.TYPE_EMAIL_TOKEN)
                && !tokenType.equals(Constants.TYPE_HE_TOKEN) 
                && !tokenType.equals(Constants.TYPE_OTP_TOKEN)) {
            
            boolean status = loginInfoRepo.existsByEmail(userEmailId);
            if (status) {
                String ipAddress = getClaims(jwtToken, Constants.IP_ADDRESS);
                if (ipAddress.equals(sourceip)) {
                    return true;
                } else {
                    // CentralLogger.error("IP address is not valid: " + jwtToken + 
                    //     ", userName: " + userEmailId + 
                    //     ", sourceIp " + sourceip + 
                    //     ", issued ipAddress " + ipAddress, null, null, -1, 0);
                }
            } else {
                // CentralLogger.error("User not exist or inactive: token: " + jwtToken + 
                //     ", userName: " + userEmailId + 
                //     ", sourceIp " + sourceip, null, null, -1, 0);
            }
            return false;
        }
        return true;
    }
} 