package com.ta3lim.backend.service;

import com.ta3lim.backend.domain.Links;
import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.NoteStatus;
import com.ta3lim.backend.repository.LinksRepository;
import com.ta3lim.backend.repository.NoteRepository;
import com.ta3lim.backend.web.errors.NoteNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    private final LinksRepository linksRepository;

    private static final String URL_PATTERN = "http://localhost:3000/#note-(\\d+)";

    @Autowired
    public NoteService(NoteRepository noteRepository, LinksRepository linksRepository) {
        this.noteRepository = noteRepository;
        this.linksRepository = linksRepository;
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Note getNoteById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public List<Note> findByStatus(NoteStatus status) {
        return noteRepository.findByStatus(status);
    }

    @Transactional
    public Note createNote(Note note) {
        return noteRepository.save(note);
    }

    @Transactional
    public Note updateNoteById(Long noteId, Note updatedNote) {
        Note existingNote = noteRepository.findById(noteId).orElseThrow(() -> new NoteNotFoundException(noteId));
        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        existingNote.setUpdateDate(updatedNote.getUpdateDate());
        existingNote.setStatus(updatedNote.getStatus());
        existingNote.getTags().clear();
        existingNote.getTags().addAll(updatedNote.getTags());
        // Clear existing links, parse the content and create new links
        linksRepository.deleteAllByReferrerId(noteId);
        Pattern pattern = Pattern.compile(URL_PATTERN);
        Matcher matcher = pattern.matcher(updatedNote.getContent());
        Set<Long> referrerChain = buildReferrerChain(existingNote);
        while (matcher.find()) {
            Long referredNoteIdLong = Long.valueOf(matcher.group(1));
            if (referrerChain.contains(referredNoteIdLong)) {
                continue; // Avoid infinite recursion if user links note back to note in the referrer chain
            }
            Note referredNote = noteRepository.findById(referredNoteIdLong)
                    .orElseThrow(() -> new NoteNotFoundException(referredNoteIdLong));
            Links link = new Links();
            link.setReferrer(existingNote); // Set the referrer
            link.setReferred(referredNote);
            linksRepository.save(link);
        }
        return noteRepository.save(existingNote);
    }

    private Set<Long> buildReferrerChain(Note note) {
        Set<Long> referrerChain = new HashSet<>();
        Note currentNote = note;
        while (currentNote != null) {
            referrerChain.add(currentNote.getId());
            if (currentNote.getLinked().isEmpty()) {
                break;
            }
            currentNote = currentNote.getLinked().get(0).getReferrer();
        }
        return referrerChain;
    }

    public void deleteNoteById(Long id) {
        if (noteRepository.existsById(id)) {
            noteRepository.deleteById(id);
        } else {
            throw new NoteNotFoundException(id);
        }
    }

}
