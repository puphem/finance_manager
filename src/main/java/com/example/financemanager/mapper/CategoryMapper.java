package com.example.financemanager.mapper;

import com.example.financemanager.dto.CategoryRequestDto;
import com.example.financemanager.dto.CategoryResponseDto;
import com.example.financemanager.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {SubcategoryMapper.class})
public interface CategoryMapper {

    Category toEntity(CategoryRequestDto dto);

    CategoryResponseDto toResponseDto(Category entity);
}
