package org.springbozo.meditracker.controller;

import org.springbozo.meditracker.service.DailyMedService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Package label images, proxied so the viewer's browser doesn't tell DailyMed which medicines they look at.
 * Public because img tags can't send the auth header; it only serves images listed on an FDA label.
 */
@RestController
@RequestMapping("/api/medicine-images")
public class MedicineImageController {

    private final DailyMedService dailyMed;

    public MedicineImageController(DailyMedService dailyMed) {
        this.dailyMed = dailyMed;
    }

    @GetMapping("/{setId}/{name:.+}")
    public ResponseEntity<byte[]> image(@PathVariable String setId, @PathVariable String name) {
        return dailyMed.image(setId, name)
                .map(res -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(res.headers().firstValue("Content-Type")
                                .filter(t -> t.startsWith("image/")).orElse("image/jpeg")))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                        .body(res.body()))
                .orElse(ResponseEntity.notFound().build());
    }
}
