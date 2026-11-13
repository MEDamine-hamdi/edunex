package com.edunex.backend.config;

import com.edunex.backend.entity.*;
import com.edunex.backend.enums.*;
import com.edunex.backend.repository.*;
import com.edunex.backend.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Fills a separate "DEMO" university with realistic data so clients can test EduNex.
 *
 * <p>Only runs with the Spring profile {@code demo}. It never touches the other universities:
 * everything is created inside the DEMO tenant. If the DEMO university already exists nothing happens,
 * unless {@code app.demo.reset=true} (then the DEMO data is wiped and created again).
 *
 * <p>Login accounts (all use the password {@code app.demo.password}, default {@code Demo@2026}):
 * admin@demo.edunex.tn (administration), prof@demo.edunex.tn (professor), student@demo.edunex.tn (student).
 */
@Slf4j
@Component
@org.springframework.context.annotation.Profile("demo")
@RequiredArgsConstructor
public class DemoDataSeeder {

    private static final String CODE = "DEMO";
    private static final String DOMAIN = "@demo.edunex.tn";

    private static final String[] MALE = {"Mohamed", "Ahmed", "Youssef", "Amine", "Karim", "Walid", "Sami", "Omar",
            "Hichem", "Fedi", "Mehdi", "Rami", "Anis", "Aymen", "Bilel", "Nizar", "Slim", "Wassim", "Hamza", "Oussama"};
    private static final String[] FEMALE = {"Yasmine", "Salma", "Ines", "Meriem", "Amira", "Rania", "Nour", "Sarra",
            "Hiba", "Emna", "Ghada", "Mariem", "Aya", "Khouloud", "Rim", "Syrine", "Wafa", "Cyrine", "Oumaima", "Nesrine"};
    private static final String[] LAST = {"Ben Ali", "Trabelsi", "Gharbi", "Mansouri", "Jebali", "Bouazizi", "Chaabane",
            "Hamdi", "Khelifi", "Ayari", "Mejri", "Dridi", "Sassi", "Nasri", "Bouzid", "Karoui", "Zouari", "Belhaj",
            "Cherif", "Mabrouk", "Haddad", "Slimani", "Rezgui", "Ferchichi", "Ouertani", "Tlili", "Baccouche",
            "Maaloul", "Souissi", "Jlassi"};
    private static final String[] STREETS = {"Avenue Habib Bourguiba", "Rue de Marseille", "Avenue de la Liberté",
            "Rue Ibn Khaldoun", "Avenue Mohamed V", "Rue du Lac Léman", "Rue de Palestine", "Avenue Hédi Nouira"};
    private static final String[] CITIES = {"Tunis", "Ariana", "La Marsa", "Ben Arous", "Manouba", "Bizerte", "Nabeul"};

    private record ProfDef(String first, String last, String email, String dept, String specialty, String type) {
    }

    private record SubjDef(String code, String name, double coef, int credits, int prof) {
    }

    private record ClassDef(String name, String level, String specialty, int yearsAgo, int[] subjects) {
    }

    private static final List<ProfDef> PROFS = List.of(
            new ProfDef("Karim", "Mansouri", "prof" + DOMAIN, "Informatique", "Algorithmique et programmation", "PERMANENT"),
            new ProfDef("Salma", "Trabelsi", "salma.trabelsi" + DOMAIN, "Informatique", "Bases de données", "PERMANENT"),
            new ProfDef("Mehdi", "Gharbi", "mehdi.gharbi" + DOMAIN, "Informatique", "Réseaux et systèmes", "PERMANENT"),
            new ProfDef("Ines", "Bouazizi", "ines.bouazizi" + DOMAIN, "Informatique", "Génie logiciel", "PERMANENT"),
            new ProfDef("Walid", "Jebali", "walid.jebali" + DOMAIN, "Mathématiques", "Probabilités et statistiques", "PERMANENT"),
            new ProfDef("Rania", "Hamdi", "rania.hamdi" + DOMAIN, "Langues", "Anglais technique", "VACATAIRE"),
            new ProfDef("Amine", "Khelifi", "amine.khelifi" + DOMAIN, "Informatique", "Intelligence artificielle", "VACATAIRE"),
            new ProfDef("Meriem", "Chaabane", "meriem.chaabane" + DOMAIN, "Informatique", "Cloud et sécurité", "PERMANENT"));

