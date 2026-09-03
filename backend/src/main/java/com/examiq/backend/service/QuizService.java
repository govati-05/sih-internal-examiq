package com.examiq.backend.service;

import com.examiq.backend.entity.Question;
import com.examiq.backend.entity.QuizAttempt;
import com.examiq.backend.entity.Subject;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.QuestionRepository;
import com.examiq.backend.repository.QuizAttemptRepository;
import com.examiq.backend.repository.SubjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates lightweight "recognition" practice quizzes from real extracted
 * questions and the repeated-question analysis - no fabricated Q&A pairs.
 * Each round asks the student to spot which of several real past-paper
 * questions has actually been the most repeated one for the subject, which
 * is honestly derivable from stored data rather than invented by an LLM.
 */
@Service
public class QuizService {

    private static final int MIN_QUESTIONS = 5;
    private static final int MAX_QUESTIONS = 10;

    private final QuestionRepository questionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final SubjectRepository subjectRepository;
    private final RepeatedQuestionAnalysisService repeatedQuestionAnalysisService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    public QuizService(QuestionRepository questionRepository,
            QuizAttemptRepository quizAttemptRepository,
            SubjectRepository subjectRepository,
            RepeatedQuestionAnalysisService repeatedQuestionAnalysisService,
            ObjectMapper objectMapper) {
        this.questionRepository = questionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.subjectRepository = subjectRepository;
        this.repeatedQuestionAnalysisService = repeatedQuestionAnalysisService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> generateQuiz(User user, Long subjectId, Integer studentYear, Integer numQuestions,
            String difficulty) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        int requested = numQuestions == null ? MIN_QUESTIONS : numQuestions;
        requested = Math.max(MIN_QUESTIONS, Math.min(MAX_QUESTIONS, requested));

        List<Question> questions = questionRepository
                .findByPaper_SubjectAndPaper_StatusOrderByCreatedAtDesc(subject, "APPROVED");
        if (studentYear != null) {
            questions = questions.stream()
                    .filter(q -> studentYear.equals(q.getPaper().getStudentYear()))
                    .toList();
        }

        if (questions.size() < 4) {
            throw new IllegalArgumentException(
                    "Not enough extracted questions for " + subject.getCanonicalName()
                            + " yet to build a practice quiz. Try another subject, or check back once more papers are approved.");
        }

        List<RepeatedQuestionAnalysisService.QuestionEntry> entries = repeatedQuestionAnalysisService
                .buildEntries(questions);
        List<List<RepeatedQuestionAnalysisService.QuestionEntry>> groups = repeatedQuestionAnalysisService
                .groupEntries(entries);

        int papersAnalyzed = (int) questions.stream().map(q -> q.getPaper().getId()).distinct().count();

        List<Map<String, Object>> topicGroups = new ArrayList<>();
        for (List<RepeatedQuestionAnalysisService.QuestionEntry> group : groups) {
            Map<String, Object> result = repeatedQuestionAnalysisService.toResult(group, papersAnalyzed);
            result.put("__groupTexts", group.stream().map(e -> e.question().getQuestionText()).distinct().toList());
            topicGroups.add(result);
        }
        topicGroups.sort((a, b) -> ((Integer) b.get("frequency")).compareTo((Integer) a.get("frequency")));

        if (difficulty != null && !difficulty.isBlank()) {
            String wanted = difficulty.trim().toUpperCase();
            List<Map<String, Object>> filtered = topicGroups.stream()
                    .filter(g -> wanted.equals(g.get("difficulty")))
                    .toList();
            if (!filtered.isEmpty()) {
                topicGroups = filtered;
            }
        }

        List<String> allQuestionTexts = questions.stream().map(Question::getQuestionText).distinct().toList();
        if (allQuestionTexts.size() < 4) {
            throw new IllegalArgumentException(
                    "Not enough distinct extracted questions for " + subject.getCanonicalName()
                            + " to build multiple-choice options yet.");
        }

        int actualCount = Math.min(requested, topicGroups.size());
        if (actualCount < 1) {
            throw new IllegalArgumentException(
                    "No repeated topics found yet for " + subject.getCanonicalName()
                            + ". At least two similar questions across different papers are needed to build a practice quiz.");
        }

        List<Map<String, Object>> quizQuestions = new ArrayList<>();
        for (int i = 0; i < actualCount; i++) {
            Map<String, Object> topicGroup = topicGroups.get(i);
            @SuppressWarnings("unchecked")
            List<String> groupTexts = (List<String>) topicGroup.get("__groupTexts");
            String correctText = groupTexts.get(0);

            List<String> distractorPool = new ArrayList<>(allQuestionTexts);
            distractorPool.removeAll(groupTexts);
            Collections.shuffle(distractorPool, random);

            List<String> options = new ArrayList<>();
            options.add(correctText);
            for (String candidate : distractorPool) {
                if (options.size() >= 4) {
                    break;
                }
                options.add(candidate);
            }
            // Graceful fallback if the subject has very few distinct questions overall.
            if (options.size() < 2) {
                continue;
            }
            Collections.shuffle(options, random);
            int correctIndex = options.indexOf(correctText);

            Map<String, Object> quizQuestion = new HashMap<>();
            quizQuestion.put("prompt", "Which of the following questions has been most frequently repeated in "
                    + subject.getCanonicalName() + " exams?");
            quizQuestion.put("options", options);
            quizQuestion.put("correctIndex", correctIndex);
            quizQuestion.put("topicLabel", topicGroup.get("topicLabel"));
            quizQuestion.put("difficulty", topicGroup.get("difficulty"));
            quizQuestion.put("frequency", topicGroup.get("frequency"));
            quizQuestion.put("recurrenceLabel", topicGroup.get("recurrenceLabel"));
            quizQuestions.add(quizQuestion);
        }

        if (quizQuestions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Not enough varied questions for " + subject.getCanonicalName() + " to build a practice quiz yet.");
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setUser(user);
        attempt.setSubject(subject);
        attempt.setStudentYear(studentYear);
        attempt.setStatus("PENDING");
        attempt.setTotalQuestions(quizQuestions.size());
        attempt.setQuestionsJson(writeJson(quizQuestions));
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return buildQuizResponse(saved, quizQuestions, false);
    }

    @Transactional
    public Map<String, Object> submitQuiz(User user, Long quizId, List<Integer> answers) {
        QuizAttempt attempt = quizAttemptRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        if (attempt.getUser() == null || !attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to submit this quiz");
        }
        if (!"PENDING".equalsIgnoreCase(attempt.getStatus())) {
            throw new IllegalArgumentException("This quiz has already been submitted");
        }

        List<Map<String, Object>> questions = readJson(attempt.getQuestionsJson());
        int correctCount = 0;
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> question = questions.get(i);
            Integer correctIndex = (Integer) question.get("correctIndex");
            Integer selected = (answers != null && i < answers.size()) ? answers.get(i) : null;
            boolean isCorrect = selected != null && selected.equals(correctIndex);
            question.put("selectedIndex", selected);
            question.put("isCorrect", isCorrect);
            if (isCorrect) {
                correctCount++;
            }
        }

        double percentage = questions.isEmpty() ? 0.0 : (correctCount * 100.0) / questions.size();

        attempt.setStatus("COMPLETED");
        attempt.setCorrectCount(correctCount);
        attempt.setScorePercentage(Math.round(percentage * 10.0) / 10.0);
        attempt.setQuestionsJson(writeJson(questions));
        attempt.setCompletedAt(LocalDateTime.now());
        QuizAttempt saved = quizAttemptRepository.save(attempt);

        return buildQuizResponse(saved, questions, true);
    }

