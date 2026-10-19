package com.edunex.backend.config;

import java.util.List;

/** Permissions enforced by the backend, with the roles that receive them the first time they are created. */
public final class PermissionCatalog {

    public record Entry(String code, String description, List<String> roles) {
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry("ACCOUNTS_MANAGE", "Créer et gérer les comptes des professeurs et des étudiants", List.of("ROLE_ADMIN_AGENT")),
            new Entry("STRUCTURE_MANAGE", "Gérer les classes, sections, matières et inscriptions", List.of("ROLE_ADMIN_AGENT")),
            new Entry("TIMETABLE_MANAGE", "Gérer les emplois du temps", List.of("ROLE_ADMIN_AGENT")),
            new Entry("GRADES_MANAGE", "Saisir et modifier les notes", List.of("ROLE_ADMIN_AGENT")),
            new Entry("ANNOUNCEMENTS_PUBLISH", "Publier des annonces", List.of("ROLE_ADMIN_AGENT")),
            new Entry("STATS_VIEW_UNIVERSITY", "Consulter les statistiques de l'université", List.of("ROLE_ADMIN_AGENT")),
            new Entry("KNOWLEDGE_MANAGE", "Gérer le guide de l'institut utilisé par l'assistant IA", List.of("ROLE_ADMIN_AGENT")),
            new Entry("ATTENDANCE_MARK", "Faire l'appel et consulter les présences de ses matières", List.of("ROLE_PROFESSOR")),
            new Entry("AI_ASSISTANT_USE", "Utiliser l'assistant IA EduNex", List.of("ROLE_STUDENT"))
    );

    private PermissionCatalog() {
    }
}