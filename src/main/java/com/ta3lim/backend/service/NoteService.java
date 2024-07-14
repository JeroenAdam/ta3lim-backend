package com.ta3lim.backend.service;

import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.Tag;
import com.ta3lim.backend.repository.NoteRepository;
import com.ta3lim.backend.repository.TagRepository;
import com.ta3lim.backend.web.errors.NoteNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;

    @Autowired
    public NoteService(NoteRepository noteRepository, TagRepository tagRepository) {
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Note getNoteById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public Note addNote(Note note) {
        return noteRepository.save(note);
    }

    public Note updateNoteById(Long id, Note updatedNote) {
        if (noteRepository.existsById(id)) {
            updatedNote.setId(id);
            return noteRepository.save(updatedNote);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

    public void deleteNoteById(Long id) {
        if (noteRepository.existsById(id)) {
            noteRepository.deleteById(id);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

    @Transactional
    public Note createNoteWithTags(Note note, List<Tag> tags) {
        // Set the note reference in each tag
        for (Tag tag : tags) {
            tag.setNote(note);
        }
        note.setTags(tags);
        return noteRepository.save(note);
    }
}
