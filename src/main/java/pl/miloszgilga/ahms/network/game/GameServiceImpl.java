package pl.miloszgilga.ahms.network.game;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.jmpsl.core.db.AbstractAuditableEntity;
import org.jmpsl.core.i18n.LocaleMessageService;
import org.jmpsl.security.user.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.miloszgilga.algorithms.BoostLevelDto;
import pl.miloszgilga.algorithms.GameAlgorithms;
import pl.miloszgilga.domain.in_game_crew.InGameCrewEntity;
import pl.miloszgilga.domain.in_game_plane_params.InGamePlaneParamEntity;
import pl.miloszgilga.domain.in_game_plane_params.InGamePlaneParamRepository;
import pl.miloszgilga.domain.in_game_worker_params.InGameWorkerParamEntity;
import pl.miloszgilga.domain.plane.PlaneEntity;
import pl.miloszgilga.domain.plane_route.PlaneRouteEntity;
import pl.miloszgilga.domain.plane_route.PlaneRouteRepository;
import pl.miloszgilga.domain.temp_stats.TempStatsEntity;
import pl.miloszgilga.domain.temp_stats.TempStatsRepository;
import pl.miloszgilga.domain.user.UserEntity;
import pl.miloszgilga.domain.user.UserRepository;
import pl.miloszgilga.dto.SimpleMessageResDto;
import pl.miloszgilga.i18n.AppLocaleSet;
import pl.miloszgilga.network.game.dto.CrewInFlightDto;
import pl.miloszgilga.network.game.dto.PlaneWithRoutesDto;
import pl.miloszgilga.network.game.dto.PlaneWithWorkersAndRouteDto;
import pl.miloszgilga.network.game.dto.RouteDto;
import pl.miloszgilga.network.game.reqdto.GenerateStatsReqDto;
import pl.miloszgilga.network.game.resdto.GeneratedSendPlaneStatsResDto;
import pl.miloszgilga.network.game.resdto.InFlightPlaneResDto;
import pl.miloszgilga.network.game.resdto.PlanesWithRoutesResDto;
import pl.miloszgilga.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.*;

import static pl.miloszgilga.exception.GameException.*;
import static pl.miloszgilga.exception.ShopException.AccountHasNotEnoughtMoneyException;

@Slf4j
@Service
@RequiredArgsConstructor
class GameServiceImpl implements GameService {
    private final SecurityUtils securityUtils;
    private final GameAlgorithms gameAlgorithms;
    private final LocaleMessageService messageService;

    private final UserRepository userRepository;
    private final TempStatsRepository tempStatsRepository;
    private final PlaneRouteRepository planeRouteRepository;
    private final InGamePlaneParamRepository inGamePlaneParamRepository;

    @Override
    public PlanesWithRoutesResDto getPlanesWithRoutes(AuthUser user) {
        final UserEntity userEntity = securityUtils.getLoggedUser(user);

        final List<PlaneWithRoutesDto> planeWithRoutesDtos = new ArrayList<>();
        final List<InGamePlaneParamEntity> planes = inGamePlaneParamRepository.findAllInJoiningTable(userEntity.getId());

        for (final InGamePlaneParamEntity plane : planes) {
            final List<PlaneRouteEntity> routes = planeRouteRepository.findAllByPlane_Id(plane.getId());
            if (routes.size() != 3) {
                throw new NotEnoughtRoutesException(plane);
            }
            final PlaneEntity planeEntity = plane.getPlane();
            final List<RouteDto> routeDtos = new ArrayList<>();

            final Set<Integer> randomValues = new HashSet<>();
            while (randomValues.size() < 3) {
                int randomNumber = RandomUtils.nextInt(1, planeEntity.getMaxHours() + 1);
                randomValues.add(randomNumber);
            }
            int i = 0;
            for (final PlaneRouteEntity routeEntity : routes) {
                if (userEntity.getRoutesBlocked() && !Objects.isNull(routeEntity.getRouteHours())) {
                    routeDtos.add(new RouteDto(routeEntity.getId(), routeEntity.getRouteHours()));
                } else {
                    final int generatedHours = new ArrayList<>(randomValues).get(i++);
                    routeEntity.setRouteHours(generatedHours);
                    routeDtos.add(new RouteDto(routeEntity.getId(), generatedHours));
                    log.info("Successfully generated new route [{}] hours: {}", routeEntity, generatedHours);
                }
            }
            planeWithRoutesDtos.add(PlaneWithRoutesDto.builder()
                .planeId(plane.getId())
                .planeName(planeEntity.getName())
                .planeCategory(planeEntity.getCategory().getName())
                .level(plane.getUpgrade())
                .routes(routeDtos)
                .crew(plane.getCrew().stream().map(AbstractAuditableEntity::getId).toList())
                .build());
        }
        userEntity.setRoutesBlocked(true);
        userRepository.save(userEntity);

        final List<String> parsedCategories = planes.stream()
            .map(p -> p.getPlane().getCategory().getName())
            .distinct()
            .toList();

        return PlanesWithRoutesResDto.builder()
            .planesCategories(parsedCategories)
            .planes(planeWithRoutesDtos)
            .build();
    }

    @Override
    public List<InFlightPlaneResDto> getInFlightPlanes(AuthUser user) {
        final UserEntity userEntity = securityUtils.getLoggedUser(user);
        final List<InFlightPlaneResDto> inFlightPlaneResDtos = new ArrayList<>();

        final List<InGamePlaneParamEntity> planes = inGamePlaneParamRepository
            .findAllByUser_IdAndAvailableIsNotNull(userEntity.getId());

        for (final InGamePlaneParamEntity plane : planes) {
            final List<CrewInFlightDto> workers = plane.getCrew().stream()
                .map(c -> new CrewInFlightDto(c.getWorker())).toList();
            inFlightPlaneResDtos.add(new InFlightPlaneResDto(plane, workers));
        }
        return inFlightPlaneResDtos;
    }

