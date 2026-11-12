package com.edunex.backend.tenant;

import com.edunex.backend.entity.University;
import com.edunex.backend.repository.UniversityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;

/** First start with universities: creates the first university and moves all existing data into it. */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UniversityBootstrap implements CommandLineRunner {

    private final UniversityRepository universities;
    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager em;

    @Override
    public void run(String... args) {
        TenantContext.clear();
        University main = universities.findAll().stream().min(Comparator.comparing(University::getId)).orElseGet(() -> {
            University u = new University();
            u.setName("Université principale");
            u.setCode("MAIN");
            return universities.save(u);
        });
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        for (Class<? extends TenantOwned> type : TenantClasses.ALL) {
            Integer moved = tx.execute(status -> backfill(type, main.getId()));
            if (moved != null && moved > 0) {
                log.info("University: {} {} row(s) assigned to {}", moved, type.getSimpleName(), main.getName());
            }
        }
    }

    private int backfill(Class<? extends TenantOwned> type, Long universityId) {
        List<? extends TenantOwned> rows = em.createQuery("select e from " + type.getSimpleName() + " e where e.universityId is null", type).getResultList();
        int moved = 0;
        for (TenantOwned row : rows) {
            if (row.getClass().getSimpleName().equals("PlatformAdministrator")) {
                continue;
            }
            row.setUniversityId(universityId);
            moved++;
        }
        return moved;
    }
}