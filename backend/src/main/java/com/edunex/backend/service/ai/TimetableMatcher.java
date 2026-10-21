package com.edunex.backend.service.ai;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Matches the names written in a PDF with the classes, subjects and professors of the platform. */
public final class TimetableMatcher {

    private static final Set<String> TITLES = Set.of("dr", "pr", "prof", "professeur", "mr", "mme", "mlle", "m",
            "monsieur", "madame", "docteur", "mrs", "ms");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})\\s*[:hH.]\\s*(\\d{2})?");

    private TimetableMatcher() {
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static Set<String> tokens(String normalized) {
        Set<String> out = new LinkedHashSet<>();
        for (String t : normalized.split(" ")) {
            if (!t.isEmpty() && (t.length() > 1 || Character.isDigit(t.charAt(0)))) {
                out.add(t);
            }
        }
        return out;
    }

    public static double similarity(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x.isEmpty() || y.isEmpty()) {
            return 0;
        }
        if (x.equals(y)) {
            return 1;
        }
        if (Math.min(x.length(), y.length()) >= 4 && (x.contains(y) || y.contains(x))) {
            return 0.9;
        }
        Set<String> tx = tokens(x);
        Set<String> ty = tokens(y);
        if (tx.isEmpty() || ty.isEmpty()) {
            return 0;
        }
        long inter = tx.stream().filter(ty::contains).count();
        return (double) inter / (tx.size() + ty.size() - inter);
    }

    /** The best item when it is clearly the best (no tie) and scores at least min, otherwise null. */
    public static <T> T best(Collection<T> items, Function<T, List<String>> labels, String query, double min) {
        T best = null;
        double bestScore = 0;
        boolean tie = false;
        for (T item : items) {
            double score = 0;
            for (String label : labels.apply(item)) {
                score = Math.max(score, similarity(label, query));
            }
            if (score > bestScore + 1e-9) {
                best = item;
                bestScore = score;
                tie = false;
            } else if (Math.abs(score - bestScore) <= 1e-9 && score > 0) {
                tie = true;
            }
        }
        return bestScore >= min && !tie ? best : null;
    }

    public static String stripTitles(String professor) {
        return Arrays.stream(normalize(professor).split(" "))
                .filter(t -> !t.isEmpty() && !TITLES.contains(t))
                .collect(Collectors.joining(" "));
    }

    public static DayOfWeek day(String s) {
        String n = normalize(s);
        if (n.length() < 3) {
            return null;
        }
        return switch (n.substring(0, 3)) {
            case "lun", "mon" -> DayOfWeek.MONDAY;
            case "mar", "tue" -> DayOfWeek.TUESDAY;
            case "mer", "wed" -> DayOfWeek.WEDNESDAY;
            case "jeu", "thu" -> DayOfWeek.THURSDAY;
            case "ven", "fri" -> DayOfWeek.FRIDAY;
            case "sam", "sat" -> DayOfWeek.SATURDAY;
            case "dim", "sun" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    public static LocalTime time(String s) {
        if (s == null) {
            return null;
        }
        Matcher m = TIME.matcher(s);
        if (!m.find()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1));
        int minute = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        return hour < 24 && minute < 60 ? LocalTime.of(hour, minute) : null;
    }
}