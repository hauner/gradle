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

package org.gradle.jvm.platform;

import org.gradle.api.Incubating;
import org.gradle.api.JavaVersion;
import org.gradle.platform.base.Platform;

/**
 * Defines and configures a Java SE runtime environment, consisting of a JVM runtime and a set of class libraries.
 *
 * <pre autoTested="true">>
 * apply plugin: 'jvm-component'
 * apply plugin: 'java-lang'
 *
 * jvm {
 *   libraries {
 *     myLib {
 *       targetPlatform "java6"
 *     }
 *   }
 * }
 * </pre>
 */
@Incubating
public interface JavaPlatform extends Platform {
    JavaVersion getTargetCompatibility();
    void setTargetCompatibility(JavaVersion targetCompatibility);
}