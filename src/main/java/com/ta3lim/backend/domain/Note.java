package com.ta3lim.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Document(indexName = "#{@environment.getProperty('spring.application.name')}")
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private LocalDateTime updateDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoteStatus status = NoteStatus.ACTIVE;

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Tag> tags;

    @Field(type = FieldType.Text) // Index tag labels as a list of text fields in Elasticsearch
    @Transient // This field is not persisted to the database, used by Elasticsearch
    private List<String> tagLabels;

    @Field(type = FieldType.Text)
    @Transient
    private List<String> linkLabels;

    @OneToMany(mappedBy = "referrer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Links> links;

    @OneToMany(mappedBy = "referred", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Links> linked;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

    public NoteStatus getStatus() { return status; }

    public void setStatus(NoteStatus status) { this.status = status; }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<String> getTagLabels() {
        if (this.tags != null) {
            return tags.stream().map(Tag::getLabel).toList(); // Flatten tags to their labels
        }
        return Collections.emptyList();
    }

    public void setTagLabels(List<String> tagLabels) {
        this.tagLabels = tagLabels;
    }

    public List<String> getLinkLabels() {
        if (this.linked != null) {
            return linked.stream()
                    .map(link -> link.getReferrer().id + " " + link.getReferrer().title)
                    .toList(); // Flatten links to strings
        }
        return Collections.emptyList();
    }

    public void setLinkLabels(List<String> linkLabels) {
        this.linkLabels = linkLabels;
    }

    public List<Links> getLinks() {
        return links;
    }

    public void setLinks(List<Links> links) {
        this.links = links;
    }

    public List<Links> getLinked() {
        return linked;
    }

    public void setLinked(List<Links> linked) {
        this.linked = linked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Note)) return false;
        Note note = (Note) o;
        return Objects.equals(id, note.id) &&
                Objects.equals(title, note.title) &&
                Objects.equals(content, note.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content);
    }

    @Override
    public String toString() {
        return "Note [ id = " + id + ", title = " + title + ", content = " + content + " ]";
    }
}