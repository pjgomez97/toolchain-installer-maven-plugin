/*
 * Copyright 2025 pjgomez97
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanic.maven.plugins.toolchain.sdkman;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.ToolchainPrivate;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.cyanic.maven.plugins.toolchain.xml.ToolchainXmlHelper.addJDKToToolchains;

public final class SdkmanHelper {

    private SdkmanHelper() {}

    public static ToolchainPrivate getJdkFromSdkman(Log log, String version) {
        Path userHome = Paths.get(System.getProperty("user.home"));

        Path sdkmanJavaDir = userHome.resolve(".sdkman").resolve("candidates").resolve("java");

        if (!sdkmanJavaDir.toFile().exists()) {
            return null;
        }

        try {
            Path jdkHome = sdkmanJavaDir.resolve(version);

            if (jdkHome.toFile().exists()) {
                log.info("Found JDK in Sdkman: " + jdkHome);
                log.info("Adding JDK to toolchains.xml");

                return addJDKToToolchains(jdkHome, version, "");
            }

        } catch (Exception e) {
            log.error("Failed to find JDK from sdkman, please use `sdk install " + version + "` to install JDK", e);
        }

        log.info("JDK not found in Sdkman");

        return null;
    }
}
