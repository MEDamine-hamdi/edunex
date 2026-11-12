package com.edunex.backend.config;

import com.edunex.backend.tenant.TenantContext;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** At the start of every transaction, the university filter is switched on for the current request. */
@Configuration
public class TenantTransactionConfig {

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory) {
            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {
                super.doBegin(transaction, definition);
                EntityManagerHolder holder = (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
                if (holder != null) {
                    holder.getEntityManager().unwrap(Session.class)
                            .enableFilter("tenantFilter").setParameter("tid", TenantContext.currentOrZero());
                }
            }
        };
    }
}