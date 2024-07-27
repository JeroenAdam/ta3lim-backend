package com.ta3lim.backend.web;

import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.NoteStatus;
import com.ta3lim.backend.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    // @Value("${app.public-url}") At the moment not needed, linked with commented PostMapping
    // private String publicUrl;

    private final NoteService noteService;

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public ResponseEntity<List<Note>> getNotes(@RequestParam Optional<String> status) {
        logger.info("GET /notes - Retrieving all notes - either DRAFT or ACTIVE");
        List<Note> notes = null;
        if (status.isPresent()) {
            if (status.get().equalsIgnoreCase("DRAFT")) {
                notes = noteService.findByStatus(NoteStatus.DRAFT);
            } else if (status.get().equalsIgnoreCase("DELETED")) {
                notes = noteService.findByStatus(NoteStatus.DELETED);
            } else if (status.get().equalsIgnoreCase("ARCHIVED")) {
                notes = noteService.findByStatus(NoteStatus.ARCHIVED);
            }
        } else {
            notes = noteService.findByStatus(NoteStatus.ACTIVE);
        }
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long id) {
        logger.info("GET /notes/{} - Retrieving note with id {}", id, id);
        Note note = noteService.getNoteById(id);
        return new ResponseEntity<>(note, HttpStatus.OK);
    }

/*    @PostMapping
    public ResponseEntity<Note> addNote(@RequestBody Note note) {
        logger.info("POST /notes - Adding new note: {}", note);
        Note created = noteService.addNote(note);
        URI location = URI.create(String.format(publicUrl+"/api/v1/notes/%s", created.getId()));
        return ResponseEntity.created(location)
                .header("X-Note", "Note successfully created")
                .body(created);
    }
*/
    @PostMapping
    public Note createNote(@RequestBody Note note) {
        return noteService.createNote(note);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note updatedNote) {
        logger.info("PUT /notes/{} - Updating note with id {}: {}", id, id);
        Note updated = noteService.updateNoteById(id, updatedNote);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable Long id) {
        logger.info("DELETE /notes/{} - Deleting note with id {}", id, id);
        noteService.deleteNoteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
