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
 * Groups extracted exam questions by semantic-ish similarity (word-overlap)
 * so genuinely repeated/high-recurrence topics can be surfaced without
 * claiming any question will appear again.
 */
@Service
public class RepeatedQuestionAnalysisService {

    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final Set<String> STOPWORDS = Set.of("the", "a", "an", "of", "in", "on", "for", "to", "and",
            "is", "are", "what", "explain", "describe", "define", "write", "list", "discuss", "with", "its", "any");

    private final QuestionRepository questionRepository;

    public RepeatedQuestionAnalysisService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Map<String, Object>> getRepeatedQuestions(Subject subject, int limit) {
        List<Question> questions = questionRepository
                .findByPaper_SubjectAndPaper_StatusOrderByCreatedAtDesc(subject, "APPROVED");

        List<QuestionEntry> entries = questions.stream()
                .map(q -> new QuestionEntry(q, normalize(q.getQuestionText())))
                .filter(e -> !e.tokens().isEmpty())
                .toList();

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

        int distinctPapersAnalyzed = (int) questions.stream().map(q -> q.getPaper().getId()).distinct().count();

        return groups.stream()
                .filter(group -> distinctPaperCount(group) >= 2)
                .sorted(Comparator.comparingInt((List<QuestionEntry> g) -> distinctPaperCount(g)).reversed())
                .limit(limit)
                .map(group -> toResult(group, distinctPapersAnalyzed))
                .collect(Collectors.toList());
    }

    private int distinctPaperCount(List<QuestionEntry> group) {
        return (int) group.stream().map(e -> e.question().getPaper().getId()).distinct().count();
    }

    private Map<String, Object> toResult(List<QuestionEntry> group, int papersAnalyzed) {
        int frequency = distinctPaperCount(group);
        Set<Integer> years = group.stream()
                .map(e -> e.question().getPaper().getYear())
                .filter(y -> y != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String representativeText = group.stream()
                .map(e -> e.question().getQuestionText())
                .max(Comparator.comparingInt(String::length))
                .orElse("");

        String recurrenceLabel;
        if (papersAnalyzed <= 0) {
            recurrenceLabel = "Appeared " + frequency + " times";
        } else {
            recurrenceLabel = "Appeared " + frequency + " times across the last " + papersAnalyzed
                    + " papers analyzed";
        }

        String tag = frequency >= 5 ? "High Recurrence" : frequency >= 3 ? "Frequently Asked" : "Repeated Topic";

        Map<String, Object> result = new HashMap<>();
        result.put("questionText", representativeText);
        result.put("frequency", frequency);
        result.put("papersAnalyzed", papersAnalyzed);
        result.put("recurrenceLabel", recurrenceLabel);
        result.put("tag", tag);
        result.put("years", years);
        return result;
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

    private Set<String> normalize(String text) {
        if (text == null) {
            return Set.of();
        }
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
        Set<String> tokens = new HashSet<>();
        for (String word : cleaned.split("\\s+")) {
            if (word.length() > 2 && !STOPWORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    private record QuestionEntry(Question question, Set<String> tokens) {
    }
}
