package com.ta3lim.backend.repository.jpa;

import com.ta3lim.backend.domain.Note;
import com.ta3lim.backend.domain.NoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    @Query("SELECT MAX(n.updateDate) FROM Note n")
    LocalDateTime findLatestUpdateDateTime();

    @Query("SELECT n.content FROM Note n")
    List<String> findAllContents();

    List<Note> findByStatus(NoteStatus status);

}
