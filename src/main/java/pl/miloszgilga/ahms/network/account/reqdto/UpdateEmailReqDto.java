package pl.miloszgilga.ahms.network.account.reqdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateEmailReqDto {
    @NotBlank(message = "jpa.validator.email.notBlank")
    @Email(message = "jpa.validator.email.regex")
    private String newEmail;

    @Override
    public String toString() {
        return "{" +
            "newEmail=" + newEmail +
            '}';
    }
}
