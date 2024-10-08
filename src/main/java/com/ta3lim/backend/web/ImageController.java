package com.ta3lim.backend.web;

import com.ta3lim.backend.domain.Image;
import com.ta3lim.backend.repository.jpa.ImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ImageController {

    @Value("${app.public-api}")
    private String publicApi;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final ImageRepository imageRepository;

    public ImageController(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    @PostMapping("/api/v1/upload")
    public String uploadThumbnail(@RequestParam("file") MultipartFile image) throws Exception {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image is empty");
        }
        String originalFilename = image.getOriginalFilename().replaceAll(" ", "_");
        String imageName = UUID.randomUUID().toString() + "-" + originalFilename;
        Path imagePath = Paths.get(uploadDir, imageName);
        Files.copy(image.getInputStream(), imagePath);
        Image img = new Image(imageName);
        imageRepository.save(img);
        return publicApi + "/images/" + imageName;
    }

    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> getImage(@PathVariable String fileName) {
        String sanitizedFileName = fileName.replaceAll(" ", "_");
        Optional<Image> imageOptional = imageRepository.findByImagePath(sanitizedFileName);

        if (imageOptional.isPresent()) {
            Image image = imageOptional.get();
            Path imagePath = Paths.get(uploadDir, image.getImagePath());

            try {
                Resource resource = new PathResource(imagePath);
                if (resource.exists() && resource.isReadable()) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(imagePath));

                    return new ResponseEntity<>(resource, headers, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            } catch (Exception e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
