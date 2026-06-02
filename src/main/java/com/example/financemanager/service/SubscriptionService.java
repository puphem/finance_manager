package com.example.financemanager.service;

import com.example.financemanager.dto.SubscriptionAlertDto;
import com.example.financemanager.dto.SubscriptionRequestDto;
import com.example.financemanager.dto.SubscriptionResponseDto;
import com.example.financemanager.entity.*;
import com.example.financemanager.entity.enums.RecurringEntryType;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPostingRepository subscriptionPostingRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final CategoryAssignmentService categoryAssignmentService;
    private final CurrentUserResolver currentUserResolver;

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> listCurrentUserSubscriptions() {
        User user = currentUserResolver.getCurrentUser();
        return subscriptionRepository.findAllByUserOrderByNextChargeDateAsc(user).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SubscriptionResponseDto create(SubscriptionRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        Subscription subscription = new Subscription();
        applyRequest(subscription, request, user);
        subscription.setUser(user);
        subscriptionRepository.save(subscription);
        return toDto(subscription);
    }

    @Transactional
    public SubscriptionResponseDto update(Long id, SubscriptionRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        Subscription subscription = subscriptionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Подписка с ID " + id + " не найдена."));
        applyRequest(subscription, request, user);
        subscriptionRepository.save(subscription);
        return toDto(subscription);
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUserResolver.getCurrentUser();
        Subscription subscription = subscriptionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Подписка с ID " + id + " не найдена."));
        subscriptionRepository.delete(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDto> getUpcoming(int days) {
        User user = currentUserResolver.getCurrentUser();
        LocalDate today = LocalDate.now();
        LocalDate end = today.plusDays(Math.max(0, days));
        return subscriptionRepository.findAllByUserAndNextChargeDateBetweenOrderByNextChargeDateAsc(user, today, end).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionAlertDto> getUpcomingAlerts(int days) {
        return getUpcoming(days).stream()
                .map(item -> new SubscriptionAlertDto(
                        item.getId(),
                        item.getSource(),
                        item.getAmount(),
                        item.getNextChargeDate(),
                        "Списание подписки «" + item.getSource() + "» запланировано на " + item.getNextChargeDate()
                ))
                .toList();
    }

    @Transactional
    public void processDueSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> dueSubscriptions = subscriptionRepository.findAllByActiveTrueAndNextChargeDateLessThanEqual(today);

        for (Subscription subscription : dueSubscriptions) {
            if (!subscription.isAutoPostEnabled()) {
                continue;
            }

            LocalDate cursor = subscription.getNextChargeDate();
            boolean changed = false;

            while (cursor != null && !cursor.isAfter(today)) {
                if (!subscriptionPostingRepository.existsBySubscriptionAndPostingDate(subscription, cursor)) {
                    postTransaction(subscription, cursor);
                    SubscriptionPosting posting = new SubscriptionPosting();
                    posting.setSubscription(subscription);
                    posting.setPostingDate(cursor);
                    posting.setStatus("POSTED");
                    posting.setDetails("Автоматическое создание транзакции");
                    subscriptionPostingRepository.save(posting);
                }

                cursor = cursor.plusDays(Math.max(1, subscription.getPeriodDays()));
                changed = true;
            }

            if (changed && cursor != null) {
                subscription.setNextChargeDate(cursor);
                subscriptionRepository.save(subscription);
            }
        }
    }

    private void postTransaction(Subscription subscription, LocalDate date) {
        if (subscription.getEntryType() == RecurringEntryType.INCOME) {
            Income income = new Income();
            income.setUser(subscription.getUser());
            income.setAmount(subscription.getAmount());
            income.setDate(date);
            income.setDescription(subscription.getDescription() == null || subscription.getDescription().isBlank()
                    ? "Автозапись: " + subscription.getSource()
                    : subscription.getDescription());
            incomeRepository.save(income);
            return;
        }

        Expense expense = new Expense();
        expense.setUser(subscription.getUser());
        expense.setAmount(subscription.getAmount());
        expense.setDate(date);
        expense.setDescription(subscription.getDescription() == null || subscription.getDescription().isBlank()
                ? "Автозапись: " + subscription.getSource()
                : subscription.getDescription());

        Category category = subscription.getCategory();
        if (category == null) {
            category = categoryAssignmentService.suggestCategory(expense.getDescription(), subscription.getUser());
        }
        expense.setCategory(category);
        expense.setSubcategory(subscription.getSubcategory());
        expenseRepository.save(expense);
    }

    private void applyRequest(Subscription subscription, SubscriptionRequestDto request, User user) {
        subscription.setSource(request.getSource().trim());
        subscription.setAmount(request.getAmount());
        subscription.setEntryType(request.getEntryType());
        subscription.setPeriodDays(request.getPeriodDays());
        subscription.setNextChargeDate(request.getNextChargeDate());
        subscription.setAutoPostEnabled(request.isAutoPostEnabled());
        subscription.setNotificationEnabled(request.isNotificationEnabled());
        subscription.setActive(request.isActive());
        subscription.setDescription(request.getDescription());

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .filter(candidate -> candidate.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Категория с ID " + request.getCategoryId() + " не найдена."));
        }

        Subcategory subcategory = null;
        if (request.getSubcategoryId() != null) {
            subcategory = subcategoryRepository.findByIdAndCategoryUser(request.getSubcategoryId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Подкатегория с ID " + request.getSubcategoryId() + " не найдена."));
            if (category != null && !subcategory.getCategory().getId().equals(category.getId())) {
                throw new IllegalArgumentException("Подкатегория не принадлежит выбранной категории.");
            }
            if (category == null) {
                category = subcategory.getCategory();
            }
        }

        subscription.setCategory(category);
        subscription.setSubcategory(subcategory);
    }

    private SubscriptionResponseDto toDto(Subscription subscription) {
        return new SubscriptionResponseDto(
                subscription.getId(),
                subscription.getSource(),
                subscription.getAmount(),
                subscription.getEntryType(),
                subscription.getPeriodDays(),
                subscription.getNextChargeDate(),
                subscription.isAutoPostEnabled(),
                subscription.isNotificationEnabled(),
                subscription.isActive(),
                subscription.getCategory() == null ? null : subscription.getCategory().getId(),
                subscription.getCategory() == null ? null : subscription.getCategory().getName(),
                subscription.getSubcategory() == null ? null : subscription.getSubcategory().getId(),
                subscription.getSubcategory() == null ? null : subscription.getSubcategory().getName(),
                subscription.getDescription()
        );
    }
}
