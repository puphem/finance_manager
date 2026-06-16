package com.example.financemanager.controller;

import com.example.financemanager.dto.PlannedExpenseConvertPreviewDto;
import com.example.financemanager.dto.PlannedExpenseRequestDto;
import com.example.financemanager.dto.PlannedExpenseResponseDto;
import com.example.financemanager.entity.enums.PlannedExpenseStatus;
import com.example.financemanager.service.PlannedExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/planned-expenses")
@RequiredArgsConstructor
public class PlannedExpenseController {

    private final PlannedExpenseService plannedExpenseService;

    @GetMapping
    public ResponseEntity<List<PlannedExpenseResponseDto>> list(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) PlannedExpenseStatus status
    ) {
        return ResponseEntity.ok(plannedExpenseService.list(filter, status));
    }

    @PostMapping
    public ResponseEntity<PlannedExpenseResponseDto> create(@Valid @RequestBody PlannedExpenseRequestDto request) {
        return ResponseEntity.ok(plannedExpenseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlannedExpenseResponseDto> update(@PathVariable Long id, @Valid @RequestBody PlannedExpenseRequestDto request) {
        return ResponseEntity.ok(plannedExpenseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        plannedExpenseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/convert-preview")
    public ResponseEntity<PlannedExpenseConvertPreviewDto> convertPreview(@PathVariable Long id) {
        return ResponseEntity.ok(plannedExpenseService.previewConvert(id));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Void> convert(@PathVariable Long id) {
        plannedExpenseService.convertToExpense(id);
        return ResponseEntity.ok().build();
    }
}
