package pl.miloszgilga.ahms.network.crew;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.miloszgilga.ahms.domain.in_game_crew.InGameCrewEntity;
import pl.miloszgilga.ahms.domain.in_game_plane_params.InGamePlaneParamEntity;
import pl.miloszgilga.ahms.domain.in_game_plane_params.InGamePlaneParamRepository;
import pl.miloszgilga.ahms.domain.in_game_worker_params.InGameWorkerParamEntity;
import pl.miloszgilga.ahms.domain.in_game_worker_params.InGameWorkerParamRepository;
import pl.miloszgilga.ahms.domain.user.UserEntity;
import pl.miloszgilga.ahms.dto.SimpleMessageResDto;
import pl.miloszgilga.ahms.exception.rest.CrewException.PlaneNotExistOrNotBoughtException;
import pl.miloszgilga.ahms.exception.rest.CrewException.WorkerNotExistOrNotBoughtException;
import pl.miloszgilga.ahms.i18n.AppLocaleSet;
import pl.miloszgilga.ahms.i18n.LocaleMessageService;
import pl.miloszgilga.ahms.network.crew.dto.CrewPlaneDto;
import pl.miloszgilga.ahms.network.crew.dto.CrewWorkerDto;
import pl.miloszgilga.ahms.network.crew.reqdto.AddCrewReqDto;
import pl.miloszgilga.ahms.network.crew.resdto.CrewDataResDto;
import pl.miloszgilga.ahms.security.LoggedUser;
import pl.miloszgilga.ahms.utils.Utilities;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class CrewServiceImpl implements CrewService {
    private final LocaleMessageService localeMessageService;

    private final InGamePlaneParamRepository inGamePlaneParamRepository;
    private final InGameWorkerParamRepository inGameWorkerParamRepository;

    @Override
    public CrewDataResDto getCrew(LoggedUser loggedUser) {
        final UserEntity userEntity = loggedUser.userEntity();

        final List<InGamePlaneParamEntity> planes = inGamePlaneParamRepository
            .findAllExcInJoiningTable(userEntity.getId());

        final List<InGameWorkerParamEntity> workers = inGameWorkerParamRepository
            .findAllExcInJoiningTable(userEntity.getId());

        final List<CrewPlaneDto> planeDtos = planes.stream()
            .map(CrewPlaneDto::new)
            .toList();

        final Map<String, List<CrewWorkerDto>> crewWorkerDtos = new HashMap<>();
        for (final InGameWorkerParamEntity worker : workers) {
            final String categoryName = worker.getWorker().getCategory().getName();

            final List<CrewWorkerDto> entities = crewWorkerDtos.get(categoryName);
            if (Objects.isNull(entities)) {
                final List<CrewWorkerDto> crewDtos = new ArrayList<>();
                crewDtos.add(new CrewWorkerDto(worker));
                crewWorkerDtos.put(Utilities.parseToCamelCase(categoryName), crewDtos);
            } else {
                entities.add(new CrewWorkerDto(worker));
            }
        }
        return CrewDataResDto.builder()
            .planes(planeDtos)
            .workers(crewWorkerDtos)
            .build();
    }

    @Override
    public SimpleMessageResDto addCrew(AddCrewReqDto reqDto, LoggedUser loggedUser) {
        final UserEntity userEntity = loggedUser.userEntity();

        final InGamePlaneParamEntity plane = inGamePlaneParamRepository
            .findByIdAndUser_Id(reqDto.getPlaneId(), userEntity.getId())
            .orElseThrow(() -> new PlaneNotExistOrNotBoughtException(reqDto.getPlaneId()));

        for (final Long workerId : reqDto.getCrew()) {
            if (!inGameWorkerParamRepository.existsByIdAndUser_Id(workerId, userEntity.getId())) {
                throw new WorkerNotExistOrNotBoughtException(reqDto.getCrew());
            }
        }
        final List<InGameWorkerParamEntity> workers = inGameWorkerParamRepository
            .findAllByIdInAndUser_Id(reqDto.getCrew(), userEntity.getId());

        if (workers.isEmpty()) {
            throw new WorkerNotExistOrNotBoughtException(reqDto.getCrew());
        }
        for (final InGameWorkerParamEntity worker : workers) {
            final InGameCrewEntity crew = new InGameCrewEntity(plane, worker);
            plane.persistInGameCrewEntity(crew);
            crew.setWorker(worker);
        }
        inGamePlaneParamRepository.save(plane);

        final String crewParsed = workers.stream()
            .map(w -> Utilities.parseWorkerFullName(w.getWorker()))
            .collect(Collectors.joining(", "));

        log.info("Crew for plane: {} was successfully added. Crew data: {}", plane.getPlane().getName(), crewParsed);
        return new SimpleMessageResDto(localeMessageService.getMessage(AppLocaleSet.ADD_NEW_CREW_RES));
    }
}
