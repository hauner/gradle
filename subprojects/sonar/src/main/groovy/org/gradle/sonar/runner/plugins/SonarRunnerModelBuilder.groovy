/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.sonar.runner.plugins

import org.gradle.api.Project
import org.gradle.sonar.runner.SonarRunnerExtension
import org.gradle.tooling.provider.model.ToolingModelBuilder

class SonarRunnerModelBuilder implements ToolingModelBuilder {

    @Override
    boolean canBuild (String modelName) {
        modelName == SonarRunnerModel.name
    }

    @Override
    Object buildAll (String modelName, Project project) {
        Map props = properties (project).sonarProperties

        new SonarRunnerModelDefault(
            props['sonar.projectName'].toString (),
            props['sonar.projectKey'].toString ())
    }

    private static Map properties (Project project) {
        project.tasks.getByName (SonarRunnerExtension.SONAR_RUNNER_TASK_NAME).properties
    }
}
