package com.edunex.backend.repository;

import com.edunex.backend.entity.AnnouncementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AnnouncementAttachmentRepository extends JpaRepository<AnnouncementAttachment, Long> {

    List<AnnouncementAttachment> findByAnnouncementIdIn(Collection<Long> announcementIds);
}