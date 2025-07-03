package com.api.digicell.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatUtils {
    public static void logError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    public static void logDebug(String message, Object... args) {
        log.debug(message, args);
    }

    public static void logInfo(String message, Object... args) {
        log.info(message, args);
    }

    public static void logWarn(String message, Object... args) {
        log.warn(message, args);
    }

    public static String generateRoomId() {
        return java.util.UUID.randomUUID().toString();
    }

    public static boolean isValidRoomId(String roomId) {
        try {
            java.util.UUID.fromString(roomId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 