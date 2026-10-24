package com.edunex.backend.security;

import com.edunex.backend.entity.LoginThrottle;
import com.edunex.backend.repository.LoginThrottleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LoginThrottleServiceTest {

    @Mock
    private LoginThrottleRepository repository;

    @InjectMocks
    private LoginThrottleService service;

    private final LoginThrottle row = new LoginThrottle();

    @BeforeEach
    void setUp() {
        row.setId("k");
        lenient().when(repository.findForUpdate("k")).thenReturn(Optional.of(row));
        lenient().when(repository.findById("k")).thenReturn(Optional.of(row));
    }

    @Test
    void locksOnTheFifthFailure() {
        for (int i = 1; i <= 4; i++) {
            assertFalse(service.recordFailure("k", 5, 15, 15));
        }
        assertTrue(service.recordFailure("k", 5, 15, 15));
        assertTrue(service.lockedSeconds("k") > 0);
    }

    @Test
    void anActionIsRefusedOnceTheWindowIsFull() {
        for (int i = 0; i < 3; i++) {
            assertEquals(0, service.consume("k", 3, 60));
        }
        assertTrue(service.consume("k", 3, 60) > 0);
    }

    @Test
    void anExpiredWindowStartsOver() {
        row.setFailures(4);
        row.setWindowStart(Instant.now().minus(Duration.ofMinutes(30)));
        assertFalse(service.recordFailure("k", 5, 15, 15));
        assertEquals(1, row.getFailures());
    }

    @Test
    void aKeyThatWasNeverSeenIsNotLocked() {
        assertEquals(0, service.lockedSeconds("k"));
    }
}