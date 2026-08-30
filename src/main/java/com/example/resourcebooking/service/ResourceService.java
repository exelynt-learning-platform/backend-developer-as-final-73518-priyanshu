package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ResourceRequest;
import com.example.resourcebooking.dto.ResourceResponse;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.repository.ResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Page<ResourceResponse> findAll(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(ResourceResponse::new);
    }

    public ResourceResponse findById(Long id) {
        return resourceRepository.findById(id)
                .map(ResourceResponse::new)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(request.getName(), request.getDescription(), request.getType());
        return new ResourceResponse(resourceRepository.save(resource));
    }

    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        return new ResourceResponse(resourceRepository.save(resource));
    }

    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found: " + id);
        }
        resourceRepository.deleteById(id);
    }
}
