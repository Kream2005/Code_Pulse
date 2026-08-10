package com.stage.backend.mapper;

import com.stage.backend.dto.integrationlog.CreateIntegrationLogRequest;
import com.stage.backend.dto.integrationlog.IntegrationLogResponse;
import com.stage.backend.entity.IntegrationLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IntegrationLogMapper {
    CreateIntegrationLogRequest toIntegrationLogRequest(CreateIntegrationLogRequest request);
    IntegrationLog toEntity(CreateIntegrationLogRequest createIntegrationLogRequest);

    IntegrationLogResponse toIntegrationLogResponse(IntegrationLog entity);
    IntegrationLog toEntity(IntegrationLogResponse integrationLogResponse);
}