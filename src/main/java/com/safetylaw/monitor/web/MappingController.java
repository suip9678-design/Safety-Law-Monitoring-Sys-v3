package com.safetylaw.monitor.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safetylaw.monitor.dto.MappingCreateRequest;
import com.safetylaw.monitor.dto.MappingResponse;
import com.safetylaw.monitor.service.MappingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mappings")
public class MappingController {

    private final MappingService mappingService;

    public MappingController(MappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public List<MappingResponse> list(
            @RequestParam(name = "document_id", required = false) Long documentId,
            @RequestParam(name = "tracked_law_id", required = false) Long trackedLawId) {
        return mappingService.list(documentId, trackedLawId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MappingResponse create(@Valid @RequestBody MappingCreateRequest request) {
        return mappingService.create(request);
    }

    @DeleteMapping("/{mappingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long mappingId) {
        mappingService.delete(mappingId);
    }
}
