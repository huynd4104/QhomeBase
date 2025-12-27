package com.QhomeBase.financebillingservice.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BuildingMapper {

    @Mapping(source = "codeName", target = "code")
    BuildingDto buildingToDto(Building building);
}





























