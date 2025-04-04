package pl.miloszgilga.ahms.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import pl.miloszgilga.ahms.domain.user.UserEntity;
import pl.miloszgilga.ahms.domain.worker.WorkerEntity;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utilities {
    public static String parseFullName(UserEntity user) {
        return user.getFirstName() + " " + user.getLastName();
    }

    public static String parseWorkerFullName(WorkerEntity worker) {
        return worker.getFirstName() + " " + worker.getLastName();
    }

    public static String parseToCamelCase(String input) {
        final StringBuilder builder = new StringBuilder();
        for (final String part : input.split(" ")) {
            builder.append(StringUtils.capitalize(part));
        }
        return StringUtils.uncapitalize(StringUtils.deleteWhitespace(builder.toString()));
    }
}
