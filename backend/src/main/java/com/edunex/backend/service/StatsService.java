package com.edunex.backend.service;

import com.edunex.backend.dto.PlatformStats;
import com.edunex.backend.dto.UniversityStats;
import com.edunex.backend.entity.ActivityLog;
import com.edunex.backend.entity.Grade;
import com.edunex.backend.entity.Role;
import com.edunex.backend.enums.AttendanceStatus;
import com.edunex.backend.enums.EnrollmentStatus;
import com.edunex.backend.enums.UserStatus;
import com.edunex.backend.repository.ActivityLogRepository;
import com.edunex.backend.repository.CourseDocumentRepository;
import com.edunex.backend.repository.CourseRepository;
import com.edunex.backend.repository.GradeRepository;
import com.edunex.backend.repository.PlatformSettingRepository;
import com.edunex.backend.repository.ProfessorRepository;
import com.edunex.backend.repository.RoleRepository;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.StudentRepository;
import com.edunex.backend.repository.SubjectRepository;
import com.edunex.backend.repository.TimetableSessionRepository;
import com.edunex.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final EntityManager em;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final CourseRepository courseRepository;
    private final CourseDocumentRepository documentRepository;
    private final TimetableSessionRepository sessionRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ActivityLogRepository logRepository;
    private final PlatformSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public UniversityStats university() {
        UniversityStats.Counts counts = new UniversityStats.Counts(
                studentRepository.count(),
                scalar("select count(s) from Student s where s.status = :st", UserStatus.ACTIVE),
                professorRepository.count(),
                classRepository.count(),
                subjectRepository.count(),
                scalar("select count(e) from Enrollment e where e.status = :st", EnrollmentStatus.ACTIVE),
                courseRepository.count(),
                documentRepository.count(),
                sessionRepository.count());

        List<UniversityStats.ClassCount> byClass = em.createQuery(
                        "select c.name, count(e) from Enrollment e join e.schoolClass c "
                                + "where e.status = :st group by c.name order by c.name", Object[].class)
                .setParameter("st", EnrollmentStatus.ACTIVE)
                .getResultList().stream()
                .map(r -> new UniversityStats.ClassCount((String) r[0], (Long) r[1]))
                .toList();

        return new UniversityStats(counts, byClass, attendance(), grades());
    }

    private UniversityStats.AttendanceStats attendance() {
        long present = 0;
        long absent = 0;
        long late = 0;
        for (Object[] r : em.createQuery("select a.status, count(a) from Attendance a group by a.status", Object[].class)
                .getResultList()) {
            long n = (Long) r[1];
            switch ((AttendanceStatus) r[0]) {
                case PRESENT -> present = n;
                case ABSENT -> absent = n;
                case LATE -> late = n;
            }
        }
        long total = present + absent + late;

        Map<String, long[]> bySubject = new TreeMap<>();
        for (Object[] r : em.createQuery(
                        "select s.name, a.status, count(a) from Attendance a join a.subject s group by s.name, a.status",
                        Object[].class).getResultList()) {
            long[] totals = bySubject.computeIfAbsent((String) r[0], k -> new long[2]);
            long n = (Long) r[2];
            totals[1] += n;
            if (r[1] == AttendanceStatus.ABSENT) {
                totals[0] += n;
            }
        }
        List<UniversityStats.SubjectAbsence> subjects = bySubject.entrySet().stream()
                .map(e -> new UniversityStats.SubjectAbsence(e.getKey(), e.getValue()[0], e.getValue()[1],
                        rate(e.getValue()[0], e.getValue()[1])))
                .toList();
        return new UniversityStats.AttendanceStats(present, absent, late, total, rate(absent, total), subjects);
    }

    private UniversityStats.GradeStats grades() {
        List<Grade> grades = gradeRepository.findAll();
        long[] buckets = new long[4];
        double sum = 0;
        for (Grade g : grades) {
            double v = g.getValue();
            sum += v;
            buckets[v < 5 ? 0 : v < 10 ? 1 : v < 15 ? 2 : 3]++;
        }
        Double average = grades.isEmpty() ? null : round2(sum / grades.size());
        List<UniversityStats.Bucket> distribution = List.of(
                new UniversityStats.Bucket("0-5", buckets[0]),
                new UniversityStats.Bucket("5-10", buckets[1]),
                new UniversityStats.Bucket("10-15", buckets[2]),
                new UniversityStats.Bucket("15-20", buckets[3]));

        Map<String, DoubleSummaryStatistics> bySubject = grades.stream().collect(Collectors.groupingBy(
                g -> g.getSubject().getName(), TreeMap::new, Collectors.summarizingDouble(Grade::getValue)));
        List<UniversityStats.SubjectAverage> subjects = bySubject.entrySet().stream()
                .map(e -> new UniversityStats.SubjectAverage(e.getKey(), round2(e.getValue().getAverage()), e.getValue().getCount()))
                .toList();
        return new UniversityStats.GradeStats(grades.size(), average, distribution, subjects);
    }

    @Transactional(readOnly = true)
    public PlatformStats platform() {
        List<PlatformStats.RoleCount> byRole = roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(r -> new PlatformStats.RoleCount(r.getName(), userRepository.countByRoleId(r.getId())))
                .toList();
        List<PlatformStats.StatusCount> byStatus = Arrays.stream(UserStatus.values())
                .map(s -> new PlatformStats.StatusCount(s.name(), userRepository.countByStatus(s)))
                .toList();

        LocalDate today = LocalDate.now();
        Map<LocalDate, long[]> days = new TreeMap<>();
        for (int i = 6; i >= 0; i--) {
            days.put(today.minusDays(i), new long[3]);
        }
        for (ActivityLog l : logRepository.findByTimestampAfter(today.minusDays(6).atStartOfDay())) {
            long[] d = days.get(l.getTimestamp().toLocalDate());
            if (d == null) {
                continue;
            }
            String a = l.getAction();
            if (a.equals("LOGIN")) {
                d[0]++;
            } else if (a.startsWith("LOGIN_FAILED") || a.startsWith("LOGIN_LOCKED")) {
                d[1]++;
            } else if (a.startsWith("CREATE ") || a.startsWith("UPDATE ") || a.startsWith("DELETE ")) {
                d[2]++;
            }
        }
        List<PlatformStats.DayActivity> last7 = days.entrySet().stream()
                .map(e -> new PlatformStats.DayActivity(e.getKey().toString(), e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                .toList();

        return new PlatformStats(userRepository.count(), byRole, byStatus,
                logRepository.count(), settingRepository.count(), last7);
    }

    private long scalar(String jpql, Object status) {
        return em.createQuery(jpql, Long.class).setParameter("st", status).getSingleResult();
    }

    private static double rate(long part, long total) {
        return total == 0 ? 0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}