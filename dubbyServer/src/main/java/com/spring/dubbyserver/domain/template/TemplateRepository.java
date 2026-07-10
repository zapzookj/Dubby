package com.spring.dubbyserver.domain.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    List<Template> findByTypeAndStatusAndLocaleOrderByIdAsc(
            Template.TemplateType type, String status, String locale);

    Optional<Template> findByCode(String code);
}
