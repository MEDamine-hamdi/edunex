package com.edunex.backend.service;

import com.edunex.backend.dto.InternshipDto;
import com.edunex.backend.dto.InternshipDto.DefenseDetail;
import com.edunex.backend.dto.InternshipDto.DefenseRow;
import com.edunex.backend.dto.InternshipDto.Juror;
import com.edunex.backend.dto.InternshipDto.PersonRef;
import com.edunex.backend.dto.InternshipDto.Row;
import com.edunex.backend.entity.Defense;
import com.edunex.backend.entity.DefenseJuror;
import com.edunex.backend.entity.Internship;
import com.edunex.backend.entity.Professor;
import com.edunex.backend.entity.Student;
import com.edunex.backend.entity.User;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ConflictException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.DefenseRepository;
import com.edunex.backend.repository.InternshipRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternshipService {

    public record Download(Resource resource, String filename) {
    }

    private static final List<String> ROLE_ORDER = List.of("PRESIDENT", "RAPPORTEUR", "MEMBER");
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private static final long MAX_REPORT = 25L * 1024 * 1024;
    private static final long MAX_RECORD = 15L * 1024 * 1024;

    private final InternshipRepository internships;
    private final DefenseRepository defenses;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;
    private final FileStorage fileStorage;
    private final NotificationService notificationService;

    // ---------- administration: stages ----------

    @Transactional(readOnly = true)
    public InternshipDto.Lookups lookups() {
        return new InternshipDto.Lookups(
                studentRepository.findAll().stream().filter(s -> s.getStatus() == UserStatus.ACTIVE)
                        .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName))
                        .map(InternshipService::ref).toList(),
                professorRepository.findAll().stream().filter(p -> p.getStatus() == UserStatus.ACTIVE)
                        .sorted(Comparator.comparing(Professor::getLastName).thenComparing(Professor::getFirstName))
                        .map(InternshipService::ref).toList());
    }

    @Transactional(readOnly = true)
    public List<Row> internships() {
        return rows(internships.findAllByOrderByStartDateDesc());
    }

    @Transactional
    public Row createInternship(InternshipDto.Save req) {
        Internship i = new Internship();
        apply(i, req);
        return rows(List.of(internships.save(i))).get(0);
    }

    @Transactional
    public Row updateInternship(Long id, InternshipDto.Save req) {
        Internship i = internship(id);
        apply(i, req);
        return rows(List.of(i)).get(0);
    }

    @Transactional
    public void deleteInternship(Long id) {
        Internship i = internship(id);
        defenses.findByInternshipId(id).ifPresent(d -> {
            deleteFile(d.getReportKey());
            deleteFile(d.getRecordKey());
            defenses.delete(d);
        });
        internships.delete(i);
    }

    // ---------- administration: soutenances ----------

    @Transactional(readOnly = true)
    public List<DefenseRow> allDefenses() {
        return defenses.findAllByOrderByScheduledAtDesc().stream().map(this::toRow).toList();
    }

    @Transactional
    public DefenseDetail saveDefense(Long id, InternshipDto.DefenseSave req) {
        Internship i = internship(req.internshipId());
        Defense d;
        if (id == null) {
            if (defenses.findByInternshipId(i.getId()).isPresent()) {
                throw new ConflictException("A defense is already planned for this internship");
            }
            d = new Defense();
            d.setInternship(i);
        } else {
            d = defense(id);
            if (!d.getInternship().getId().equals(i.getId())) {
                throw new BadRequestException("The internship of a defense cannot change");
            }
        }
        validateJury(req.jury());
        d.setScheduledAt(req.scheduledAt());
        d.setRoom(req.room() == null || req.room().isBlank() ? null : req.room().trim());

        Map<Long, DefenseJuror> existing = d.getJurors().stream()
                .collect(Collectors.toMap(j -> j.getProfessor().getId(), Function.identity()));
        Set<Long> wanted = req.jury().stream().map(InternshipDto.JurorInput::professorId).collect(Collectors.toSet());
        d.getJurors().removeIf(j -> !wanted.contains(j.getProfessor().getId()));
        for (InternshipDto.JurorInput in : req.jury()) {
            DefenseJuror j = existing.get(in.professorId());
            if (j == null) {
                j = new DefenseJuror();
                j.setDefense(d);
                j.setProfessor(professorRepository.findById(in.professorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Professor not found")));
                d.getJurors().add(j);
            }
            j.setRole(in.role().trim().toUpperCase(Locale.ROOT));
        }
        d = defenses.save(d);
        notifyAll(d, "Soutenance planifiée : « " + i.getTitle() + " » le " + d.getScheduledAt().format(WHEN)
                + (d.getRoom() != null ? ", salle " + d.getRoom() : "") + ".");
        return toDetail(d);
    }

    @Transactional
    public DefenseDetail setResult(Long id, InternshipDto.ResultInput req) {
        Defense d = defense(id);
        d.setGrade(req.grade());
        d.setComments(req.comments() == null || req.comments().isBlank() ? null : req.comments().trim());
        d.setStatus("DONE");
        notificationService.notifyUsers(List.of(d.getInternship().getStudent()), NotificationType.INTERNSHIP,
                "Le résultat de votre soutenance « " + d.getInternship().getTitle() + " » est disponible.");
        return toDetail(d);
    }

    @Transactional
    public DefenseDetail attach(Long id, String kind, MultipartFile file) {
        boolean report = "report".equals(kind);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > (report ? MAX_REPORT : MAX_RECORD)) {
            throw new BadRequestException("File is too large");
        }
        String name = displayName(file);
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        boolean allowed = report ? ext.equals("pdf") : Set.of("pdf", "png", "jpg", "jpeg").contains(ext);
        if (!allowed) {
            throw new BadRequestException(report ? "The report must be a PDF" : "Use a PDF, PNG or JPG file");
        }
        if (!FileSignatures.matches(ext, file)) {
            throw new BadRequestException("The content of the file does not match its type");
        }
        Defense d = defense(id);
        String old = report ? d.getReportKey() : d.getRecordKey();
        String key = fileStorage.store(file);
        if (report) {
            d.setReportKey(key);
            d.setReportName(name);
        } else {
            d.setRecordKey(key);
            d.setRecordName(name);
        }
        deleteFile(old);
        return toDetail(d);
    }

    @Transactional
    public DefenseDetail removeFile(Long id, String kind) {
        Defense d = defense(id);
        if ("report".equals(kind)) {
            deleteFile(d.getReportKey());
            d.setReportKey(null);
            d.setReportName(null);
        } else {
            deleteFile(d.getRecordKey());
            d.setRecordKey(null);
            d.setRecordName(null);
        }
        return toDetail(d);
    }

    // ---------- reading (agents, the student, the supervisor and the jury) ----------

    @Transactional(readOnly = true)
    public DefenseDetail detail(String email, boolean manager, Long id) {
        Defense d = defense(id);
        check(d, email, manager);
        return toDetail(d);
    }

    @Transactional(readOnly = true)
    public Download download(String email, boolean manager, Long id, String kind) {
        Defense d = defense(id);
        check(d, email, manager);
        boolean report = "report".equals(kind);
        String key = report ? d.getReportKey() : d.getRecordKey();
        if (key == null) {
            throw new ResourceNotFoundException("Document not found");
        }
        return new Download(fileStorage.load(key), report ? d.getReportName() : d.getRecordName());
    }

    @Transactional(readOnly = true)
    public List<Row> mineInternships(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (u instanceof Student) {
            return rows(internships.findByStudentIdOrderByStartDateDesc(u.getId()));
        }
        if (u instanceof Professor) {
            return rows(internships.findBySupervisorIdOrderByStartDateDesc(u.getId()));
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public List<DefenseRow> mineDefenses(String email) {
        User u = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (u instanceof Student) {
            return defenses.findByInternshipStudentIdOrderByScheduledAtDesc(u.getId()).stream().map(this::toRow).toList();
        }
        if (u instanceof Professor) {
            return defenses.findForProfessor(u.getId()).stream().map(this::toRow).toList();
        }
        return List.of();
    }

    // ---------- helpers ----------

    private void check(Defense d, String email, boolean manager) {
        if (manager) {
            return;
        }
        User u = userRepository.findByEmail(email).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        Internship i = d.getInternship();
        boolean student = u instanceof Student && i.getStudent().getId().equals(u.getId());
        boolean professor = u instanceof Professor && (i.getSupervisor().getId().equals(u.getId())
                || d.getJurors().stream().anyMatch(j -> j.getProfessor().getId().equals(u.getId())));
        if (!student && !professor) {
            throw new AccessDeniedException("Not allowed");
        }
    }

    private void apply(Internship i, InternshipDto.Save req) {
        if (req.startDate() != null && req.endDate() != null && req.endDate().isBefore(req.startDate())) {
            throw new BadRequestException("The end date is before the start date");
        }
        i.setStudent(studentRepository.findById(req.studentId()).orElseThrow(() -> new ResourceNotFoundException("Student not found")));
        i.setSupervisor(professorRepository.findById(req.supervisorId()).orElseThrow(() -> new ResourceNotFoundException("Professor not found")));
        i.setCompany(blank(req.company()));
        i.setCompanySupervisor(blank(req.companySupervisor()));
        i.setTitle(req.title().trim());
        i.setKind(blank(req.kind()));
        i.setStartDate(req.startDate());
        i.setEndDate(req.endDate());
    }

    private static void validateJury(List<InternshipDto.JurorInput> jury) {
        Set<Long> ids = new HashSet<>();
        int presidents = 0;
        int rapporteurs = 0;
        for (InternshipDto.JurorInput j : jury) {
            String role = j.role().trim().toUpperCase(Locale.ROOT);
            if (!ROLE_ORDER.contains(role)) {
                throw new BadRequestException("A juror is PRESIDENT, RAPPORTEUR or MEMBER");
            }
            if (!ids.add(j.professorId())) {
                throw new BadRequestException("A professor cannot appear twice in the jury");
            }
            presidents += role.equals("PRESIDENT") ? 1 : 0;
            rapporteurs += role.equals("RAPPORTEUR") ? 1 : 0;
        }
        if (presidents != 1 || rapporteurs != 1) {
            throw new BadRequestException("The jury needs exactly one president and one rapporteur");
        }
    }

    private void notifyAll(Defense d, String message) {
        Map<Long, User> people = new HashMap<>();
        Internship i = d.getInternship();
        people.put(i.getStudent().getId(), i.getStudent());
        people.put(i.getSupervisor().getId(), i.getSupervisor());
        d.getJurors().forEach(j -> people.put(j.getProfessor().getId(), j.getProfessor()));
        notificationService.notifyUsers(new ArrayList<>(people.values()), NotificationType.INTERNSHIP, message);
    }

    private List<Row> rows(List<Internship> list) {
        Map<Long, Defense> byInternship = defenses.findAll().stream()
                .collect(Collectors.toMap(d -> d.getInternship().getId(), Function.identity()));
        return list.stream().map(i -> {
            Defense d = byInternship.get(i.getId());
            return new Row(i.getId(), ref(i.getStudent()), ref(i.getSupervisor()), i.getCompany(), i.getCompanySupervisor(),
                    i.getTitle(), i.getKind(), i.getStartDate(), i.getEndDate(),
                    d == null ? null : d.getId(), d == null ? null : d.getStatus(), d == null ? null : d.getScheduledAt());
        }).toList();
    }

    private DefenseRow toRow(Defense d) {
        Internship i = d.getInternship();
        return new DefenseRow(d.getId(), d.getStatus(), d.getScheduledAt(), d.getRoom(), ref(i.getStudent()),
                ref(i.getSupervisor()), i.getTitle(), jury(d), d.getGrade(), mention(d.getGrade()));
    }

    private DefenseDetail toDetail(Defense d) {
        Internship i = d.getInternship();
        return new DefenseDetail(d.getId(), d.getStatus(), d.getScheduledAt(), d.getRoom(), d.getGrade(),
                mention(d.getGrade()), decision(d.getGrade()), d.getComments(), i.getId(), i.getTitle(), i.getCompany(),
                i.getCompanySupervisor(), i.getKind(), i.getStartDate(), i.getEndDate(), ref(i.getStudent()),
                ref(i.getSupervisor()), jury(d), d.getReportKey() != null, d.getReportName(),
                d.getRecordKey() != null, d.getRecordName());
    }

    private static List<Juror> jury(Defense d) {
        return d.getJurors().stream()
                .sorted(Comparator.comparingInt(j -> ROLE_ORDER.indexOf(j.getRole())))
                .map(j -> new Juror(j.getProfessor().getId(), name(j.getProfessor()), j.getProfessor().getEmail(), j.getRole()))
                .toList();
    }

    static String mention(Double g) {
        if (g == null) {
            return null;
        }
        if (g < 10) {
            return "Ajourné";
        }
        if (g < 12) {
            return "Passable";
        }
        if (g < 14) {
            return "Assez bien";
        }
        return g < 16 ? "Bien" : "Très bien";
    }

    static String decision(Double g) {
        return g == null ? null : (g >= 10 ? "Admis" : "Ajourné");
    }

    private static PersonRef ref(Student s) {
        return new PersonRef(s.getId(), name(s), s.getEmail(), s.getStudentNumber());
    }

    private static PersonRef ref(Professor p) {
        return new PersonRef(p.getId(), name(p), p.getEmail(), p.getEmployeeNumber());
    }

    private static String name(User u) {
        return u.getFirstName() + " " + u.getLastName();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String displayName(MultipartFile f) {
        String original = f.getOriginalFilename() == null ? "document" : f.getOriginalFilename();
        String n = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1).trim();
        return n.isEmpty() ? "document" : (n.length() > 200 ? n.substring(n.length() - 200) : n);
    }

    private void deleteFile(String key) {
        if (key != null) {
            fileStorage.delete(key);
        }
    }

    private Internship internship(Long id) {
        return internships.findById(id).orElseThrow(() -> new ResourceNotFoundException("Internship not found"));
    }

    private Defense defense(Long id) {
        return defenses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Defense not found"));
    }
}