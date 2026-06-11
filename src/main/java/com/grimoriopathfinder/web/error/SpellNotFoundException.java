package com.grimoriopathfinder.web.error;

public class SpellNotFoundException extends RuntimeException {

    public SpellNotFoundException(String message) {
        super(message);
    }
}
