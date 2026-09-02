package com.examiq.backend.service;

import com.examiq.backend.entity.Paper;
import com.examiq.backend.entity.Rating;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.PaperRepository;
import com.examiq.backend.repository.RatingRepository;
import com.examiq.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    public RatingService(RatingRepository ratingRepository, PaperRepository paperRepository, UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Rating ratePaper(Long paperId, Long userId, Integer score, String comment) {
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("Rating score must be between 1 and 5");
        }

        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Rating> existingRating = ratingRepository.findByPaperAndUser(paper, user);
        if (existingRating.isPresent()) {
            Rating rating = existingRating.get();
            rating.setScore(score);
            rating.setComment(comment);
            return ratingRepository.save(rating);
        }

        Rating rating = new Rating();
        rating.setPaper(paper);
        rating.setUser(user);
        rating.setScore(score);
        rating.setComment(comment);
        return ratingRepository.save(rating);
    }

    public Double getAverageRating(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        List<Rating> ratings = ratingRepository.findByPaper(paper);
        if (ratings.isEmpty()) {
            return null;
        }

        return ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0.0);
    }

    public List<Rating> getPaperRatings(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        return ratingRepository.findByPaper(paper);
    }
}
