package com.example.resourcebooking.repository;

import com.example.resourcebooking.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
}
