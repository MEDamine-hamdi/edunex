package com.edunex.backend.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaFixRunner implements CommandLineRunner {

    private final EntityManager em;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void run(String... args) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> em
                    .createNativeQuery("ALTER TABLE institute_documents ALTER COLUMN file_key DROP NOT NULL")
                    .executeUpdate());
        } catch (Exception e) {
            log.warn("Could not relax institute_documents.file_key: {}", e.toString());
        }
    }
}