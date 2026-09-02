package com.examiq.backend.service;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Question;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Groups extracted exam questions by similarity so genuinely repeated /
 * high-recurrence topics can be surfaced without claiming any question will
 * appear again.
 *
 * The AI service's embedding endpoint ("/ai/embed") is currently a stub that
 * returns a fixed dimension/count rather than real vectors, so building
 * "semantic similarity" on top of it would just be fake AI theater. Instead
 * this uses word-overlap (Jaccard) similarity over a lightly stemmed and
 * synonym-normalized token set, which is enough to also match paraphrases
 * like "deadlock prevention" vs "preventing deadlocks" using only real,
 * explainable logic.
 */
@Service
public class RepeatedQuestionAnalysisService {

    private static final double SIMILARITY_THRESHOLD = 0.5;
    private static final Set<String> STOPWORDS = Set.of("the", "a", "an", "of", "in", "on", "for", "to", "and",
            "is", "are", "what", "explain", "describe", "define", "write", "list", "discuss", "with", "its", "any",
            "used", "use", "give", "brief", "briefly", "short", "note", "notes", "state");

    // Small hand-curated synonym map so common paraphrase pairs collapse to the
    // same token without pretending to do real NLP/embeddings.
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("preventing", "prevent"),
            Map.entry("prevention", "prevent"),
            Map.entry("prevents", "prevent"),
            Map.entry("methods", "method"),
            Map.entry("techniques", "method"),
            Map.entry("technique", "method"),
            Map.entry("approaches", "method"),
            Map.entry("approach", "method"),
            Map.entry("advantages", "advantage"),
            Map.entry("benefits", "advantage"),
            Map.entry("differences", "difference"),
            Map.entry("compare", "difference"),
            Map.entry("comparison", "difference"),
            Map.entry("types", "type"),
            Map.entry("kinds", "type"),
            Map.entry("algorithms", "algorithm"),
            Map.entry("properties", "property"));

    private final QuestionRepository questionRepository;

    public RepeatedQuestionAnalysisService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Map<String, Object>> getRepeatedQuestions(Subject subject, int limit) {
        return getRepeatedQuestions(subject, limit, null);
    }

    public List<Map<String, Object>> getRepeatedQuestions(Subject subject, int limit, String difficultyFilter) {
        List<Question> questions = questionRepository
                .findByPaper_SubjectAndPaper_StatusOrderByCreatedAtDesc(subject, "APPROVED");

        List<QuestionEntry> entries = questions.stream()
                .map(q -> new QuestionEntry(q, normalize(q.getQuestionText())))
                .filter(e -> !e.tokens().isEmpty())
                .toList();

        List<List<QuestionEntry>> groups = groupEntries(entries);

        int distinctPapersAnalyzed = (int) questions.stream().map(q -> q.getPaper().getId()).distinct().count();

        List<Map<String, Object>> results = groups.stream()
                .filter(group -> distinctPaperCount(group) >= 2)
                .sorted(Comparator.comparingInt((List<QuestionEntry> g) -> distinctPaperCount(g)).reversed())
                .map(group -> toResult(group, distinctPapersAnalyzed))
                .collect(Collectors.toList());

        if (difficultyFilter != null && !difficultyFilter.isBlank()) {
            String wanted = difficultyFilter.trim().toUpperCase(Locale.ROOT);
            List<Map<String, Object>> filtered = results.stream()
                    .filter(r -> wanted.equals(r.get("difficulty")))
                    .toList();
            if (!filtered.isEmpty()) {
                results = filtered;
            }
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /** Package-visible so the quiz generator can reuse the exact same grouping. */
    List<List<QuestionEntry>> groupEntries(List<QuestionEntry> entries) {
        List<List<QuestionEntry>> groups = new ArrayList<>();
        for (QuestionEntry entry : entries) {
            List<QuestionEntry> bestGroup = null;
            double bestScore = 0;
            for (List<QuestionEntry> group : groups) {
                double score = jaccard(entry.tokens(), group.get(0).tokens());
                if (score >= SIMILARITY_THRESHOLD && score > bestScore) {
                    bestScore = score;
                    bestGroup = group;
                }
            }
            if (bestGroup != null) {
                bestGroup.add(entry);
            } else {
                List<QuestionEntry> newGroup = new ArrayList<>();
                newGroup.add(entry);
                groups.add(newGroup);
            }
        }
        return groups;
    }

    List<QuestionEntry> buildEntries(List<Question> questions) {
        return questions.stream()
                .map(q -> new QuestionEntry(q, normalize(q.getQuestionText())))
                .filter(e -> !e.tokens().isEmpty())
                .toList();
    }

    int distinctPaperCount(List<QuestionEntry> group) {
        return (int) group.stream().map(e -> e.question().getPaper().getId()).distinct().count();
    }

    Map<String, Object> toResult(List<QuestionEntry> group, int papersAnalyzed) {
        int frequency = distinctPaperCount(group);
        Set<Integer> years = group.stream()
                .map(e -> e.question().getPaper().getYear())
                .filter(y -> y != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Integer mostRecentYear = years.stream().max(Integer::compareTo).orElse(null);

        String representativeText = group.stream()
                .map(e -> e.question().getQuestionText())
                .max(Comparator.comparingInt(String::length))
                .orElse("");

        String topicLabel = buildTopicLabel(group, representativeText);

        String recurrenceLabel = "Appeared in " + frequency + " paper" + (frequency == 1 ? "" : "s");
        if (mostRecentYear != null) {
            recurrenceLabel += " • most recently in " + mostRecentYear;
        }

        String tag = frequency >= 5 ? "High Recurrence" : frequency >= 3 ? "Frequently Asked" : "Repeated Topic";

        String difficulty = averageDifficulty(group);

        Map<String, Object> result = new HashMap<>();
        result.put("topicLabel", topicLabel);
        result.put("questionText", representativeText);
        result.put("frequency", frequency);
        result.put("papersAnalyzed", papersAnalyzed);
        result.put("recurrenceLabel", recurrenceLabel);
        result.put("tag", tag);
        result.put("years", years);
        result.put("mostRecentYear", mostRecentYear);
        result.put("difficulty", difficulty);
        return result;
    }

    /**
     * Derives a short human-readable topic label from the most frequent
     * meaningful tokens shared across the group, e.g. "Deadlock Prevention".
     * Falls back to a truncated version of the representative question when no
     * clear shared tokens exist.
     */
    private String buildTopicLabel(List<QuestionEntry> group, String representativeText) {
        Map<String, Integer> frequency = new HashMap<>();
        for (QuestionEntry entry : group) {
            for (String token : entry.tokens()) {
                frequency.merge(token, 1, Integer::sum);
            }
        }
        List<String> topTokens = frequency.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .map(Map.Entry::getKey)
                .limit(2)
                .toList();

        if (topTokens.isEmpty()) {
            return representativeText.length() > 60 ? representativeText.substring(0, 60) + "…" : representativeText;
        }

        return topTokens.stream()
                .map(t -> t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1))
                .collect(Collectors.joining(" "));
    }

    /** Real-data difficulty bucket from the marks recorded on extracted questions. */
    private String averageDifficulty(List<QuestionEntry> group) {
        double avgMarks = group.stream()
                .map(e -> e.question().getMarks())
                .filter(m -> m != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(-1);
        if (avgMarks < 0) {
            return "MEDIUM";
        }
        if (avgMarks <= 5) {
            return "EASY";
        }
        if (avgMarks <= 9) {
            return "MEDIUM";
        }
        return "HARD";
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    Set<String> normalize(String text) {
        if (text == null) {
            return Set.of();
        }
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        Set<String> tokens = new HashSet<>();
        for (String word : cleaned.split("\\s+")) {
            if (word.length() <= 2 || STOPWORDS.contains(word)) {
                continue;
            }
            tokens.add(stem(SYNONYMS.getOrDefault(word, word)));
        }
        return tokens;
    }

    /** Very small suffix-stripping stemmer - not linguistically perfect, just enough to fold plurals/verb forms together. */
    private String stem(String word) {
        if (word.length() > 6 && word.endsWith("ations")) {
            return word.substring(0, word.length() - 6);
        }
        if (word.length() > 5 && word.endsWith("ation")) {
            return word.substring(0, word.length() - 5);
        }
        if (word.length() > 5 && word.endsWith("ing")) {
            return word.substring(0, word.length() - 3);
        }
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.length() > 4 && (word.endsWith("es"))) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.length() > 4 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    record QuestionEntry(Question question, Set<String> tokens) {
    }
}
