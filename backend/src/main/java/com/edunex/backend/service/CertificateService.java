package com.edunex.backend.service;

import com.edunex.backend.dto.CertificateDto;
import com.edunex.backend.entity.Attendance;
import com.edunex.backend.entity.CertificateLink;
import com.edunex.backend.entity.Exam;
import com.edunex.backend.entity.ExamRetake;
import com.edunex.backend.entity.MedicalCertificate;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.AttendanceStatus;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.AdministrationAgentRepository;
import com.edunex.backend.repository.AttendanceRepository;
import com.edunex.backend.repository.CertificateLinkRepository;
import com.edunex.backend.repository.EnrollmentRepository;
import com.edunex.backend.repository.ExamRepository;
import com.edunex.backend.repository.ExamRetakeRepository;
import com.edunex.backend.repository.MedicalCertificateRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Medical certificates. Sent through the app, a certificate must be verified by an administrative agent; handed in
 * person, it is recorded (and counts at once). When a certificate is approved, the absences of the period are removed
 * and each missed exam becomes a make-up exam to schedule.
 */
@Service
@RequiredArgsConstructor
public class CertificateService {

    public record FileData(Resource resource, String filename) {
    }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final long MAX_FILE = 5L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("pdf", "png", "jpg", "jpeg");

    private final MedicalCertificateRepository certificates;
    private final CertificateLinkRepository links;
    private final ExamRetakeRepository retakes;
    private final AttendanceRepository attendanceRepository;
    private final ExamRepository examRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AdministrationAgentRepository agentRepository;
    private final NotificationService notificationService;
    private final FileStorage fileStorage;
    private final EliminationService eliminationService;

    @Value("${app.certificates.deadline-days:7}")
    private int deadlineDays;

    // ====================================================== the student

    @Transactional(readOnly = true)
    public CertificateDto.Coverage coverage(String email, LocalDate from, LocalDate to) {
        Student s = student(email);
        checkPeriod(from, to);
        return coverageOf(s.getId(), from, to);
    }