    private static final List<SubjDef> SUBJECTS = List.of(
            new SubjDef("DM-INF101", "Algorithmique et structures de données", 3.0, 6, 0),   // 0
            new SubjDef("DM-INF102", "Programmation orientée objet (Java)", 2.5, 5, 0),     // 1
            new SubjDef("DM-INF201", "Bases de données relationnelles", 3.0, 5, 1),         // 2
            new SubjDef("DM-INF202", "Réseaux informatiques", 2.0, 4, 2),                   // 3
            new SubjDef("DM-INF203", "Systèmes d'exploitation", 2.0, 4, 2),                 // 4
            new SubjDef("DM-INF204", "Développement web", 2.5, 5, 3),                       // 5
            new SubjDef("DM-INF301", "Génie logiciel et UML", 3.0, 5, 3),                   // 6
            new SubjDef("DM-MAT101", "Mathématiques discrètes", 2.0, 4, 4),                 // 7
            new SubjDef("DM-MAT201", "Probabilités et statistiques", 2.0, 4, 4),            // 8
            new SubjDef("DM-LAN101", "Anglais technique", 1.0, 2, 5),                       // 9
            new SubjDef("DM-INF401", "Machine Learning", 3.0, 6, 6),                        // 10
            new SubjDef("DM-INF402", "Cloud computing et DevOps", 2.5, 5, 7),               // 11
            new SubjDef("DM-INF302", "Sécurité des systèmes d'information", 2.0, 4, 7),     // 12
            new SubjDef("DM-LAN201", "Communication professionnelle", 1.0, 2, 5));          // 13

    private static final List<ClassDef> CLASSES = List.of(
            new ClassDef("L1 Informatique", "L1", "Informatique", 0, new int[]{0, 1, 4, 7, 9, 2}),
            new ClassDef("L2 Informatique", "L2", "Informatique", 1, new int[]{2, 3, 5, 8, 9, 6}),
            new ClassDef("L3 Génie Logiciel", "L3", "Génie Logiciel", 2, new int[]{6, 5, 12, 13, 3, 2}),
            new ClassDef("M1 Data Science et IA", "M1", "Data Science", 3, new int[]{10, 11, 8, 12, 9, 2}));

    private static final LocalTime[][] SLOTS = {
            {LocalTime.of(8, 30), LocalTime.of(10, 0)},
            {LocalTime.of(10, 15), LocalTime.of(11, 45)},
            {LocalTime.of(13, 30), LocalTime.of(15, 0)},
            {LocalTime.of(15, 15), LocalTime.of(16, 45)}};
    private static final DayOfWeek[] DAYS = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
    private static final List<String> LECTURE_ROOMS = List.of("Amphi 1", "A101", "A102", "A103", "B201", "B202");
    private static final List<String> LAB_ROOMS = List.of("Lab-Info 1", "Lab-Info 2");

