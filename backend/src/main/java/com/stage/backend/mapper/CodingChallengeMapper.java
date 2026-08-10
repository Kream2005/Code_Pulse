package com.stage.backend.mapper;

import com.stage.backend.dto.codingchallenge.CodingChallengeDto;
import com.stage.backend.entity.CodingChallenge;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CodingChallengeMapper {
    CodingChallengeDto toCodingChallengeDto(CodingChallenge codingChallenge);
    CodingChallenge toEntity(CodingChallengeDto codingChallengeDto);
}
