package com.example.resourcebooking.dto;

import com.example.resourcebooking.entity.Resource;
import lombok.Getter;

@Getter
public class ResourceResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final String type;

    public ResourceResponse(Resource resource) {
        this.id = resource.getId();
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.type = resource.getType();
    }
}
