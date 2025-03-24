package pl.miloszgilga.ahms.network.bought.resdto;

import pl.miloszgilga.ahms.domain.in_game_worker_params.InGameWorkerParamEntity;
import pl.miloszgilga.ahms.utils.Utilities;

import java.util.Objects;

public record InGameWorkerResDto(
    String fullName,
    String categoryName,
    int experience,
    int cooperation,
    int rebelliousness,
    int skills,
    boolean isAvailable
) {
    public InGameWorkerResDto(InGameWorkerParamEntity entity) {
        this(Utilities.parseWorkerFullName(entity.getWorker()), entity.getWorker().getCategory().getName(),
            entity.getExperience(), entity.getCooperation(), entity.getRebelliousness(), entity.getSkills(),
            Objects.isNull(entity.getAvailable()));
    }
}
