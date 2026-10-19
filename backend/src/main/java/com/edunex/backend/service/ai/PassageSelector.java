package com.edunex.backend.service.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/** Ranks candidate chunks for a question and numbers the sources per document. */
public final class PassageSelector {

    public record Candidate(Long documentId, String documentTitle, String courseTitle, String subjectName,
                            String kind, String searchText, String text) {
    }

    private PassageSelector() {
    }

    public static List<Passage> select(List<Candidate> candidates, String query, int topK, int maxPerDocument) {
        if (candidates.isEmpty() || query == null || query.isBlank()) {
            return List.of();
        }
        List<String> texts = candidates.stream().map(Candidate::searchText).toList();
        double[] scores = Bm25Ranker.score(query, texts);
        Integer[] order = IntStream.range(0, candidates.size()).boxed()
                .sorted((a, b) -> Double.compare(scores[b], scores[a]))
                .toArray(Integer[]::new);
        double best = scores[order[0]];
        if (best <= 0) {
            return List.of();
        }

        List<Passage> passages = new ArrayList<>();
        Map<Long, Integer> perDocument = new HashMap<>();
        Map<Long, Integer> numbers = new LinkedHashMap<>();
        for (int idx : order) {
            if (passages.size() >= topK || scores[idx] <= 0 || scores[idx] < best * 0.25) {
                break;
            }
            Candidate c = candidates.get(idx);
            if (perDocument.merge(c.documentId(), 1, Integer::sum) > maxPerDocument) {
                continue;
            }
            numbers.putIfAbsent(c.documentId(), numbers.size() + 1);
            passages.add(new Passage(numbers.get(c.documentId()), c.kind(), c.documentId(), c.documentTitle(),
                    c.courseTitle(), c.subjectName(), c.text()));
        }
        return passages;
    }
}