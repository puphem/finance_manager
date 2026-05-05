package com.example.financemanager.service.impl;

import com.example.financemanager.dto.IncomeDto;
import com.example.financemanager.entity.Income;
import com.example.financemanager.exception.ResourceNotFoundException;
import com.example.financemanager.mapper.IncomeMapper;
import com.example.financemanager.repository.IncomeRepository;
import com.example.financemanager.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncomeServiceImpl implements IncomeService {

    private final IncomeRepository incomeRepository;
    private final IncomeMapper incomeMapper;

    @Override
    @Transactional
    public IncomeDto createIncome(IncomeDto incomeDto) {
        Income income = incomeMapper.toEntity(incomeDto);
        Income savedIncome = incomeRepository.save(income);
        return incomeMapper.toDto(savedIncome);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeDto> getAllIncomes(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;

        if ("day".equalsIgnoreCase(period)) {
            startDate = today;
        } else if ("week".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        } else if ("month".equalsIgnoreCase(period)) {
            startDate = today.with(TemporalAdjusters.firstDayOfMonth());
        } else {
            return incomeRepository.findAllByOrderByDateDesc().stream()
                    .map(incomeMapper::toDto)
                    .collect(Collectors.toList());
        }

        return incomeRepository.findByDateBetweenOrderByDateDesc(startDate, today).stream()
                .map(incomeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeDto getIncomeById(Long id) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Доход с ID " + id + " не найден."));
        return incomeMapper.toDto(income);
    }

    @Override
    @Transactional
    public IncomeDto updateIncome(Long id, IncomeDto incomeDto) {
        Income income = incomeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Доход с ID " + id + " не найден."));

        income.setAmount(incomeDto.getAmount());
        income.setDate(incomeDto.getDate());
        income.setDescription(incomeDto.getDescription());

        Income updatedIncome = incomeRepository.save(income);
        return incomeMapper.toDto(updatedIncome);
    }

    @Override
    @Transactional
    public void deleteIncome(Long id) {
        if (!incomeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Доход с ID " + id + " не найден.");
        }
        incomeRepository.deleteById(id);
    }
}
