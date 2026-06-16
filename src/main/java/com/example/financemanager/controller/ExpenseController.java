package com.example.financemanager.controller;

import com.example.financemanager.dto.CategoryExpenseDto;
import com.example.financemanager.dto.CategoryPredictionDto;
import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;
import com.example.financemanager.dto.SubcategoryExpenseDto;
import com.example.financemanager.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> createExpense(@Valid @RequestBody ExpenseRequestDto expenseDto) {
        return new ResponseEntity<>(expenseService.createExpense(expenseDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponseDto>> getAllExpenses(
            @RequestParam(required = false) String period
    ) {
        return ResponseEntity.ok(expenseService.getAllExpenses(period));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> getExpenseById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequestDto expenseDto) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expenseDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/summary-by-category")
    public ResponseEntity<List<CategoryExpenseDto>> getSummaryByCategory(@RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(expenseService.getCategoryExpenseSummary(period));
    }

    @GetMapping("/summary-by-subcategory")
    public ResponseEntity<List<SubcategoryExpenseDto>> getSummaryBySubcategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "month") String period
    ) {
        return ResponseEntity.ok(expenseService.getSubcategoryExpenseSummary(categoryId, period));
    }

    @GetMapping("/predict-category")
    public ResponseEntity<CategoryPredictionDto> predictCategory(@RequestParam String description) {
        return ResponseEntity.ok(expenseService.predictCategory(description));
    }
}