    @Transactional
    public CertificateDto.Row submit(String email, LocalDate from, LocalDate to, String note, List<Long> examIds, MultipartFile file) {
        Student s = student(email);
        checkPeriod(from, to);
        if (LocalDate.now().isAfter(to.plusDays(deadlineDays))) {
            throw new ConflictException("The deadline to send this certificate through the app has passed ("
                    + deadlineDays + " days after the end of the absence): bring it to the administration");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("The certificate file is required");
        }
        String fileName = checkFile(file);
        ensureNoOverlap(s.getId(), from, to);
        long waiting = certificates.findByStudentIdOrderByCreatedAtDesc(s.getId()).stream()
                .filter(c -> "PENDING".equals(c.getStatus())).count();
        if (waiting >= 3) {
            throw new ConflictException("You already have 3 certificates waiting for verification");
        }
        List<Long> exams = validExams(s.getId(), from, to, examIds);
        if (absencesIn(s.getId(), from, to).isEmpty() && exams.isEmpty()) {
            throw new BadRequestException("No absence or exam found in this period");
        }
        MedicalCertificate c = new MedicalCertificate();
        c.setStudentId(s.getId());
        c.setSource("APP");
        c.setStartDate(from);
        c.setEndDate(to);
        c.setNote(clean(note));
        c.setExamIds(join(exams));
        c.setFileKey(fileStorage.store(file));
        c.setFileName(fileName);
        c.setCreatedBy(name(s));
        c = certificates.save(c);

        List<User> agents = new ArrayList<>();
        agentRepository.findAll().stream().filter(a -> a.getStatus() == UserStatus.ACTIVE).forEach(agents::add);
        if (!agents.isEmpty()) {
            notificationService.notifyUsers(agents, NotificationType.ATTENDANCE_UPDATE,
                    "Nouveau certificat médical à vérifier : " + name(s) + " (" + from.format(DAY) + " - " + to.format(DAY) + ").");
        }
        return row(c, s);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto.Row> mine(String email) {
        Student s = student(email);
        return certificates.findByStudentIdOrderByCreatedAtDesc(s.getId()).stream().map(c -> row(c, s)).toList();
    }

    @Transactional(readOnly = true)
    public FileData myFile(String email, Long id) {
        Student s = student(email);
        MedicalCertificate c = certificate(id);
        if (!c.getStudentId().equals(s.getId())) {
            throw new AccessDeniedException("Not your certificate");
        }
        return file(c);
    }

    @Transactional
    public void withdraw(String email, Long id) {
        Student s = student(email);
        MedicalCertificate c = certificate(id);
        if (!c.getStudentId().equals(s.getId())) {
            throw new AccessDeniedException("Not your certificate");
        }
        if (!"PENDING".equals(c.getStatus())) {
            throw new ConflictException("Only a certificate waiting for verification can be withdrawn");
        }
        c.setStatus("WITHDRAWN");
        if (c.getFileKey() != null) {
            fileStorage.delete(c.getFileKey());
            c.setFileKey(null);
        }
    }

    @Transactional(readOnly = true)
    public List<CertificateDto.RetakeItem> myRetakes(String email) {
        Student s = student(email);
        return retakeItems(retakes.findByStudentIdOrderByCreatedAtDesc(s.getId()));
    }

    // ====================================================== the administration

    @Transactional(readOnly = true)
    public List<CertificateDto.Row> list(String status) {
        List<MedicalCertificate> all = status == null || status.isBlank()
                ? certificates.findAllByOrderByCreatedAtDesc()
                : certificates.findByStatusOrderByCreatedAtAsc(status.trim().toUpperCase(Locale.ROOT));
        Map<Long, Student> students = studentRepository.findAllById(all.stream().map(MedicalCertificate::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Student::getId, s -> s));
        return all.stream().filter(c -> students.containsKey(c.getStudentId())).map(c -> row(c, students.get(c.getStudentId()))).toList();
    }

    @Transactional(readOnly = true)
    public CertificateDto.Detail detail(Long id) {
        MedicalCertificate c = certificate(id);
        Student s = studentById(c.getStudentId());
        List<CertificateDto.AbsenceItem> absences;
        if ("PENDING".equals(c.getStatus())) {
            absences = absencesIn(s.getId(), c.getStartDate(), c.getEndDate());
        } else {
            List<Long> ids = links.findByCertificateId(id).stream().map(CertificateLink::getAttendanceId).toList();
            absences = attendanceRepository.findAllById(ids).stream().sorted(Comparator.comparing(Attendance::getDate))
                    .map(a -> new CertificateDto.AbsenceItem(a.getId(), a.getDate(), a.getSubject().getName(), a.getStatus().name(), a.getJustification()))
                    .toList();
        }
        List<CertificateDto.ExamItem> exams = examRepository.findAllById(parseIds(c.getExamIds())).stream()
                .sorted(Comparator.comparing(Exam::getExamDate)).map(CertificateService::examItem).toList();
        return new CertificateDto.Detail(row(c, s), absences, exams, retakeItems(retakes.findByCertificateId(id)));
    }

    @Transactional(readOnly = true)
    public CertificateDto.Coverage coverageForStudent(Long studentId, LocalDate from, LocalDate to) {
        studentById(studentId);
        checkPeriod(from, to);
        return coverageOf(studentId, from, to);
    }

    @Transactional(readOnly = true)
    public FileData adminFile(Long id) {
        return file(certificate(id));
    }

    @Transactional
    public CertificateDto.Row approve(Long id, String agentEmail, String note) {
        MedicalCertificate c = certificate(id);
        if (!"PENDING".equals(c.getStatus())) {
            throw new ConflictException("This certificate was already processed");
        }
        String agent = actor(agentEmail);
        c.setStatus("APPROVED");
        c.setDecidedAt(java.time.LocalDateTime.now());
        c.setDecidedBy(agent);
        c.setDecisionNote(clean(note));
        applyApproval(c, agent);
        Student s = studentById(c.getStudentId());
        tell(s, NotificationType.ATTENDANCE_UPDATE, "Votre certificat médical du " + c.getStartDate().format(DAY) + " au "
                + c.getEndDate().format(DAY) + " a été validé : vos absences sont justifiées."
                + (c.getExamIds() != null && !c.getExamIds().isBlank() ? " Un rattrapage vous sera proposé pour l'examen manqué." : ""));
        return row(c, s);
    }

    @Transactional
    public CertificateDto.Row reject(Long id, String agentEmail, String reason) {
        MedicalCertificate c = certificate(id);
        if (!"PENDING".equals(c.getStatus())) {
            throw new ConflictException("This certificate was already processed");
        }
        c.setStatus("REJECTED");
        c.setDecidedAt(java.time.LocalDateTime.now());
        c.setDecidedBy(actor(agentEmail));
        c.setDecisionNote(clean(reason));
        Student s = studentById(c.getStudentId());
        tell(s, NotificationType.ATTENDANCE_UPDATE, "Votre certificat médical du " + c.getStartDate().format(DAY) + " au "
                + c.getEndDate().format(DAY) + " a été refusé. Motif : " + reason.trim());
        return row(c, s);
    }

    @Transactional
    public CertificateDto.Row revoke(Long id, String agentEmail, String reason) {
        MedicalCertificate c = certificate(id);
        if (!"APPROVED".equals(c.getStatus())) {
            throw new ConflictException("Only an approved certificate can be revoked");
        }
        for (CertificateLink l : links.findByCertificateId(id)) {
            attendanceRepository.findById(l.getAttendanceId()).ifPresent(a -> {
                a.setStatus(AttendanceStatus.valueOf(l.getPreviousStatus()));
                a.setJustification(l.getPreviousJustification());
            });
        }
        links.deleteByCertificateId(id);
        for (ExamRetake r : retakes.findByCertificateId(id)) {
            if ("TO_SCHEDULE".equals(r.getStatus()) || "SCHEDULED".equals(r.getStatus())) {
                r.setStatus("CANCELLED");
            }
        }
        c.setStatus("REVOKED");
        c.setDecidedAt(java.time.LocalDateTime.now());
        c.setDecidedBy(actor(agentEmail));
        c.setDecisionNote(clean(reason));
        eliminationService.recomputeAll();
        Student s = studentById(c.getStudentId());
        tell(s, NotificationType.ATTENDANCE_UPDATE, "La validation de votre certificat médical du " + c.getStartDate().format(DAY)
                + " au " + c.getEndDate().format(DAY) + " a été annulée." + (reason == null || reason.isBlank() ? "" : " Motif : " + reason.trim()));
        return row(c, s);
    }

    /** The student handed the paper to the administration: it is recorded and counts at once. */
    @Transactional
    public CertificateDto.Row inPerson(String agentEmail, Long studentId, LocalDate from, LocalDate to, String note,
                                       List<Long> examIds, MultipartFile file) {
        Student s = studentById(studentId);
        checkPeriod(from, to);
        ensureNoOverlap(studentId, from, to);
        List<Long> exams = validExams(studentId, from, to, examIds);
        if (absencesIn(studentId, from, to).isEmpty() && exams.isEmpty()) {
            throw new BadRequestException("No absence or exam found in this period");
        }
        String agent = actor(agentEmail);
        MedicalCertificate c = new MedicalCertificate();
        c.setStudentId(studentId);
        c.setSource("IN_PERSON");
        c.setStatus("APPROVED");
        c.setStartDate(from);
        c.setEndDate(to);
        c.setNote(clean(note));
        c.setExamIds(join(exams));
        c.setCreatedBy(agent);
        c.setDecidedAt(java.time.LocalDateTime.now());
        c.setDecidedBy(agent);
        if (file != null && !file.isEmpty()) {
            c.setFileName(checkFile(file));
            c.setFileKey(fileStorage.store(file));
        }
        c = certificates.save(c);
        applyApproval(c, agent);
        tell(s, NotificationType.ATTENDANCE_UPDATE, "Votre certificat médical du " + from.format(DAY) + " au " + to.format(DAY)
                + " a été enregistré par l'administration : vos absences sont justifiées."
                + (exams.isEmpty() ? "" : " Un rattrapage vous sera proposé pour l'examen manqué."));
        return row(c, s);
    }

    @Transactional(readOnly = true)
    public List<CertificateDto.StudentHit> searchStudents(String q) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        return studentRepository.findAll().stream()
                .filter(s -> needle.isEmpty() || (name(s) + " " + s.getStudentNumber()).toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                .limit(20).map(s -> new CertificateDto.StudentHit(s.getId(), name(s), s.getStudentNumber())).toList();
    }

    // ====================================================== make-up exams

    @Transactional(readOnly = true)
    public List<CertificateDto.RetakeItem> allRetakes() {
        return retakeItems(retakes.findAllByOrderByCreatedAtDesc());
    }

    @Transactional
    public List<CertificateDto.RetakeItem> schedule(CertificateDto.ScheduleRequest req, String agentEmail) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BadRequestException("The end time must be after the start time");
        }
        if (req.date().isBefore(LocalDate.now())) {
            throw new BadRequestException("The date is in the past");
        }
        Set<Long> ids = new LinkedHashSet<>(req.retakeIds());
        List<ExamRetake> list = retakes.findAllById(ids);
        if (list.size() != ids.size()) {
            throw new ResourceNotFoundException("Make-up exam not found");
        }
        for (ExamRetake r : list) {
            if (!"TO_SCHEDULE".equals(r.getStatus()) && !"SCHEDULED".equals(r.getStatus())) {
                throw new ConflictException("A make-up exam that is done or cancelled cannot be scheduled");
            }
        }
        String room = req.room() == null || req.room().isBlank() ? null : req.room().trim();
        List<Exam> sameDay = examRepository.findByExamDate(req.date()).stream()
                .filter(e -> "PLANNED".equals(e.getStatus()) && overlaps(e.getStartTime(), e.getEndTime(), req.startTime(), req.endTime())).toList();
        List<ExamRetake> others = retakes.findByRetakeDate(req.date()).stream()
                .filter(x -> "SCHEDULED".equals(x.getStatus()) && !ids.contains(x.getId())
                        && overlaps(x.getStartTime(), x.getEndTime(), req.startTime(), req.endTime())).toList();
        if (room != null) {
            if (sameDay.stream().anyMatch(e -> room.equalsIgnoreCase(e.getRoom())) || others.stream().anyMatch(x -> room.equalsIgnoreCase(x.getRoom()))) {
                throw new ConflictException("This room is already used at this time");
            }
        }
        String agent = actor(agentEmail);
        for (ExamRetake r : list) {
            Student s = studentById(r.getStudentId());
            Set<Long> classIds = enrollmentRepository.findByStudentIdAndStatus(s.getId(), EnrollmentStatus.ACTIVE).stream()
                    .map(e -> e.getSchoolClass().getId()).collect(Collectors.toSet());
            if (sameDay.stream().anyMatch(e -> e.getClasses().stream().anyMatch(c -> classIds.contains(c.getId())))) {
                throw new ConflictException(name(s) + " has an exam at this time");
            }
            if (others.stream().anyMatch(x -> x.getStudentId().equals(s.getId()))) {
                throw new ConflictException(name(s) + " already has a make-up exam at this time");
            }
        }
        for (ExamRetake r : list) {
            r.setRetakeDate(req.date());
            r.setStartTime(req.startTime());
            r.setEndTime(req.endTime());
            r.setRoom(room);
            r.setStatus("SCHEDULED");
            r.setScheduledBy(agent);
            Exam exam = examRepository.findById(r.getExamId()).orElse(null);
            tell(studentById(r.getStudentId()), NotificationType.TIMETABLE_CHANGE, "Rattrapage planifié"
                    + (exam != null ? " : " + exam.getSubject().getName() + " (" + exam.getExamType() + ")" : "") + " le "
                    + req.date().format(DAY) + " de " + req.startTime().format(HOUR) + " à " + req.endTime().format(HOUR)
                    + (room != null ? ", salle " + room : "") + ".");
        }
        return retakeItems(list);
    }

    @Transactional
    public void cancelRetake(Long id) {
        ExamRetake r = retake(id);
        if ("DONE".equals(r.getStatus())) {
            throw new ConflictException("This make-up exam is already done");
        }
        r.setStatus("CANCELLED");
    }

    @Transactional
    public void markRetakeDone(Long id) {
        ExamRetake r = retake(id);
        if (!"SCHEDULED".equals(r.getStatus())) {
            throw new ConflictException("Only a scheduled make-up exam can be marked as done");
        }
        r.setStatus("DONE");
    }

    // ====================================================== helpers

    private void applyApproval(MedicalCertificate c, String actor) {
        String text = "Absence justifiée : certificat médical du " + c.getStartDate().format(DAY) + " au "
                + c.getEndDate().format(DAY) + " (validé par " + actor + ")";
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        for (Attendance a : attendanceRepository.findByStudentId(c.getStudentId())) {
            if (a.getStatus() == AttendanceStatus.ABSENT && !a.getDate().isBefore(c.getStartDate()) && !a.getDate().isAfter(c.getEndDate())) {
                CertificateLink l = new CertificateLink();
                l.setCertificateId(c.getId());
                l.setAttendanceId(a.getId());
                l.setPreviousStatus(a.getStatus().name());
                l.setPreviousJustification(a.getJustification());
                links.save(l);
                a.setStatus(AttendanceStatus.PRESENT);
                a.setJustification(text);
            }
        }
        for (Long examId : parseIds(c.getExamIds())) {
            Exam exam = examRepository.findById(examId).orElse(null);
            if (exam == null || !"PLANNED".equals(exam.getStatus())) {
                continue;
            }
            ExamRetake r = retakes.findByStudentIdAndExamId(c.getStudentId(), examId).orElseGet(ExamRetake::new);
            if (r.getId() != null && !"CANCELLED".equals(r.getStatus())) {
                continue;
            }
            r.setStudentId(c.getStudentId());
            r.setExamId(examId);
            r.setCertificateId(c.getId());
            r.setStatus("TO_SCHEDULE");
            r.setRetakeDate(null);
            r.setStartTime(null);
            r.setEndTime(null);
            r.setRoom(null);
            retakes.save(r);
        }
        eliminationService.recomputeAll();
    }

    private CertificateDto.Coverage coverageOf(Long studentId, LocalDate from, LocalDate to) {
        List<CertificateDto.ExamItem> exams = examEntities(studentId, from, to).stream()
                .filter(e -> retakes.findByStudentIdAndExamId(studentId, e.getId()).filter(r -> !"CANCELLED".equals(r.getStatus())).isEmpty())
                .sorted(Comparator.comparing(Exam::getExamDate)).map(CertificateService::examItem).toList();
        return new CertificateDto.Coverage(absencesIn(studentId, from, to), exams);
    }

    private List<CertificateDto.AbsenceItem> absencesIn(Long studentId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT && !a.getDate().isBefore(from) && !a.getDate().isAfter(to))
                .sorted(Comparator.comparing(Attendance::getDate))
                .map(a -> new CertificateDto.AbsenceItem(a.getId(), a.getDate(), a.getSubject().getName(), a.getStatus().name(), a.getJustification()))
                .toList();
    }

    private List<Exam> examEntities(Long studentId, LocalDate from, LocalDate to) {
        Set<Long> classIds = enrollmentRepository.findByStudentIdAndStatus(studentId, EnrollmentStatus.ACTIVE).stream()
                .map(e -> e.getSchoolClass().getId()).collect(Collectors.toSet());
        if (classIds.isEmpty()) {
            return List.of();
        }
        return examRepository.findForClasses(classIds, from, to).stream().filter(e -> "PLANNED".equals(e.getStatus())).toList();
    }

    private List<Long> validExams(Long studentId, LocalDate from, LocalDate to, List<Long> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        Set<Long> allowed = coverageOf(studentId, from, to).exams().stream().map(CertificateDto.ExamItem::examId).collect(Collectors.toSet());
        Set<Long> out = new LinkedHashSet<>();
        for (Long id : requested) {
            if (!allowed.contains(id)) {
                throw new BadRequestException("An exam does not belong to this student or to this period");
            }
            out.add(id);
        }
        return new ArrayList<>(out);
    }

    private void ensureNoOverlap(Long studentId, LocalDate from, LocalDate to) {
        boolean clash = certificates.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .filter(c -> "PENDING".equals(c.getStatus()) || "APPROVED".equals(c.getStatus()))
                .anyMatch(c -> !c.getEndDate().isBefore(from) && !c.getStartDate().isAfter(to));
        if (clash) {
            throw new ConflictException("A certificate already covers these dates");
        }
    }

    private void checkPeriod(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("Invalid period");
        }
        if (ChronoUnit.DAYS.between(from, to) > 60) {
            throw new BadRequestException("A certificate covers at most 60 days");
        }
        if (from.isBefore(LocalDate.now().minusDays(120))) {
            throw new BadRequestException("The period is too old");
        }
    }

    private static String checkFile(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "certificate" : file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        if (name.isEmpty()) {
            name = "certificate";
        }
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (file.getSize() > MAX_FILE || !TYPES.contains(ext) || !signatureOk(ext, file)) {
            throw new BadRequestException("The certificate must be a PDF, PNG or JPG file of 5 MB maximum");
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }

    private static boolean signatureOk(String ext, MultipartFile file) {
        byte[] h = new byte[16];
        int n;
        try (InputStream in = file.getInputStream()) {
            n = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            return false;
        }
        switch (ext) {
            case "pdf":
                return n >= 4 && h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            case "png":
                return n >= 4 && (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "jpg":
            case "jpeg":
                return n >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF;
            default:
                return false;
        }
    }

    private FileData file(MedicalCertificate c) {
        if (c.getFileKey() == null) {
            throw new ResourceNotFoundException("No file for this certificate");
        }
        return new FileData(fileStorage.load(c.getFileKey()), c.getFileName() == null ? "certificate" : c.getFileName());
    }

    private List<CertificateDto.RetakeItem> retakeItems(List<ExamRetake> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<Long, Exam> exams = examRepository.findAllById(list.stream().map(ExamRetake::getExamId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Exam::getId, e -> e));
        Map<Long, Student> students = studentRepository.findAllById(list.stream().map(ExamRetake::getStudentId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Student::getId, s -> s));
        List<CertificateDto.RetakeItem> out = new ArrayList<>();
        for (ExamRetake r : list) {
            Exam e = exams.get(r.getExamId());
            Student s = students.get(r.getStudentId());
            if (e == null || s == null) {
                continue;
            }
            out.add(new CertificateDto.RetakeItem(r.getId(), e.getId(), e.getSubject().getName(), e.getExamType(), e.getExamDate(),
                    s.getId(), name(s), s.getStudentNumber(), r.getStatus(), r.getRetakeDate(), r.getStartTime(), r.getEndTime(), r.getRoom()));
        }
        return out;
    }

    private static CertificateDto.ExamItem examItem(Exam e) {
        return new CertificateDto.ExamItem(e.getId(), e.getSubject().getName(), e.getExamType(), e.getExamDate(), e.getStartTime(), e.getRoom());
    }

    private CertificateDto.Row row(MedicalCertificate c, Student s) {
        return new CertificateDto.Row(c.getId(), c.getStudentId(), name(s), s.getStudentNumber(), c.getSource(), c.getStatus(),
                c.getStartDate(), c.getEndDate(), c.getNote(), c.getFileKey() != null, c.getFileName(), c.getCreatedAt(),
                c.getCreatedBy(), c.getDecidedAt(), c.getDecidedBy(), c.getDecisionNote());
    }

    private void tell(Student s, NotificationType type, String message) {
        List<User> who = new ArrayList<>();
        who.add(s);
        notificationService.notifyUsers(who, type, message);
    }

    private static boolean overlaps(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
        if (s1 == null || e1 == null) {
            return false;
        }
        return s1.isBefore(e2) && s2.isBefore(e1);
    }

    private static List<Long> parseIds(String csv) {
        List<Long> out = new ArrayList<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                try {
                    out.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException ignored) {
                    // not an id
                }
            }
        }
        return out;
    }

    private static String join(List<Long> ids) {
        return ids.isEmpty() ? null : ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String actor(String email) {
        return userRepository.findByEmail(email).map(CertificateService::name).orElse(email);
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private Student student(String email) {
        return userRepository.findByEmail(email).filter(u -> u instanceof Student).map(u -> (Student) u)
                .orElseThrow(() -> new AccessDeniedException("Student account required"));
    }

    private Student studentById(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private MedicalCertificate certificate(Long id) {
        return certificates.findById(id).orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));
    }

    private ExamRetake retake(Long id) {
        return retakes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Make-up exam not found"));
    }
}