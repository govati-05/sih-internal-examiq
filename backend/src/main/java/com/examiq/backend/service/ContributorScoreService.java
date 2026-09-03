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
    private final NotificationService notificationService;

    public ContributorScoreService(ContributorScoreRepository contributorScoreRepository, UserRepository userRepository,
            NotificationService notificationService) {
        this.contributorScoreRepository = contributorScoreRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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
        String previousTier = score.getTier();
        score.setScore(score.getScore() + points);
        updateTier(score);
        ContributorScore saved = contributorScoreRepository.save(score);

        if (previousTier != null && !previousTier.equals(saved.getTier()) && isUpgrade(previousTier, saved.getTier())) {
            try {
                notificationService.createNotification(userId, "Contributor Achievement Unlocked",
                        "Congratulations! You've reached the " + badgeLabel(saved.getTier())
                                + " tier with " + saved.getScore() + " points.",
                        "CONTRIBUTOR_ACHIEVEMENT");
            } catch (Exception e) {
                System.err.println("Failed to create contributor achievement notification: " + e.getMessage());
            }
        }
        return saved;
    }

    private boolean isUpgrade(String previousTier, String newTier) {
        return tierRank(newTier) > tierRank(previousTier);
    }

    private int tierRank(String tier) {
        if (tier == null) {
            return 0;
        }
        return switch (tier) {
            case "TRUSTED_CONTRIBUTOR" -> 1;
            case "TOP_CONTRIBUTOR" -> 2;
            case "VERIFIED_FACULTY" -> 3;
            default -> 0;
        };
    }

    public String badgeLabel(String tier) {
        if (tier == null) {
            return "Contributor";
        }
        return switch (tier) {
            case "TOP_CONTRIBUTOR", "VERIFIED_FACULTY" -> "Top Contributor";
            case "TRUSTED_CONTRIBUTOR" -> "Trusted Contributor";
            default -> "Contributor";
        };
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
