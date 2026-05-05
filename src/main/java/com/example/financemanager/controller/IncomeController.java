package com.example.financemanager.controller;

import com.example.financemanager.dto.IncomeDto;
import com.example.financemanager.service.IncomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/incomes")
@RequiredArgsConstructor
public class IncomeController {

    private final IncomeService incomeService;

    @PostMapping
    public ResponseEntity<IncomeDto> createIncome(@Valid @RequestBody IncomeDto incomeDto) {
        return new ResponseEntity<>(incomeService.createIncome(incomeDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IncomeDto>> getAllIncomes(@RequestParam(required = false) String period) {
        return ResponseEntity.ok(incomeService.getAllIncomes(period));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncomeDto> getIncomeById(@PathVariable Long id) {
        return ResponseEntity.ok(incomeService.getIncomeById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncomeDto> updateIncome(@PathVariable Long id, @Valid @RequestBody IncomeDto incomeDto) {
        return ResponseEntity.ok(incomeService.updateIncome(id, incomeDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id) {
        incomeService.deleteIncome(id);
        return ResponseEntity.noContent().build();
    }
}
