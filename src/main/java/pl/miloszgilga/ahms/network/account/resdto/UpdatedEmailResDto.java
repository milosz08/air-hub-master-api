package pl.miloszgilga.ahms.network.account.resdto;

import lombok.Builder;

@Builder
public record UpdatedEmailResDto(
    String message,
    String newEmail
) {
}
