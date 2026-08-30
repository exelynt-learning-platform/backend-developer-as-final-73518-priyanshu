package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    @Operation(summary = "List all resources (paginated)")
    public ResponseEntity<Page<ResourceResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(resourceService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource by ID")
    public ResponseEntity<ResourceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(resourceService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a resource (ADMIN only)")
    public ResponseEntity<ResourceResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody ResourceRequest request) {
        return ResponseEntity.ok(resourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a resource (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        resourceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
