package com.tecdes.appsabancada;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class AppsabancadaApplicationTests {

    @Test
    void mainClassIsLoadable() {
        assertDoesNotThrow(() -> Class.forName("com.tecdes.appsabancada.AppsabancadaApplication"));
    }
}
