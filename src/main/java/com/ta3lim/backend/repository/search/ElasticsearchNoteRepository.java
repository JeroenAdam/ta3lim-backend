package com.ta3lim.backend.repository.search;

import com.ta3lim.backend.domain.Note;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ElasticsearchNoteRepository extends ElasticsearchRepository<Note, Long> {
    List<Note> findByTitle(String title);
    List<Note> findByContentContaining(String content);
}