    public Map<String, Object> getAttempt(User user, Long quizId) {
        QuizAttempt attempt = quizAttemptRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));
        if (attempt.getUser() == null || !attempt.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not authorized to view this quiz");
        }
        List<Map<String, Object>> questions = readJson(attempt.getQuestionsJson());
        return buildQuizResponse(attempt, questions, "COMPLETED".equalsIgnoreCase(attempt.getStatus()));
    }

    public List<Map<String, Object>> getHistory(User user) {
        return quizAttemptRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::toSummary)
                .toList();
    }

    public Map<String, Object> getQuizStats(User user) {
        List<QuizAttempt> completed = quizAttemptRepository.findByUserAndStatusOrderByCreatedAtDesc(user, "COMPLETED");
        Map<String, Object> stats = new HashMap<>();
        stats.put("attemptsCount", completed.size());
        double avgScore = completed.stream()
                .filter(a -> a.getScorePercentage() != null)
                .mapToDouble(QuizAttempt::getScorePercentage)
                .average()
                .orElse(0.0);
        stats.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
        return stats;
    }

    private Map<String, Object> toSummary(QuizAttempt attempt) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", attempt.getId());
        item.put("subjectName", attempt.getSubject() != null ? attempt.getSubject().getCanonicalName() : "Unknown");
        item.put("status", attempt.getStatus());
        item.put("totalQuestions", attempt.getTotalQuestions());
        item.put("correctCount", attempt.getCorrectCount());
        item.put("scorePercentage", attempt.getScorePercentage());
        item.put("createdAt", attempt.getCreatedAt());
        item.put("completedAt", attempt.getCompletedAt());
        return item;
    }

    private Map<String, Object> buildQuizResponse(QuizAttempt attempt, List<Map<String, Object>> questions,
            boolean includeAnswers) {
        Map<String, Object> response = new HashMap<>();
        response.put("quizId", attempt.getId());
        response.put("subjectName", attempt.getSubject() != null ? attempt.getSubject().getCanonicalName() : "Unknown");
        response.put("status", attempt.getStatus());
        response.put("totalQuestions", attempt.getTotalQuestions());

        List<Map<String, Object>> outQuestions = new ArrayList<>();
        List<String> topicsNeedingRevision = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            Map<String, Object> out = new HashMap<>();
            out.put("prompt", q.get("prompt"));
            out.put("options", q.get("options"));
            out.put("difficulty", q.get("difficulty"));
            if (includeAnswers) {
                out.put("correctIndex", q.get("correctIndex"));
                out.put("selectedIndex", q.get("selectedIndex"));
                out.put("isCorrect", q.get("isCorrect"));
                out.put("topicLabel", q.get("topicLabel"));
                out.put("recurrenceLabel", q.get("recurrenceLabel"));
                if (Boolean.FALSE.equals(q.get("isCorrect")) && q.get("topicLabel") != null) {
                    topicsNeedingRevision.add((String) q.get("topicLabel"));
                }
            }
            outQuestions.add(out);
        }
        response.put("questions", outQuestions);

        if (includeAnswers) {
            response.put("correctCount", attempt.getCorrectCount());
            response.put("scorePercentage", attempt.getScorePercentage());
            response.put("topicsNeedingRevision", topicsNeedingRevision.stream().distinct().toList());
        }
        return response;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist quiz data", e);
        }
    }

    private List<Map<String, Object>> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read stored quiz data", e);
        }
    }
}