    @Override
    @Transactional
    public GeneratedSendPlaneStatsResDto generateStats(GenerateStatsReqDto reqDto, AuthUser user) {
        final UserEntity userEntity = securityUtils.getLoggedUser(user);
        final PlaneWithWorkersAndRouteDto details = getPlaneDetails(userEntity, reqDto.getPlaneId(), reqDto.getRouteId());
        if (Objects.isNull(details.route().getRouteHours())) {
            throw new RouteNotFoundException(details.route().getId());
        }
        final LocalDateTime arrival = LocalDateTime.now().plusHours(details.route().getRouteHours());
        final int exp = gameAlgorithms.generateExp(details, userEntity.getLevel());
        final BoostLevelDto boostLevelDto = gameAlgorithms.checkIfBoostLevel(exp, userEntity);
        final int prize = gameAlgorithms.generatePrize(details, userEntity.getLevel());
        final int flightCosts = gameAlgorithms.generateFlightCosts(details, prize);

        if (userEntity.getMoney() - flightCosts < 0) {
            throw new AccountHasNotEnoughtMoneyException(userEntity.getMoney(), flightCosts);
        }
        final TempStatsEntity tempStatsEntity = TempStatsEntity.builder()
            .selectedRoute(details.route().getId())
            .arrivalTime(arrival)
            .upgradedLevel(boostLevelDto.nextLevel())
            .addedExp(exp)
            .flightCosts(flightCosts)
            .build();
        tempStatsEntity.setPlane(details.plane());

        tempStatsRepository.deleteByPlane_IdAndPlane_User_Id(reqDto.getPlaneId(), userEntity.getId());
        tempStatsRepository.save(tempStatsEntity);

        inGamePlaneParamRepository.save(details.plane());

        log.info("Successfully generated stats for plane with id: {} on route with id: {}",
            reqDto.getPlaneId(), reqDto.getRouteId());

        return GeneratedSendPlaneStatsResDto.builder()
            .currentLevel(userEntity.getLevel())
            .fromLevel(boostLevelDto.fromLevel())
            .toLevel(boostLevelDto.toLevel())
            .nextLevel(boostLevelDto.nextLevel())
            .currentExp(userEntity.getExp())
            .maxLevels(gameAlgorithms.getMaxLevel())
            .isUpgraded(boostLevelDto.isBoosted())
            .addedExp(exp)
            .prize(prize)
            .totalCost(flightCosts)
            .accountDeposit(Math.max(userEntity.getMoney() - flightCosts, 0))
            .arrival(arrival)
            .build();
    }

    @Override
    @Transactional
    public SimpleMessageResDto sendPlane(Long planeId, AuthUser user) {
        final UserEntity userEntity = securityUtils.getLoggedUser(user);
        final TempStatsEntity tempStats = tempStatsRepository
            .findByPlane_IdAndPlane_User_Id(planeId, userEntity.getId())
            .orElseThrow(() -> new NonExistingTempStatsException(planeId));

        final PlaneWithWorkersAndRouteDto details = getPlaneDetails(userEntity, planeId, tempStats.getSelectedRoute());
        for (final InGameWorkerParamEntity workerParam : details.workers()) {
            workerParam.setAvailable(tempStats.getArrivalTime());
        }
        details.plane().setAvailable(tempStats.getArrivalTime());
        gameAlgorithms.generateDamage(details.plane());

        userEntity.setLevel(tempStats.getUpgradedLevel());
        userEntity.setExp(userEntity.getExp() + tempStats.getAddedExp());
        userEntity.setMoney(userEntity.getMoney() - tempStats.getFlightCosts());

        userRepository.save(userEntity);
        tempStatsRepository.delete(tempStats);

        log.info("Successfully send plane with id: {} on route with id: {}", planeId, tempStats.getSelectedRoute());
        return new SimpleMessageResDto(messageService.getMessage(AppLocaleSet.SEND_PLANE_ON_ROUTE_RES));
    }

    @Override
    @Transactional
    public SimpleMessageResDto revertPlane(Long planeId, AuthUser user) {
        final UserEntity userEntity = securityUtils.getLoggedUser(user);
        tempStatsRepository.deleteByPlane_IdAndPlane_User_Id(planeId, userEntity.getId());

        log.info("Successfully delete temp stats with plane: {}", planeId);
        return new SimpleMessageResDto(messageService.getMessage(AppLocaleSet.REVERT_PLANE_ON_ROUTE_RES));
    }

    private PlaneWithWorkersAndRouteDto getPlaneDetails(UserEntity user, Long planeId, Long routeId) {
        final InGamePlaneParamEntity plane = inGamePlaneParamRepository
            .findByPlaneIdInJoiningTable(user.getId(), planeId)
            .orElseThrow(() -> new PlaneNotExistException(planeId));
        if (!Objects.isNull(plane.getAvailable())) {
            throw new LockedPlaneException(planeId);
        }
        final PlaneRouteEntity planeRouteEntity = plane.getPlaneRouteEntities().stream()
            .filter(r -> r.getId().equals(routeId))
            .findFirst()
            .orElseThrow(() -> new RouteNotFoundException(routeId));

        final List<InGameWorkerParamEntity> workers = plane.getCrew().stream()
            .map(InGameCrewEntity::getWorker).toList();

        return PlaneWithWorkersAndRouteDto.builder()
            .plane(plane)
            .route(planeRouteEntity)
            .workers(workers)
            .build();
    }
}
