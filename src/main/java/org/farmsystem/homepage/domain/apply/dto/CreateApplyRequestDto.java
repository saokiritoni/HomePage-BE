package org.farmsystem.homepage.domain.apply.dto;

public record CreateApplyRequestDto(String password, String name, String major, String studentNumber, String phoneNumber, String email) {
    // TODO: Validation 추가
}
