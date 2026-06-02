package com.example.financemanager.service;

import com.example.financemanager.dto.CategoryPredictionDto;
import com.example.financemanager.entity.*;
import com.example.financemanager.repository.ExpenseRepository;
import com.example.financemanager.repository.UserCategoryModelStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PersonalCategoryModelService {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    private final ExpenseRepository expenseRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final UserCategoryModelStatsRepository modelStatsRepository;

    @Transactional(readOnly = true)
    public CategoryPredictionDto predict(String description, User user) {
        String normalized = normalize(description);
        if (normalized.isBlank()) {
            Category fallbackCategory = categoryAssignmentService.suggestCategory(description, user);
            Subcategory fallbackSubcategory = categoryAssignmentService.suggestSubcategory(description, fallbackCategory, user);
            return toDto(fallbackCategory, fallbackSubcategory, 0.20, "keyword-fallback");
        }

        List<Expense> history = expenseRepository.findByUserAndDateBetweenOrderByDateDesc(
                user,
                LocalDate.now().minusMonths(18),
                LocalDate.now()
        );

        Map<Long, ScoreBucket> buckets = new HashMap<>();
        Set<String> tokens = Set.of(SPLIT_PATTERN.split(normalized));

        for (Expense expense : history) {
            if (expense.getCategory() == null || expense.getDescription() == null || expense.getDescription().isBlank()) {
                continue;
            }
            String historyNormalized = normalize(expense.getDescription());
            if (historyNormalized.isBlank()) {
                continue;
            }
            int overlap = 0;
            for (String token : tokens) {
                if (!token.isBlank() && historyNormalized.contains(token)) {
                    overlap++;
                }
            }
            if (overlap == 0) {
                continue;
            }

            Long categoryId = expense.getCategory().getId();
            ScoreBucket bucket = buckets.computeIfAbsent(categoryId, ignored -> new ScoreBucket(expense.getCategory()));
            bucket.score += overlap;
            bucket.samples++;
            if (expense.getSubcategory() != null) {
                bucket.subcategoryVotes.merge(expense.getSubcategory().getId(), 1, Integer::sum);
                bucket.subcategories.put(expense.getSubcategory().getId(), expense.getSubcategory());
            }
        }

        if (buckets.isEmpty()) {
            Category fallbackCategory = categoryAssignmentService.suggestCategory(description, user);
            Subcategory fallbackSubcategory = categoryAssignmentService.suggestSubcategory(description, fallbackCategory, user);
            return toDto(fallbackCategory, fallbackSubcategory, 0.30, "keyword-fallback");
        }

        List<ScoreBucket> sorted = buckets.values().stream()
                .sorted((left, right) -> Integer.compare(right.score, left.score))
                .toList();

        ScoreBucket best = sorted.get(0);
        int totalScore = sorted.stream().mapToInt(item -> item.score).sum();
        double confidence = totalScore == 0 ? 0.35 : (double) best.score / (double) totalScore;
        confidence = Math.min(0.99, Math.max(0.35, confidence));

        Subcategory selectedSubcategory = best.resolveSubcategory();
        return toDto(best.category, selectedSubcategory, confidence, "personal-model");
    }

    @Transactional
    public void registerFeedback(User user, Long predictedCategoryId, Long finalCategoryId) {
        if (predictedCategoryId == null || finalCategoryId == null) {
            return;
        }

        UserCategoryModelStats stats = modelStatsRepository.findByUser(user)
                .orElseGet(() -> {
                    UserCategoryModelStats created = new UserCategoryModelStats();
                    created.setUser(user);
                    return created;
                });

        stats.setTotalPredictions(stats.getTotalPredictions() + 1);
        if (predictedCategoryId.equals(finalCategoryId)) {
            stats.setAcceptedPredictions(stats.getAcceptedPredictions() + 1);
        } else {
            stats.setCorrectedPredictions(stats.getCorrectedPredictions() + 1);
        }
        stats.setModelVersion(stats.getModelVersion() + 1);

        BigDecimal total = BigDecimal.valueOf(stats.getTotalPredictions());
        if (total.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal acceptedRate = BigDecimal.valueOf(stats.getAcceptedPredictions())
                    .divide(total, 4, RoundingMode.HALF_UP);
            stats.setAcceptRate(acceptedRate);
            stats.setPrecisionMetric(acceptedRate);
        }

        modelStatsRepository.save(stats);
    }

    private CategoryPredictionDto toDto(Category category, Subcategory subcategory, double confidence, String source) {
        return new CategoryPredictionDto(
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                subcategory == null ? null : subcategory.getId(),
                subcategory == null ? null : subcategory.getName(),
                confidence,
                source
        );
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .replaceAll("[^\\p{L}\\p{N}\\s]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static class ScoreBucket {
        private final Category category;
        private int score = 0;
        private int samples = 0;
        private final Map<Long, Integer> subcategoryVotes = new HashMap<>();
        private final Map<Long, Subcategory> subcategories = new HashMap<>();

        private ScoreBucket(Category category) {
            this.category = category;
        }

        private Subcategory resolveSubcategory() {
            if (subcategoryVotes.isEmpty()) {
                return null;
            }
            Long best = subcategoryVotes.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            return best == null ? null : subcategories.get(best);
        }
    }
}
