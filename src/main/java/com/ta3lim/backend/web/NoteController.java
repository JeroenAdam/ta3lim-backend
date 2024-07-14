package com.ta3lim.backend.web;

import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.Tag;
import com.ta3lim.backend.service.NoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@CrossOrigin(origins = "http://localhost:3000")
public class NoteController {

    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);
    private final NoteService noteService;

    @Value("${app.public-url}")
    private String publicUrl;

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<Note> getAllNotes() {
        logger.info("GET /notes - Retrieving all notes");
        return noteService.getAllNotes();
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
        return noteService.createNoteWithTags(note, note.getTags());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Note updatedNote) {
        logger.info("PUT /notes/{} - Updating note with id {}: {}", id, id, updatedNote);
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
