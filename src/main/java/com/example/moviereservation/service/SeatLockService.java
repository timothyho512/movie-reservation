package com.example.moviereservation.service;
import com.example.moviereservation.entity.*;


public class SeatLockService {

    private void validateSeatLockIdentity(User user, String sessionId) {
    boolean hasUser = user != null;
    boolean hasSessionId = sessionId != null && !sessionId.isBlank();

    if (hasUser == hasSessionId) {
        throw new IllegalArgumentException(
                "Exactly one of user or sessionId must be provided"
        );
    }
}

}
