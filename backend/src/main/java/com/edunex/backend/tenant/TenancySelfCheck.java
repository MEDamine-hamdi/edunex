package com.edunex.backend.tenant;

import com.edunex.backend.entity.University;
import com.edunex.backend.repository.SchoolClassRepository;
import com.edunex.backend.repository.UniversityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;

/** Runs only when APP_TENANCY_SELFCHECK=true (the safety step): proves that universities are really kept apart. */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class TenancySelfCheck implements CommandLineRunner {

    private final UniversityRepository universities;
    private final PlatformTransactionManager transactionManager;
    private final SchoolClassRepository classRepository;

    @Value("${app.tenancy.selfcheck:false}")
    private boolean enabled;

    @PersistenceContext
    private EntityManager em;

    @Override
    public void run(String... args) {
        if (!enabled) {
            return;
        }
        TenantContext.clear();
        University main = universities.findAll().stream().filter(u -> !"SELFCHECK".equals(u.getCode()))
                .min(Comparator.comparing(University::getId)).orElseThrow();
        universities.findAll().stream().filter(u -> "SELFCHECK".equals(u.getCode())).forEach(universities::delete);
        University temp = new University();
        temp.setName("Self check (temporary)");
        temp.setCode("SELFCHECK");
        final Long tempId = universities.save(temp).getId();
        final Long mainId = main.getId();
        try {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            long platformAdmins = count("PlatformAdministrator", null, tx);
            for (Class<? extends TenantOwned> type : TenantClasses.ALL) {
                String name = type.getSimpleName();
                long all = count(name, null, tx);
                long mine = count(name, mainId, tx);
                long other = count(name, tempId, tx);
                long expected = name.equals("User") ? all - platformAdmins : all;
                if (other != 0) {
                    fail(name + ": an empty university can see " + other + " row(s)");
                }
                if (mine != expected) {
                    fail(name + ": the first university sees " + mine + " of " + expected + " row(s)");
                }
            }
            for (String sub : List.of("Student", "Professor", "AdministrationAgent")) {
                long other = count(sub, tempId, tx);
                if (other != 0) {
                    fail(sub + ": an empty university can see " + other + " row(s)");
                }
            }
            Long someClass = classRepository.findAll().stream().map(c -> c.getId()).findFirst().orElse(null);
            if (someClass != null) {
                boolean visible = TenantContext.call(tempId, () -> {
                    try {
                        return classRepository.findById(someClass).isPresent();
                    } catch (RuntimeException e) {
                        return false;
                    }
                });
                if (visible) {
                    fail("a class of another university can be opened by its number");
                }
            }
            log.info("TENANCY SELF-CHECK OK");
        } finally {
            TenantContext.clear();
            universities.deleteById(tempId);
        }
    }

    private long count(String entity, Long universityId, TransactionTemplate tx) {
        return TenantContext.call(universityId, () -> {
            Long n = tx.execute(s -> em.createQuery("select count(e) from " + entity + " e", Long.class).getSingleResult());
            return n == null ? 0L : n;
        });
    }

    private static void fail(String message) {
        throw new IllegalStateException("TENANCY SELF-CHECK FAILED: " + message);
    }
}