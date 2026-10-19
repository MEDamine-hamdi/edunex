package com.edunex.backend.service.ai;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Keyword ranking (BM25) with accent folding, light plural stemming and French/English stop words. */
public final class Bm25Ranker {

    private static final double K1 = 1.4;
    private static final double B = 0.75;

    private static final Set<String> STOP = Set.copyOf(Arrays.asList(
            "le", "la", "les", "de", "des", "du", "un", "une", "et", "ou", "en", "au", "aux", "ce", "ces", "cet",
            "cette", "que", "qui", "quoi", "dans", "pour", "par", "sur", "avec", "sans", "est", "sont", "etre", "il",
            "elle", "ils", "elles", "nous", "vous", "je", "tu", "on", "ne", "pas", "plus", "se", "sa", "son", "ses",
            "leur", "leurs", "mais", "donc", "car", "ni", "comme", "quel", "quelle", "quels", "quelles", "comment",
            "quand", "ete", "ai", "as", "avons", "avez", "ont", "fait", "faire", "peut", "peux", "moi", "mon", "ma",
            "mes", "ton", "ta", "tes", "the", "an", "of", "to", "in", "is", "are", "and", "for", "on", "with", "as",
            "by", "at", "from", "that", "this", "it", "be", "what", "how", "why", "when", "which", "who", "do",
            "does", "can", "you", "me", "my"));

    private Bm25Ranker() {
    }

    public static List<String> tokens(String text) {
        if (text == null) {
            return List.of();
        }
        String folded = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        List<String> out = new ArrayList<>();
        for (String raw : folded.split("[^\\p{L}\\p{N}]+")) {
            if (raw.isEmpty() || (raw.length() < 2 && !Character.isDigit(raw.charAt(0))) || STOP.contains(raw)) {
                continue;
            }
            out.add(stem(raw));
        }
        return out;
    }

    private static String stem(String token) {
        return token.length() > 4 && (token.endsWith("s") || token.endsWith("x"))
                ? token.substring(0, token.length() - 1)
                : token;
    }

    /** One score per text; 0 means no query term was found. */
    public static double[] score(String query, List<String> texts) {
        int n = texts.size();
        double[] scores = new double[n];
        List<String> terms = tokens(query).stream().distinct().toList();
        if (terms.isEmpty() || n == 0) {
            return scores;
        }
        Set<String> wanted = new HashSet<>(terms);
        List<Map<String, Integer>> frequencies = new ArrayList<>(n);
        int[] lengths = new int[n];
        Map<String, Integer> documentFrequency = new HashMap<>();
        double totalLength = 0;
        for (int i = 0; i < n; i++) {
            List<String> tokens = tokens(texts.get(i));
            lengths[i] = tokens.size();
            totalLength += tokens.size();
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                if (wanted.contains(token)) {
                    tf.merge(token, 1, Integer::sum);
                }
            }
            tf.keySet().forEach(term -> documentFrequency.merge(term, 1, Integer::sum));
            frequencies.add(tf);
        }
        double average = Math.max(1.0, totalLength / n);
        for (int i = 0; i < n; i++) {
            for (String term : terms) {
                Integer f = frequencies.get(i).get(term);
                if (f == null) {
                    continue;
                }
                int df = documentFrequency.get(term);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                scores[i] += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * lengths[i] / average));
            }
        }
        return scores;
    }
}