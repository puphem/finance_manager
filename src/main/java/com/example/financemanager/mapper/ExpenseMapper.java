package com.example.financemanager.mapper;

import com.example.financemanager.dto.ExpenseRequestDto;
import com.example.financemanager.dto.ExpenseResponseDto;
import com.example.financemanager.entity.Expense;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, SubcategoryMapper.class})
public interface ExpenseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subcategory", ignore = true)
    @Mapping(target = "receipt", ignore = true) // Игнорируем при создании
    Expense toEntity(ExpenseRequestDto dto);

    // Говорим мапперу, как преобразовать receipt в receipt
    @Mapping(source = "receipt", target = "receipt")
    ExpenseResponseDto toResponseDto(Expense entity);
}
