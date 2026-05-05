package com.example.financemanager.service;

import com.example.financemanager.dto.IncomeDto;
import java.util.List;

public interface IncomeService {
    IncomeDto createIncome(IncomeDto incomeDto);
    List<IncomeDto> getAllIncomes(String period);
    IncomeDto getIncomeById(Long id);
    IncomeDto updateIncome(Long id, IncomeDto incomeDto);
    void deleteIncome(Long id);
}
