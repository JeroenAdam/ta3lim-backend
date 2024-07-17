package com.ta3lim.backend.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Objects;

@Entity
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String imagePath; // URL or file path

    public Image(String imagePath) {
        this.imagePath = imagePath;
    }

    public Image() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Image image = (Image) o;
        return Objects.equals(id, image.id) && Objects.equals(imagePath, image.imagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, imagePath);
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}