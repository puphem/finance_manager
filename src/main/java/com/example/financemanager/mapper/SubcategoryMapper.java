package com.example.financemanager.mapper;

import com.example.financemanager.dto.SubcategoryResponseDto;
import com.example.financemanager.entity.Subcategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubcategoryMapper {
    @Mapping(source = "category.id", target = "categoryId")
    SubcategoryResponseDto toResponseDto(Subcategory subcategory);
}
