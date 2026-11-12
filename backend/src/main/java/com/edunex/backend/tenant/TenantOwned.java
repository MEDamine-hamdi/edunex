package com.edunex.backend.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;

/** Base of everything that belongs to one university. */
@MappedSuperclass
public abstract class TenantOwned {

    @Column(name = "university_id")
    private Long universityId;

    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    @PrePersist
    protected void assignUniversity() {
        if (universityId == null) {
            long current = TenantContext.currentOrZero();
            if (current != 0) {
                universityId = current;
            }
        }
        if (universityId == null) {
            String name = getClass().getSimpleName();
            if (name.equals("Student") || name.equals("Professor") || name.equals("AdministrationAgent")) {
                throw new IllegalStateException("A " + name + " must belong to a university");
            }
        }
    }

    /** Second lock: a record of another university can never be loaded, even by its number. */
    @PostLoad
    protected void guardUniversity() {
        long current = TenantContext.currentOrZero();
        if (current != 0 && universityId != null && universityId != current) {
            throw new IllegalStateException("Access to another university refused");
        }
    }
}