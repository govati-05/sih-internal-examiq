package com.examiq.backend.service;

import com.examiq.backend.entity.ContributorScore;
import com.examiq.backend.entity.User;
import com.examiq.backend.repository.ContributorScoreRepository;
import com.examiq.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ContributorScoreService {

    private final ContributorScoreRepository contributorScoreRepository;
    private final UserRepository userRepository;

    public ContributorScoreService(ContributorScoreRepository contributorScoreRepository, UserRepository userRepository) {
        this.contributorScoreRepository = contributorScoreRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ContributorScore getOrCreateScore(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<ContributorScore> existingScore = contributorScoreRepository.findByUser(user);
        if (existingScore.isPresent()) {
            return existingScore.get();
        }

        ContributorScore score = new ContributorScore();
        score.setUser(user);
        score.setScore(0);
        score.setTier("NEW_CONTRIBUTOR");
        return contributorScoreRepository.save(score);
    }

    @Transactional
    public ContributorScore addPoints(Long userId, int points) {
        ContributorScore score = getOrCreateScore(userId);
        score.setScore(score.getScore() + points);
        updateTier(score);
        return contributorScoreRepository.save(score);
    }

    @Transactional
    public ContributorScore subtractPoints(Long userId, int points) {
        ContributorScore score = getOrCreateScore(userId);
        score.setScore(Math.max(0, score.getScore() - points));
        updateTier(score);
        return contributorScoreRepository.save(score);
    }

    private void updateTier(ContributorScore score) {
        int points = score.getScore();
        if (points >= 100) {
            score.setTier("VERIFIED_FACULTY");
        } else if (points >= 50) {
            score.setTier("TOP_CONTRIBUTOR");
        } else if (points >= 20) {
            score.setTier("TRUSTED_CONTRIBUTOR");
        } else {
            score.setTier("NEW_CONTRIBUTOR");
        }
    }
}
