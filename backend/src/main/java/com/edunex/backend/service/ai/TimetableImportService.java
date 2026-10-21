package com.edunex.backend.service.ai;

import com.edunex.backend.dto.ImportDto;
import com.edunex.backend.dto.SessionRequest;
import com.edunex.backend.dto.TimetableRequest;
import com.edunex.backend.enums.NotificationType;
import com.edunex.backend.exception.AiRateLimitedException;
import com.edunex.backend.exception.AiUnavailableException;
import com.edunex.backend.exception.BadRequestException;
import com.edunex.backend.exception.ResourceNotFoundException;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.service.NotificationService;
import com.edunex.backend.service.TimetableService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads a timetable PDF page by page with the AI model and proposes sessions. Nothing is created until the
 * administration confirms the (corrected) proposal. Jobs live in memory for a few hours.
 */
@Slf4j
@Service
public class TimetableImportService {

    public record RawSession(String day, String start, String end, String subject, String professor, String room,
                             String className) {
    }

    private static final class Job {
        final String owner;
        final Instant created = Instant.now();
        volatile String status = "PROCESSING";
        volatile String message;
        volatile int totalPages;
        volatile int donePages;
        volatile List<ImportDto.Row> rows = List.of();
        final List<String> warnings = new CopyOnWriteArrayList<>();

        Job(String owner) {
            this.owner = owner;
        }
    }

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String PROMPT = """
            You extract a university timetable from the text of ONE page of a PDF. The text keeps the horizontal layout of the page, so the columns of a grid are aligned with spaces.
            Reply with ONLY one JSON object, no explanation, in this exact shape:
            {"sessions":[{"day":"MONDAY","start":"08:30","end":"10:00","subject":"as written","professor":"as written or empty","room":"as written or empty","className":"class or group as written (page title or header) or empty"}]}
            Rules:
            - day is one of MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY (Lundi=MONDAY, Mardi=TUESDAY, Mercredi=WEDNESDAY, Jeudi=THURSDAY, Vendredi=FRIDAY, Samedi=SATURDAY, Dimanche=SUNDAY).
            - Times are 24-hour HH:mm. When a time slot is written once for a whole row of the grid, apply it to every cell of that row.
            - One object per course session. Ignore breaks, headers, legends, signatures and footers.
            - Never invent a session. If a field is unknown, use an empty string.
            """;

