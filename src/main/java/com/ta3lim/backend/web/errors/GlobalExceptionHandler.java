package com.ta3lim.backend.web.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoteNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoteNotFoundException(NoteNotFoundException ex, WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Note Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false)));

        return new ResponseEntity<>(problemDetail, HttpStatus.NOT_FOUND);
    }
}
