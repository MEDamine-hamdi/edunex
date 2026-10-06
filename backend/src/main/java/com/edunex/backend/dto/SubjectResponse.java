package com.edunex.backend.dto;

import java.util.List;

public record SubjectResponse(
        Long id,
        String name,
        String code,
        Double coefficient,
        Integer credits,
        List<PersonRef> professors
) {
    public record PersonRef(Long id, String firstName, String lastName) {
    }
}
