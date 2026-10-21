package com.edunex.backend.service.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkFetcherGuardTest {

    @Test
    void privateAndInternalAddressesAreRefused() {
        for (String url : new String[]{"http://127.0.0.1/", "http://10.0.0.5/admin", "http://192.168.1.1/",
                "http://172.16.0.9/", "http://169.254.169.254/latest/meta-data", "http://[::1]/", "http://0.0.0.0/",
                "http://100.64.0.1/", "http://[fd00::1]/"}) {
            assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate(url), url);
        }
    }

    @Test
    void onlyHttpOnDefaultPortsWithoutCredentialsIsAllowed() {
        assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate("ftp://93.184.216.34/file"));
        assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate("http://93.184.216.34:8080/"));
        assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate("http://user:pass@93.184.216.34/"));
        assertThrows(IllegalArgumentException.class, () -> LinkFetcher.validate("not a url"));
    }

    @Test
    void aPublicAddressIsAccepted() {
        assertDoesNotThrow(() -> LinkFetcher.validate("https://93.184.216.34/page"));
    }
}