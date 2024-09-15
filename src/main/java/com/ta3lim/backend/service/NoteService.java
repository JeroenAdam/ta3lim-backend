package com.ta3lim.backend.service;

import com.ta3lim.backend.domain.Links;
import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.NoteStatus;
import com.ta3lim.backend.repository.search.ElasticsearchNoteRepository;
import com.ta3lim.backend.repository.jpa.LinksRepository;
import com.ta3lim.backend.repository.jpa.NoteRepository;
import com.ta3lim.backend.web.errors.NoteNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    private final ElasticsearchNoteRepository elasticsearchNoteRepository;

    private final LinksRepository linksRepository;

    @Value("${app.public-url}")
    private String publicUrl;

    @Autowired
    public NoteService(NoteRepository noteRepository, ElasticsearchNoteRepository elasticsearchNoteRepository, LinksRepository linksRepository) {
        this.noteRepository = noteRepository;
        this.elasticsearchNoteRepository = elasticsearchNoteRepository;
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
        Note savedNote = noteRepository.save(note);
        // Duplicate existingNote into toIndexNote and remove tags
        Note toIndexNote = new Note();
        toIndexNote.setId(savedNote.getId());
        toIndexNote.setTitle(savedNote.getTitle());
        toIndexNote.setContent(savedNote.getContent());
        toIndexNote.setUpdateDate(savedNote.getUpdateDate());
        toIndexNote.setStatus(savedNote.getStatus());
        toIndexNote.setTags(null); // Remove tags from toIndexNote before indexing, will send flattened below
        toIndexNote.setTagLabels(savedNote.getTagLabels());
        elasticsearchNoteRepository.save(toIndexNote);  // Index in Elasticsearch
        return savedNote;
    }

    @Transactional
    public Note updateNoteById(Long noteId, Note updatedNote) {
        Note existingNote = noteRepository.findById(noteId).orElseThrow(() -> new NoteNotFoundException(noteId));
        existingNote.setTitle(updatedNote.getTitle());
        existingNote.setContent(updatedNote.getContent());
        existingNote.setUpdateDate(updatedNote.getUpdateDate());
        existingNote.setStatus(updatedNote.getStatus());
        if (updatedNote.getTags() != null) {
            existingNote.getTags().clear();
            existingNote.getTags().addAll(updatedNote.getTags());
        }
        // Clear existing links, parse the content and create new links
        linksRepository.deleteAllByReferrerId(noteId);
        Pattern pattern = Pattern.compile(publicUrl + "/#note-(\\d+)");
        Matcher matcher = pattern.matcher(updatedNote.getContent());
        Set<Long> referrerChain = buildReferrerChain(existingNote);
        while (matcher.find()) {
            Long referredNoteIdLong = Long.valueOf(matcher.group(1));
            if (referrerChain.contains(referredNoteIdLong)) {
                continue; // Avoid infinite recursion if user links note back to note in the referrer chain
            }
            Optional<Note> referredNote = noteRepository.findById(referredNoteIdLong);
            if (referredNote.isPresent()) {
                Note referred = referredNote.get();
                Links link = new Links();
                link.setReferrer(existingNote); // Set the referrer
                link.setReferred(referred);
                linksRepository.save(link);
            }
        }
        noteRepository.save(existingNote);
        indexNote(existingNote);
        return existingNote;
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

    @Transactional
    public void reindexAllNotes() {
        List<Note> allNotes = noteRepository.findAll();
        for (Note note : allNotes) {
            indexNote(note);
        }
    }

    private void indexNote(Note note) {
        Note toIndexNote = new Note();
        toIndexNote.setId(note.getId());
        toIndexNote.setTitle(note.getTitle());
        toIndexNote.setContent(note.getContent());
        toIndexNote.setUpdateDate(note.getUpdateDate());
        toIndexNote.setStatus(note.getStatus());
        toIndexNote.setTags(null); // Remove tags from toIndexNote before indexing
        toIndexNote.setTagLabels(note.getTagLabels());
        toIndexNote.setLinks(null); // Remove links from toIndexNote before indexing
        toIndexNote.setLinkLabels(note.getLinkLabels());
        elasticsearchNoteRepository.save(toIndexNote); // Index in Elasticsearch
    }

}
