package com.ta3lim.backend.web;

import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.web.errors.NoteNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final Map<Long, Note> notes = new HashMap<>();

    @GetMapping
    public List<Note> getAllNotes() {
        return new ArrayList<>(notes.values());
    }
    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        Note note = notes.get(id);
        if (note != null) {
            return new ResponseEntity<>(note, HttpStatus.OK);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

    @PostMapping
    public ResponseEntity<Void> addNote(@RequestBody Note note) {
        notes.put(note.getId(), note);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNote(@PathVariable Long id, @RequestBody Note updatedNote) {
        if (notes.containsKey(id)) {
            notes.put(id, updatedNote);
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable Long id) {
        if (notes.containsKey(id)) {
            notes.remove(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

}
