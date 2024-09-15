package com.ta3lim.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.*;

import java.util.Optional;

@Entity
public class Links {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "referrer", referencedColumnName = "id")
    private Note referrer;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "referred", referencedColumnName = "id")
    private Note referred;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId() {
        this.id = id;
    }

    public Note getReferrer() {
        return referrer;
    }

    public void setReferrer(Note referrer) {
        this.referrer = referrer;
    }

    public Note getReferred() {
        return referred;
    }

    public void setReferred(Note referred) {
        this.referred = referred;
    }

}
