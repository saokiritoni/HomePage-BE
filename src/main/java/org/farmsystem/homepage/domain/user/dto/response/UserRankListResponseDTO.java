package org.farmsystem.homepage.domain.user.dto.response;

import java.util.List;

public record UserRankListResponseDTO(
        UserRankResponseDTO myRank,
        List<UserRankResponseDTO> userRankList
) {
}
