/*
 * Copyright (c) 2023 by MILOSZ GILGA <http://miloszgilga.pl>
 *
 * File name: CrewWorkerDto.java
 * Last modified: 6/26/23, 2:52 PM
 * Project name: air-hub-master-server
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     <http://www.apache.org/license/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the license.
 */

package pl.miloszgilga.network.crew.dto;

import pl.miloszgilga.utils.Utilities;
import pl.miloszgilga.domain.in_game_worker_params.InGameWorkerParamEntity;

import java.util.Objects;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

public record CrewWorkerDto(
    long id,
    String fullName,
    boolean available,
    int experience,
    int cooperation,
    int rebelliousness,
    int skills
) {
    public CrewWorkerDto(InGameWorkerParamEntity worker) {
        this(worker.getId(), Utilities.parseWorkerFullName(worker.getWorker()), Objects.isNull(worker.getAvailable()),
            worker.getExperience(), worker.getCooperation(), worker.getRebelliousness(), worker.getSkills());
    }
}
