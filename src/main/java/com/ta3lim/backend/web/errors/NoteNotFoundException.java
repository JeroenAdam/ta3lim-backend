package com.ta3lim.backend.web.errors;

public class NoteNotFoundException extends RuntimeException {
    public NoteNotFoundException(Long id) {
        super("Could not find note with ID: " + id);
    }
}