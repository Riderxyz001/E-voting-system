package com.evoting.evotingsystem.repository;

import com.evoting.evotingsystem.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    List<Faq> findTop6ByActiveTrueOrderByDisplayOrderAscCreatedAtDesc();
}
