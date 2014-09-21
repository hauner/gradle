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

package org.gradle.sonar.runner.tasks;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Joiner;
import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.internal.DefaultJavaForkOptions;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.sonar.runner.SonarRunnerExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Analyses one or more projects with the <a href="http://docs.codehaus.org/display/SONAR/Analyzing+with+Sonar+Runner">Sonar Runner</a>.
 * <p>
 * Can be used with or without the {@code "sonar-runner"} plugin.
 * If used together with the plugin, {@code sonarProperties} will be populated with defaults based on Gradle's object model and user-defined
 * values configured via {@link SonarRunnerExtension} and {@link org.gradle.sonar.runner.SonarRunnerRootExtension}.
 * If used without the plugin, all properties have to be configured manually.
 * <p>
 * For more information on how to configure the Sonar Runner, and on which properties are available, see the
 * <a href="http://docs.codehaus.org/display/SONAR/Analyzing+with+SonarQube+Runner">Sonar Runner documentation</a>.
 */
@Incubating
public class SonarRunner extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(SonarRunner.class);
    private static final String MAIN_CLASS_NAME = "org.sonar.runner.Main";
    private static final String SONAR_PROJECT_SETTINGS = "project.settings";
    public static final String SONAR_PROPERTIES_FILE = "sonar.properties";

    private JavaForkOptions forkOptions;
    private Map<String, Object> sonarProperties;

    @TaskAction
    public void run() {
        prepareExec().build().start().waitForFinish().assertNormalExitValue();
    }

    JavaExecHandleBuilder prepareExec() {
        Map<String, Object> properties = getSonarProperties();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Executing Sonar Runner with properties:\n[{}]", Joiner.on(", ").withKeyValueSeparator(": ").join(properties));
        }

        if (getUsePropertyFile ()) {
            properties = getSonarFileProperties();
        }

        JavaExecHandleBuilder javaExec = new JavaExecHandleBuilder(getFileResolver());
        getForkOptions().copyTo(javaExec);

        FileCollection sonarRunnerConfiguration = getProject().getConfigurations().getAt(SonarRunnerExtension.SONAR_RUNNER_CONFIGURATION_NAME);

        return javaExec
                .setClasspath(sonarRunnerConfiguration)
                .setMain(MAIN_CLASS_NAME)
                .systemProperties(properties);
    }

    /**
     * Options for the analysis process. Configured via {@link org.gradle.sonar.runner.SonarRunnerRootExtension#forkOptions}.
     */
    public JavaForkOptions getForkOptions() {
        if (forkOptions == null) {
            forkOptions = new DefaultJavaForkOptions(getFileResolver());
        }

        return forkOptions;
    }

    /**
     * The String key/value pairs to be passed to the Sonar Runner.
     *
     * {@code null} values are not permitted.
     */
    @Input
    public Map<String, Object> getSonarProperties() {
        if (sonarProperties == null) {
            sonarProperties = Maps.newLinkedHashMap();
        }

        return sonarProperties;
    }

    @Inject
    protected FileResolver getFileResolver() {
        throw new UnsupportedOperationException();
    }

    private Map<String, Object> getSonarFileProperties() {
        try {
            File sonarPropertiesFile = createSonarPropertiesFile (getSonarProperties());

            Map<String, Object> properties = new HashMap<String, Object> ();
            properties.put (SONAR_PROJECT_SETTINGS, sonarPropertiesFile.getAbsolutePath ());

            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException (e);
        }
    }

    private File createSonarPropertiesFile (Map<String, Object> properties) throws IOException {
        File propertiesFile = new File (getTemporaryDir (), SONAR_PROPERTIES_FILE);

        Properties props = new Properties ();
        props.putAll (properties);

        FileWriter writer = new FileWriter (propertiesFile);
        props.store (writer, "generated by sonar-runner plugin");
        writer.close ();

        return propertiesFile;
    }

    private boolean getUsePropertyFile() {
        return ((SonarRunnerExtension) getProject().getExtensions()
            .getByName (SonarRunnerExtension.SONAR_RUNNER_EXTENSION_NAME)).getUsePropertiesFile ();
    }
}
