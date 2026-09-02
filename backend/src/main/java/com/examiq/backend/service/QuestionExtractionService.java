package com.examiq.backend.service;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Question;
import com.examiq.backend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts questions from an approved paper using the AI service's
 * segmentation endpoint and persists them so repeated-question analysis has
 * real rows to group over.
 */
@Service
public class QuestionExtractionService {

    private final QuestionRepository questionRepository;
    private final RestTemplate restTemplate;

    @Value("${app.ai.service-url:http://localhost:8001}")
    private String aiServiceUrl;

    public QuestionExtractionService(QuestionRepository questionRepository, RestTemplate restTemplate) {
        this.questionRepository = questionRepository;
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public void extractAndStoreQuestions(Paper paper) {
        if (paper == null || !questionRepository.findByPaper(paper).isEmpty()) {
            return;
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("text", paper.getTitle());
            request.put("paper_id", paper.getId());

            Map<String, Object> response = restTemplate.postForObject(aiServiceUrl + "/ai/segment", request,
                    Map.class);
            if (response == null || !response.containsKey("data")) {
                return;
            }
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            Object questionsObj = data.get("questions");
            if (!(questionsObj instanceof List<?> questions)) {
                return;
            }

            for (Object item : questions) {
                if (!(item instanceof Map<?, ?> map)) {
                    continue;
                }
                Object text = map.get("text");
                if (text == null || text.toString().isBlank()) {
                    continue;
                }
                Question question = new Question();
                question.setPaper(paper);
                question.setQuestionText(text.toString());
                Object marks = map.get("marks");
                if (marks instanceof Number number) {
                    question.setMarks(number.intValue());
                }
                questionRepository.save(question);
            }
        } catch (Exception e) {
            System.out.println("Warning: question extraction failed: " + e.getMessage());
        }
    }
}
