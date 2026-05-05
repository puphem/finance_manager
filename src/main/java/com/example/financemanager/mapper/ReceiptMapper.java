package com.example.financemanager.mapper;

import com.example.financemanager.dto.ReceiptResponseDto;
import com.example.financemanager.entity.Receipt;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ExpenseMapper.class})
public interface ReceiptMapper {
    ReceiptResponseDto toResponseDto(Receipt receipt);
}
