package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    List<Testimonial> findTop6ByActiveTrueOrderByCreatedAtDesc();
}