    private final GroqClient groq;
    private final TimetableImportResolver resolver;
    private final TimetableService timetableService;
    private final NotificationService notificationService;
    private final ProfessorRepository professorRepository;
    private final int pauseSeconds;
    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "timetable-import");
        t.setDaemon(true);
        return t;
    });

    public TimetableImportService(GroqClient groq, TimetableImportResolver resolver, TimetableService timetableService,
                                  NotificationService notificationService, ProfessorRepository professorRepository,
                                  @Value("${app.ai.import-pause-seconds:7}") int pauseSeconds) {
        this.groq = groq;
        this.resolver = resolver;
        this.timetableService = timetableService;
        this.notificationService = notificationService;
        this.professorRepository = professorRepository;
        this.pauseSeconds = pauseSeconds;
    }

    public ImportDto.JobView start(String email, MultipartFile file, Long timetableId, Long classId) {
        if (!groq.isConfigured()) {
            throw new AiUnavailableException("The AI assistant is not configured");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (file.isEmpty() || !name.endsWith(".pdf")) {
            throw new BadRequestException("Please upload a PDF file");
        }
        if (file.getSize() > 15L * 1024 * 1024) {
            throw new BadRequestException("The PDF is too large (15 MB maximum)");
        }
        List<String> pages;
        try {
            pages = LayoutPdfText.pages(file.getBytes()).stream()
                    .filter(p -> p.replaceAll("\\s", "").length() >= 30).toList();
        } catch (IOException e) {
            throw new BadRequestException("This PDF cannot be read");
        }
        if (pages.isEmpty()) {
            throw new BadRequestException("No readable text in this PDF (scanned documents are not supported)");
        }
        if (pages.size() > 30) {
            throw new BadRequestException("Too many pages (30 maximum)");
        }
        jobs.values().removeIf(j -> j.created.isBefore(Instant.now().minus(3, ChronoUnit.HOURS)));
        String id = UUID.randomUUID().toString();
        Job job = new Job(email);
        job.totalPages = pages.size();
        jobs.put(id, job);
        List<String> readyPages = pages;
        executor.submit(() -> process(job, readyPages, classId, timetableId));
        return view(id, job);
    }

    public ImportDto.JobView get(String email, String id) {
        return view(id, owned(email, id));
    }

    public void discard(String email, String id) {
        owned(email, id);
        jobs.remove(id);
    }

    public ImportDto.ConfirmResponse confirm(String email, String id, ImportDto.ConfirmRequest req) {
        owned(email, id);
        Long timetableId = req.timetableId();
        if (timetableId == null) {
            if (req.academicYear() == null || req.academicYear().isBlank() || req.semester() == null || req.semester().isBlank()) {
                throw new BadRequestException("Choose a timetable or give an academic year and a semester");
            }
            timetableId = timetableService.create(new TimetableRequest(req.academicYear(), req.semester())).id();
        }
        List<ImportDto.RowResult> results = new ArrayList<>();
        Set<Long> classIds = new HashSet<>();
        Set<Long> professorIds = new HashSet<>();
        int created = 0;
        List<ImportDto.ConfirmRow> rows = req.rows() == null ? List.of() : req.rows();
        for (int i = 0; i < rows.size(); i++) {
            ImportDto.ConfirmRow r = rows.get(i);
            try {
                timetableService.importSession(timetableId, new SessionRequest(DayOfWeek.valueOf(r.day()),
                        LocalTime.parse(r.start()), LocalTime.parse(r.end()), r.room() == null || r.room().isBlank() ? null : r.room(),
                        r.classId(), r.subjectId(), r.professorId()));
                results.add(new ImportDto.RowResult(i, true, null));
                classIds.add(r.classId());
                professorIds.add(r.professorId());
                created++;
            } catch (Exception e) {
                results.add(new ImportDto.RowResult(i, false, e.getMessage() == null ? "Erreur" : e.getMessage()));
            }
        }
        if (created > 0) {
            String text = "Emploi du temps mis à jour : de nouvelles séances ont été ajoutées.";
            classIds.forEach(c -> notificationService.notifyClassStudents(c, NotificationType.TIMETABLE_CHANGE, text));
            notificationService.notifyUsers(professorRepository.findAllById(professorIds),
                    NotificationType.TIMETABLE_CHANGE, text);
        }
        if (created == rows.size()) {
            jobs.remove(id);
        }
        return new ImportDto.ConfirmResponse(timetableId, created, rows.size() - created, results);
    }

    // ---------- background work ----------

    private void process(Job job, List<String> pages, Long classId, Long timetableId) {
        try {
            List<RawSession> all = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                try {
                    all.addAll(readPage(pages.get(i)));
                } catch (Exception e) {
                    job.warnings.add("Page " + (i + 1) + " non lue : " + e.getMessage());
                }
                job.donePages = i + 1;
                if (i < pages.size() - 1) {
                    Thread.sleep(pauseSeconds * 1000L);
                }
            }
            job.rows = resolver.resolve(all, classId, timetableId);
            job.message = all.isEmpty() ? "Aucune séance n'a été trouvée dans ce document." : null;
            job.status = "DONE";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            job.status = "FAILED";
            job.message = "Analyse interrompue";
        } catch (Exception e) {
            log.warn("Timetable import failed: {}", e.toString());
            job.status = "FAILED";
            job.message = e.getMessage();
        }
    }

    private List<RawSession> readPage(String text) throws InterruptedException {
        List<GroqClient.Message> messages = List.of(new GroqClient.Message("system", PROMPT),
                new GroqClient.Message("user", "PAGE TEXT:\n" + text));
        for (int attempt = 1; ; attempt++) {
            try {
                return parse(groq.complete(messages, 4000, "medium").content());
            } catch (AiRateLimitedException e) {
                if (attempt >= 3) {
                    throw e;
                }
                Thread.sleep(Math.min(e.getRetryAfterSeconds(), 40) * 1000L);
            }
        }
    }

    static List<RawSession> parse(String content) {
        int a = content.indexOf('{');
        int b = content.lastIndexOf('}');
        if (a < 0 || b <= a) {
            return List.of();
        }
        Map<?, ?> root = MAPPER.readValue(content.substring(a, b + 1), Map.class);
        List<RawSession> out = new ArrayList<>();
        if (root.get("sessions") instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    out.add(new RawSession(str(m.get("day")), str(m.get("start")), str(m.get("end")),
                            str(m.get("subject")), str(m.get("professor")), str(m.get("room")), str(m.get("className"))));
                }
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private Job owned(String email, String id) {
        Job job = jobs.get(id);
        if (job == null || !job.owner.equals(email)) {
            throw new ResourceNotFoundException("Import not found or expired");
        }
        return job;
    }

    private ImportDto.JobView view(String id, Job j) {
        return new ImportDto.JobView(id, j.status, j.message, j.totalPages, j.donePages, List.copyOf(j.warnings), j.rows);
    }
}