    private final UniversityRepository universities;
    private final RoleRepository roles;
    private final StudentRepository students;
    private final ProfessorRepository professors;
    private final ProfessorKindRepository professorKinds;
    private final AdministrationAgentRepository agents;
    private final SubjectRepository subjects;
    private final SchoolClassRepository classes;
    private final SectionRepository sections;
    private final EnrollmentRepository enrollments;
    private final ClassTeachingRepository teachings;
    private final CourseRepository courses;
    private final TimetableRepository timetables;
    private final GradeRepository grades;
    private final AttendanceRepository attendances;
    private final AbsencePolicyRepository absencePolicies;
    private final AnnouncementRepository announcements;
    private final NotificationRepository notifications;
    private final ExamRepository exams;
    private final InternshipRepository internships;
    private final DocumentTypeRepository documentTypes;
    private final DocumentRequestRepository documentRequests;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.demo.password:Demo@2026}")
    private String demoPassword;

    @Value("${app.demo.reset:false}")
    private boolean reset;

    @Value("${app.demo.students-per-class:15}")
    private int studentsPerClass;

    // ------------------------------------------------------------------------------------------------
    // entry point
    // ------------------------------------------------------------------------------------------------

    /** Runs once everything else has started, so roles and permissions already exist. */
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        TenantContext.clear();
        University existing = universities.findAll().stream()
                .filter(u -> CODE.equals(u.getCode())).findFirst().orElse(null);
        if (existing != null && !reset) {
            log.info("DEMO data: the {} university already exists, nothing to do (set app.demo.reset=true to rebuild it)", CODE);
            return;
        }
        if (existing != null) {
            log.warn("DEMO data: reset requested, wiping the {} university", CODE);
            if (!wipe(existing.getId())) {
                return;
            }
        }

        University u = new University();
        u.setName("Université Démo");
        u.setCode(CODE);
        u.setCity("Tunis");
        final Long uid = universities.save(u).getId();
        try {
            TenantContext.run(uid, () -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> populate()));
        } catch (RuntimeException e) {
            log.error("DEMO data: seeding failed, the partial DEMO university is removed", e);
            TenantContext.clear();
            try {
                universities.deleteById(uid);
            } catch (RuntimeException ignored) {
                // nothing else to do
            }
        } finally {
            TenantContext.clear();
        }
    }

    // ------------------------------------------------------------------------------------------------
    // data
    // ------------------------------------------------------------------------------------------------

    private void populate() {
        Random rnd = new Random(2026);
        String hash = passwordEncoder.encode(demoPassword);

        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        String academicYear = startYear + "-" + (startYear + 1);
        boolean secondSemester = today.getMonthValue() >= 2 && today.getMonthValue() <= 7;
        String semester = secondSemester ? "S2" : "S1";
        LocalDate semesterStart = secondSemester ? LocalDate.of(startYear + 1, 2, 1) : LocalDate.of(startYear, 9, 15);
        if (semesterStart.isAfter(today.minusDays(14)) || semesterStart.isBefore(today.minusDays(200))) {
            semesterStart = today.minusDays(60);
        }

        Role studentRole = role("ROLE_STUDENT", "University student");
        Role professorRole = role("ROLE_PROFESSOR", "University professor");
        Role agentRole = role("ROLE_ADMIN_AGENT", "University administration agent");

        // ---- administration agents
        List<AdministrationAgent> agentList = new ArrayList<>();
        agentList.add(agent("Leila", "Ben Salah", "admin" + DOMAIN, "DEMO-A01", "Scolarité", agentRole, hash, rnd));
        agentList.add(agent("Hatem", "Zaoui", "hatem.zaoui" + DOMAIN, "DEMO-A02", "Examens et stages", agentRole, hash, rnd));
        agents.saveAll(agentList);

        // ---- professors
        List<Professor> profList = new ArrayList<>();
        int seq = 1;
        for (ProfDef d : PROFS) {
            Professor p = new Professor();
            fillPerson(p, d.first(), d.last(), d.email(), professorRole, hash, rnd);
            p.setEmployeeNumber(String.format("DEMO-P%02d", seq++));
            p.setDepartment(d.dept());
            p.setSpecialty(d.specialty());
            p.setProfessorType(d.type());
            profile(p, LocalDate.of(1968 + rnd.nextInt(22), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)), rnd);
            profList.add(p);
        }
        professors.saveAll(profList);
        for (int i = 0; i < profList.size(); i++) {
            ProfessorKind k = new ProfessorKind();
            k.setProfessorId(profList.get(i).getId());
            k.setKind(PROFS.get(i).type());
            professorKinds.save(k);
        }

        // ---- subjects
        List<Subject> subjList = new ArrayList<>();
        for (SubjDef d : SUBJECTS) {
            Subject s = new Subject();
            s.setName(d.name());
            s.setCode(d.code());
            s.setCoefficient(d.coef());
            s.setCredits(d.credits());
            s.setProfessors(new HashSet<>(List.of(profList.get(d.prof()))));
            subjList.add(s);
        }
        subjects.saveAll(subjList);

        // ---- classes, sections, teachings
        List<SchoolClass> classList = new ArrayList<>();
        List<List<Section>> sectionsByClass = new ArrayList<>();
        for (ClassDef d : CLASSES) {
            SchoolClass c = new SchoolClass();
            c.setName(d.name());
            c.setLevel(d.level());
            c.setAcademicYear(academicYear);
            Set<Subject> set = new HashSet<>();
            for (int idx : d.subjects()) {
                set.add(subjList.get(idx));
            }
            c.setSubjects(set);
            classes.save(c);
            classList.add(c);

            List<Section> secs = new ArrayList<>();
            for (int g = 1; g <= 2; g++) {
                Section sec = new Section();
                sec.setName("Groupe " + g);
                sec.setSchoolClass(c);
                secs.add(sections.save(sec));
            }
            sectionsByClass.add(secs);

            for (int idx : d.subjects()) {
                ClassTeaching t = new ClassTeaching();
                t.setClassId(c.getId());
                t.setSubjectId(subjList.get(idx).getId());
                t.setProfessorId(profList.get(SUBJECTS.get(idx).prof()).getId());
                teachings.save(t);
            }
        }

        // ---- students and enrollments
        List<List<Student>> studentsByClass = new ArrayList<>();
        int studentSeq = 1;
        long cin = 99000001L;
        Set<String> usedEmails = new HashSet<>();
        for (int ci = 0; ci < CLASSES.size(); ci++) {
            ClassDef d = CLASSES.get(ci);
            List<Student> list = new ArrayList<>();
            for (int i = 0; i < studentsPerClass; i++) {
                boolean female = rnd.nextBoolean();
                String first = female ? FEMALE[rnd.nextInt(FEMALE.length)] : MALE[rnd.nextInt(MALE.length)];
                String last = LAST[rnd.nextInt(LAST.length)];
                String email;
                if (ci == 1 && i == 0) {          // the main demo student
                    first = "Yasmine";
                    last = "Ben Ali";
                    email = "student" + DOMAIN;
                } else {
                    do {
                        email = norm(first) + "." + norm(last) + (10 + rnd.nextInt(90)) + DOMAIN;
                    } while (!usedEmails.add(email));
                }
                usedEmails.add(email);
                Student s = new Student();
                fillPerson(s, first, last, email, studentRole, hash, rnd);
                s.setCin(String.valueOf(cin++));
                s.setStudentNumber(String.format("DEMO%02d-%04d", startYear % 100, studentSeq++));
                s.setEnrollmentYear(startYear - d.yearsAgo());
                s.setLevel(d.level());
                s.setSpecialty(d.specialty());
                profile(s, LocalDate.of(startYear - 19 - d.yearsAgo() - rnd.nextInt(2), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)), rnd);
                list.add(s);
            }
            students.saveAll(list);
            studentsByClass.add(list);

            for (int i = 0; i < list.size(); i++) {
                Enrollment e = new Enrollment();
                e.setStudent(list.get(i));
                e.setSchoolClass(classList.get(ci));
                e.setSection(sectionsByClass.get(ci).get(i % 2));
                e.setEnrollmentDate(LocalDate.of(startYear, 9, 10));
                e.setAcademicYear(academicYear);
                e.setStatus(EnrollmentStatus.ACTIVE);
                enrollments.save(e);
            }
        }

        // ---- courses
        for (int si = 0; si < subjList.size(); si++) {
            Professor p = profList.get(SUBJECTS.get(si).prof());
            String name = SUBJECTS.get(si).name();
            Course c1 = new Course();
            c1.setTitle("Support de cours — " + name);
            c1.setDescription("Polycopié du cours « " + name + " » : objectifs, plan du semestre et bibliographie.");
            c1.setSubject(subjList.get(si));
            c1.setProfessor(p);
            Course c2 = new Course();
            c2.setTitle("Série de TD — " + name);
            c2.setDescription("Exercices dirigés et corrigés pour « " + name + " ».");
            c2.setSubject(subjList.get(si));
            c2.setProfessor(p);
            courses.saveAll(List.of(c1, c2));
        }

        // ---- timetable (no clash for a class, a professor or a room)
        Timetable timetable = new Timetable();
        timetable.setAcademicYear(academicYear);
        timetable.setSemester(semester);
        List<TimetableSession> sessions = buildSessions(rnd, timetable, classList, subjList);
        timetable.getSessions().addAll(sessions);
        timetables.save(timetable);

        // ---- absence policy (default for every subject)
        AbsencePolicy policy = new AbsencePolicy();
        policy.setSubjectId(null);
        policy.setMaxAbsences(5);
        policy.setLatesPerAbsence(3);
        absencePolicies.save(policy);

        // ---- grades
        List<Grade> gradeList = new ArrayList<>();
        long span = Math.max(1, today.toEpochDay() - semesterStart.toEpochDay() - 8);
        EvaluationType[] types = {EvaluationType.QUIZ, EvaluationType.ASSIGNMENT, EvaluationType.EXAM};
        for (int ci = 0; ci < CLASSES.size(); ci++) {
            for (Student s : studentsByClass.get(ci)) {
                double ability = 11.5 + rnd.nextGaussian() * 2.2;
                for (int idx : CLASSES.get(ci).subjects()) {
                    for (EvaluationType type : types) {
                        Grade g = new Grade();
                        g.setStudent(s);
                        g.setSubject(subjList.get(idx));
                        g.setEvaluationType(type);
                        g.setValue(clampGrade(ability + rnd.nextGaussian() * 2.5));
                        g.setDate(semesterStart.plusDays(7 + (long) (rnd.nextDouble() * span)));
                        gradeList.add(g);
                    }
                }
            }
        }
        grades.saveAll(gradeList);

        // ---- attendance for the last three weeks (two "problem" students per class show the elimination screens)
        List<Attendance> attList = new ArrayList<>();
        LocalDate from = semesterStart.isAfter(today.minusDays(21)) ? semesterStart : today.minusDays(21);
        for (LocalDate d = from; d.isBefore(today); d = d.plusDays(1)) {
            for (TimetableSession ts : sessions) {
                if (ts.getDayOfWeek() != d.getDayOfWeek()) {
                    continue;
                }
                int ci = classList.indexOf(ts.getSchoolClass());
                List<Student> list = studentsByClass.get(ci);
                for (int i = 0; i < list.size(); i++) {
                    boolean problem = i == 1 || i == 2;
                    double r = rnd.nextDouble();
                    double pAbsent = problem ? 0.30 : 0.05;
                    double pLate = problem ? 0.10 : 0.06;
                    AttendanceStatus status = r < pAbsent ? AttendanceStatus.ABSENT
                            : r < pAbsent + pLate ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                    Attendance a = new Attendance();
                    a.setDate(d);
                    a.setStatus(status);
                    if (status == AttendanceStatus.ABSENT && rnd.nextInt(5) == 0) {
                        a.setJustification("Certificat médical fourni");
                    }
                    a.setStudent(list.get(i));
                    a.setSubject(ts.getSubject());
                    a.setMarkedBy(ts.getProfessor());
                    attList.add(a);
                }
            }
        }
        attendances.saveAll(attList);

        // ---- announcements
        AdministrationAgent author = agentList.get(0);
        announcements.save(announcement("Bienvenue sur EduNex",
                "Chers étudiants, chers enseignants, bienvenue sur la plateforme de l'université. Retrouvez ici vos emplois du temps, notes, cours et demandes administratives.",
                TargetAudience.ALL, null, author));
        announcements.save(announcement("Stages de fin d'études : dépôt des sujets",
                "Les étudiants de dernière année sont invités à déposer leur sujet de stage auprès de l'administration avant la fin du mois.",
                TargetAudience.STUDENTS, null, author));
        announcements.save(announcement("Réunion pédagogique du département Informatique",
                "Une réunion pédagogique est programmée cette semaine en salle B202. Ordre du jour : examens du semestre et affectation des surveillances.",
                TargetAudience.PROFESSORS, null, author));
        announcements.save(announcement("Report du devoir surveillé de Bases de données",
                "Le devoir surveillé de Bases de données relationnelles est reporté d'une semaine. La nouvelle date sera communiquée sur l'emploi du temps.",
                TargetAudience.CLASS, classList.get(1), author));
        announcements.save(announcement("Fermeture exceptionnelle de la bibliothèque",
                "La bibliothèque universitaire sera fermée exceptionnellement samedi pour travaux de maintenance.",
                TargetAudience.ALL, null, author));
        announcements.save(announcement("Procédure des demandes de documents",
                "Rappel : toutes les demandes d'attestations doivent désormais être traitées depuis le module Demandes de documents.",
                TargetAudience.ADMIN_AGENTS, null, author));

        // ---- exams (weekdays in the next weeks)
        String[] examTypes = {"Examen", "Devoir surveillé", "Examen", "Devoir surveillé"};
        for (int ci = 0; ci < CLASSES.size(); ci++) {
            for (int k = 0; k < 2; k++) {
                int subjIdx = CLASSES.get(ci).subjects()[k * 2];
                LocalDate date = today.plusDays(10 + ci * 3L + k * 7L);
                while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    date = date.plusDays(1);
                }
                Exam e = new Exam();
                e.setSubject(subjList.get(subjIdx));
                e.setClasses(new HashSet<>(List.of(classList.get(ci))));
                e.setExamType(examTypes[(ci + k) % examTypes.length]);
                e.setExamDate(date);
                e.setStartTime(k == 0 ? LocalTime.of(9, 0) : LocalTime.of(14, 0));
                e.setEndTime(k == 0 ? LocalTime.of(11, 0) : LocalTime.of(16, 0));
                e.setRoom(k == 0 ? "Amphi 1" : "B201");
                e.setSupervisorsNeeded(2);
                exams.save(e);
            }
        }

        // ---- internships for final-year students
        String[][] stages = {
                {"TechNova Tunis", "Sofien Larbi", "Conception d'une application mobile de suivi de livraison", "PFE"},
                {"DataBridge Solutions", "Nadia Kallel", "Mise en place d'un entrepôt de données et tableaux de bord décisionnels", "PFE"},
                {"CloudSpring SARL", "Anouar Missaoui", "Automatisation du déploiement continu avec Docker et Kubernetes", "PFE"},
                {"Medina Digital", "Houda Ben Amor", "Refonte d'une plateforme e-commerce en microservices", "Stage d'été"},
                {"AtlasSoft", "Fares Gasmi", "Détection d'anomalies sur des séries temporelles industrielles", "PFE"},
                {"Carthage Analytics", "Ons Ferjani", "Système de recommandation pour une application de formation en ligne", "PFE"}};
        int n = 0;
        for (int ci = 2; ci < 4; ci++) {
            for (int i = 0; i < 3; i++) {
                String[] st = stages[n];
                Internship in = new Internship();
                in.setStudent(studentsByClass.get(ci).get(i + 3));
                in.setSupervisor(profList.get((n % 3) + 1));
                in.setCompany(st[0]);
                in.setCompanySupervisor(st[1]);
                in.setTitle(st[2]);
                in.setKind(st[3]);
                in.setStartDate(today.minusDays(20));
                in.setEndDate(today.plusDays(130));
                internships.save(in);
                n++;
            }
        }

        // ---- document types and requests
        List<DocumentType> types2 = new ArrayList<>();
        for (String[] t : new String[][]{
                {"Attestation de scolarité", "STUDENT", "0"}, {"Relevé de notes", "STUDENT", "0"},
                {"Attestation de réussite", "STUDENT", "0"}, {"Certificat d'assiduité", "STUDENT", "0"},
                {"Convention de stage", "STUDENT", "0"}, {"Duplicata de la carte étudiant", "STUDENT", "0"},
                {"Attestation de travail", "PROFESSOR", "0"}, {"Attestation de salaire", "PROFESSOR", "0"},
                {"Ordre de mission", "PROFESSOR", "0"}, {"Autre document", "BOTH", "1"}}) {
            DocumentType dt = new DocumentType();
            // the name is unique across ALL universities in the current schema: avoid a clash with the main one
            dt.setName(nameTaken(t[0]) ? t[0] + " (Démo)" : t[0]);
            dt.setAudience(t[1]);
            dt.setRequiresDetails("1".equals(t[2]));
            types2.add(documentTypes.save(dt));
        }
        List<Student> l2 = studentsByClass.get(1);
        Object[][] requests = {
                {l2.get(0), 0, "Dossier de bourse", RequestStatus.READY_FOR_PICKUP, "Document prêt, à retirer au bureau de la scolarité."},
                {l2.get(0), 1, "Candidature à un stage", RequestStatus.IN_PROGRESS, "En cours de signature."},
                {l2.get(3), 0, "Demande de logement universitaire", RequestStatus.FILED, null},
                {l2.get(4), 3, "Justification d'absences", RequestStatus.IN_PROGRESS, "Dossier en cours de vérification."},
                {studentsByClass.get(2).get(0), 4, "Stage de fin d'études", RequestStatus.FILED, null},
                {studentsByClass.get(0).get(5), 5, "Carte étudiant perdue", RequestStatus.READY_FOR_PICKUP, "Nouvelle carte disponible."}};
        for (Object[] r : requests) {
            DocumentRequest dr = new DocumentRequest();
            dr.setRequester((Student) r[0]);
            dr.setDocType(types2.get((Integer) r[1]).getName());
            dr.setCopies(1);
            dr.setReason((String) r[2]);
            dr.setStatus((RequestStatus) r[3]);
            dr.setAdminNote((String) r[4]);
            if (r[3] != RequestStatus.FILED) {
                dr.setHandledBy("Leila Ben Salah");
            }
            documentRequests.save(dr);
        }

        // ---- notifications for the three demo accounts
        Student demoStudent = studentsByClass.get(1).get(0);
        notifications.saveAll(List.of(
                notification(demoStudent, NotificationType.NEW_GRADE, "Une nouvelle note a été publiée en Bases de données relationnelles.", false),
                notification(demoStudent, NotificationType.ATTENDANCE_UPDATE, "Votre présence a été mise à jour pour le cours de Réseaux informatiques.", false),
                notification(demoStudent, NotificationType.ANNOUNCEMENT, "Nouvelle annonce : Report du devoir surveillé de Bases de données.", false),
                notification(demoStudent, NotificationType.TIMETABLE_CHANGE, "Votre emploi du temps a été modifié pour cette semaine.", true),
                notification(demoStudent, NotificationType.DOCUMENT_REQUEST, "Votre attestation de scolarité est prête à être retirée.", true),
                notification(profList.get(0), NotificationType.ANNOUNCEMENT, "Nouvelle annonce : Réunion pédagogique du département Informatique.", false),
                notification(profList.get(0), NotificationType.TIMETABLE_CHANGE, "Une séance a été ajoutée à votre emploi du temps.", true),
                notification(agentList.get(0), NotificationType.DOCUMENT_REQUEST, "Nouvelle demande de document déposée par un étudiant.", false)));

        log.info("""
                DEMO data ready ({} students, {} professors, {} classes, {} sessions, {} grades, {} attendance rows).
                  administration : admin{}  /  {}
                  professor      : prof{}  /  {}
                  student        : student{}  /  {}""",
                studentsPerClass * CLASSES.size(), profList.size(), classList.size(), sessions.size(), gradeList.size(),
                attList.size(), DOMAIN, demoPassword, DOMAIN, demoPassword, DOMAIN, demoPassword);
    }

    // ------------------------------------------------------------------------------------------------
    // timetable builder
    // ------------------------------------------------------------------------------------------------

    private List<TimetableSession> buildSessions(Random rnd, Timetable timetable, List<SchoolClass> classList,
                                                  List<Subject> subjList) {
        Set<String> classBusy = new HashSet<>();
        Set<String> profBusy = new HashSet<>();
        Set<String> roomBusy = new HashSet<>();
        Set<String> classSubjectDay = new HashSet<>();
        List<TimetableSession> out = new ArrayList<>();
        for (int ci = 0; ci < CLASSES.size(); ci++) {
            for (int idx : CLASSES.get(ci).subjects()) {
                SubjDef def = SUBJECTS.get(idx);
                for (int k = 0; k < 2; k++) {                    // one lecture + one tutorial per week
                    List<int[]> candidates = new ArrayList<>();
                    for (int d = 0; d < DAYS.length; d++) {
                        for (int s = 0; s < SLOTS.length; s++) {
                            candidates.add(new int[]{d, s});
                        }
                    }
                    Collections.shuffle(candidates, rnd);
                    for (int[] c : candidates) {
                        String slot = c[0] + "-" + c[1];
                        String cb = ci + "|" + slot;
                        String pb = def.prof() + "|" + slot;
                        String csd = ci + "|" + idx + "|" + c[0];
                        if (classBusy.contains(cb) || profBusy.contains(pb) || classSubjectDay.contains(csd)) {
                            continue;
                        }
                        List<String> pool = new ArrayList<>(k == 1 && def.code().contains("INF") ? LAB_ROOMS : LECTURE_ROOMS);
                        Collections.shuffle(pool, rnd);
                        String room = pool.stream().filter(r -> !roomBusy.contains(r + "|" + slot)).findFirst().orElse(null);
                        if (room == null) {
                            continue;
                        }
                        classBusy.add(cb);
                        profBusy.add(pb);
                        roomBusy.add(room + "|" + slot);
                        classSubjectDay.add(csd);
                        TimetableSession ts = new TimetableSession();
                        ts.setTimetable(timetable);
                        ts.setSchoolClass(classList.get(ci));
                        ts.setSubject(subjList.get(idx));
                        ts.setProfessor(subjList.get(idx).getProfessors().iterator().next());
                        ts.setDayOfWeek(DAYS[c[0]]);
                        ts.setStartTime(SLOTS[c[1]][0]);
                        ts.setEndTime(SLOTS[c[1]][1]);
                        ts.setRoom(room);
                        out.add(ts);
                        break;
                    }
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------
    // reset
    // ------------------------------------------------------------------------------------------------

    private static final String U = "(select id from users where university_id = :uid)";
    private static final String CLS = "(select id from classes where university_id = :uid)";
    private static final String SUBJ = "(select id from subjects where university_id = :uid)";

    private static final List<String> WIPE = List.of(
            "delete from chat_attachments where chat_session_id in (select id from chat_sessions where university_id = :uid)",
            "delete from chat_messages where chat_session_id in (select id from chat_sessions where university_id = :uid)",
            "delete from chat_sessions where university_id = :uid",
            "delete from conversation_messages where conversation_id in (select id from conversations where university_id = :uid)",
            "delete from conversation_members where conversation_id in (select id from conversations where university_id = :uid)",
            "delete from conversations where university_id = :uid",
            "delete from defense_jurors where defense_id in (select id from defenses where university_id = :uid)",
            "delete from defenses where university_id = :uid",
            "delete from internships where university_id = :uid",
            "delete from exam_supervision_events where exam_id in (select id from exams where university_id = :uid)",
            "delete from exam_supervisions where exam_id in (select id from exams where university_id = :uid)",
            "delete from exam_classes where exam_id in (select id from exams where university_id = :uid)",
            "delete from exams where university_id = :uid",
            "delete from session_absences where university_id = :uid",
            "delete from sessions where university_id = :uid",
            "delete from timetables where university_id = :uid",
            "delete from attendances where university_id = :uid",
            "delete from grades where university_id = :uid",
            "delete from eliminations where university_id = :uid",
            "delete from absence_policies where university_id = :uid",
            "delete from quiz_attempts where student_id in " + U,
            "delete from course_completions where student_id in " + U,
            "delete from course_documents where university_id = :uid",
            "delete from courses where university_id = :uid",
            "delete from enrollments where university_id = :uid",
            "delete from sections where university_id = :uid",
            "delete from class_teachings where university_id = :uid",
            "delete from class_subjects where class_id in " + CLS,
            "delete from subject_professors where subject_id in " + SUBJ,
            "delete from announcement_attachments where announcement_id in (select id from announcements where university_id = :uid)",
            "delete from announcements where university_id = :uid",
            "delete from notifications where university_id = :uid",
            "delete from document_requests where university_id = :uid",
            "delete from document_types where university_id = :uid",
            "delete from email_preferences where user_id in " + U,
            "delete from account_tokens where user_id in " + U,
            "delete from user_permissions where user_id in " + U,
            "delete from activity_logs where user_id in " + U,
            "delete from profiles where user_id in " + U,
            "delete from professor_kinds where professor_id in " + U,
            "delete from students where id in " + U,
            "delete from professors where id in " + U,
            "delete from administration_agents where id in " + U,
            "delete from users where university_id = :uid",
            "delete from classes where university_id = :uid",
            "delete from subjects where university_id = :uid");

    /** Deletes every row of the DEMO university. Statements that fail are retried: foreign keys decide the order. */
    private boolean wipe(Long uid) {
        TenantContext.clear();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<String> pending = new ArrayList<>(WIPE);
        for (int pass = 0; pass < 5 && !pending.isEmpty(); pass++) {
            Iterator<String> it = pending.iterator();
            while (it.hasNext()) {
                String sql = it.next();
                try {
                    tx.executeWithoutResult(s -> em.createNativeQuery(sql).setParameter("uid", uid).executeUpdate());
                    it.remove();
                } catch (RuntimeException e) {
                    // retried on the next pass
                }
            }
        }
        try {
            universities.deleteById(uid);
        } catch (RuntimeException e) {
            log.error("DEMO data: the reset could not remove everything. Statements still failing: {}. "
                    + "Delete the DEMO university rows by hand or restore a database snapshot.", pending);
            return false;
        }
        if (!pending.isEmpty()) {
            log.warn("DEMO data: these cleanup statements never matched anything (harmless if the table does not exist): {}", pending);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------------------------------------

    private Role role(String name, String description) {
        return roles.findByName(name).orElseGet(() -> {
            Role r = new Role();
            r.setName(name);
            r.setDescription(description);
            return roles.save(r);
        });
    }

    private AdministrationAgent agent(String first, String last, String email, String employeeNumber, String service,
                                      Role role, String hash, Random rnd) {
        AdministrationAgent a = new AdministrationAgent();
        fillPerson(a, first, last, email, role, hash, rnd);
        a.setEmployeeNumber(employeeNumber);
        a.setService(service);
        profile(a, LocalDate.of(1975 + rnd.nextInt(15), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)), rnd);
        return a;
    }

    private long nextCin = 98000001L;

    private void fillPerson(User u, String first, String last, String email, Role role, String hash, Random rnd) {
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPassword(hash);
        u.setMustChangePassword(false);
        u.setStatus(UserStatus.ACTIVE);
        u.setRole(role);
        u.setPhone("+216 " + new int[]{2, 5, 9}[rnd.nextInt(3)] + String.format("%07d", rnd.nextInt(10_000_000)));
        u.setCin(String.valueOf(nextCin++));
    }

    private void profile(User u, LocalDate birth, Random rnd) {
        com.edunex.backend.entity.Profile p = new com.edunex.backend.entity.Profile();
        p.setUser(u);
        p.setDateOfBirth(birth);
        p.setAddress((1 + rnd.nextInt(120)) + " " + STREETS[rnd.nextInt(STREETS.length)] + ", "
                + CITIES[rnd.nextInt(CITIES.length)]);
        u.setProfile(p);
    }

    private Announcement announcement(String title, String content, TargetAudience audience, SchoolClass target,
                                      AdministrationAgent author) {
        Announcement a = new Announcement();
        a.setTitle(title);
        a.setContent(content);
        a.setTargetAudience(audience);
        a.setTargetClass(target);
        a.setPublishedBy(author);
        return a;
    }

    private Notification notification(User user, NotificationType type, String message, boolean read) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setMessage(message);
        n.setRead(read);
        return n;
    }

    private boolean nameTaken(String name) {
        Number count = (Number) em.createNativeQuery("select count(*) from document_types where lower(name) = lower(:n)")
                .setParameter("n", name).getSingleResult();
        return count.longValue() > 0;
    }

    private static double clampGrade(double v) {
        double c = Math.max(2.0, Math.min(19.5, v));
        return Math.round(c * 4.0) / 4.0;
    }

    private static String norm(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }
}
