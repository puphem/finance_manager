package com.example.financemanager.mapper;

import com.example.financemanager.dto.IncomeDto;
import com.example.financemanager.entity.Income;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IncomeMapper {
    Income toEntity(IncomeDto dto);
    IncomeDto toDto(Income entity);
}
