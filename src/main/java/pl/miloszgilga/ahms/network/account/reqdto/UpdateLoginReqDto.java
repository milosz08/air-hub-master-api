package pl.miloszgilga.ahms.network.account.reqdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.miloszgilga.ahms.validator.Regex;

@Data
@NoArgsConstructor
public class UpdateLoginReqDto {
    @NotBlank(message = "jpa.validator.login.notBlank")
    @Pattern(regexp = Regex.LOGIN, message = "jpa.validator.login.regex")
    private String newLogin;

    @Override
    public String toString() {
        return "{" +
            "newLogin=" + newLogin +
            '}';
    }
}
