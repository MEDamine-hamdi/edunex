package com.edunex.backend.tenant;

import com.edunex.backend.entity.*;

import java.util.List;

/** Generated: every entity that belongs to a university. */
public final class TenantClasses {

    public static final List<Class<? extends TenantOwned>> ALL = List.of(User.class, SchoolClass.class, Section.class, Subject.class, Enrollment.class, Course.class, CourseDocument.class, Timetable.class, TimetableSession.class, Grade.class, Attendance.class, Announcement.class, Notification.class, ChatSession.class, InstituteDocument.class, DocumentRequest.class, DocumentType.class, SessionAbsence.class, Conversation.class, Exam.class, AbsencePolicy.class, Elimination.class, Internship.class, Defense.class, ClassTeaching.class);

    private TenantClasses() {
    }
}