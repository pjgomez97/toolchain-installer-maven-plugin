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

package org.cyanic.maven.plugins.toolchain.jbang;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.ToolchainPrivate;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.cyanic.maven.plugins.toolchain.xml.ToolchainXmlHelper.addJDKToToolchains;

public final class JBangHelper {

    private JBangHelper() {}

    public static ToolchainPrivate getJdkFromJbang(Log log, String version, String vendor) {
        Path userHome = Paths.get(System.getProperty("user.home"));

        Path jbangHome = userHome.resolve(".jbang");

        String majorVersion = resolveVersion(version);

        Path jdkHome = jbangHome.resolve("cache").resolve("jdks").resolve(majorVersion);

        if (!jdkHome.toFile().exists()) {
            Path jbangPath;

            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                jbangPath = jbangHome.resolve("bin").resolve("jbang.cmd");
            } else {
                jbangPath = jbangHome.resolve("bin").resolve("jbang");
            }

            if (!jbangPath.toFile().exists()) {
                return null;
            }

            String jbangCmd = jbangPath.toAbsolutePath().toString();

            try {
                Process process = new ProcessBuilder(jbangCmd, "jdk", "install", majorVersion).start();

                process.waitFor();

                log.info("JDK installed with JBang");
                log.info("Adding JDK to toolchains.xml");

                return addJDKToToolchains(jdkHome, version, vendor);
            } catch (Exception e) {
                log.error("Failed to find JDK from jbang", e);
            }
        }

        log.info("JDK not found in JBang");

        return null;
    }

    private static String resolveVersion(String version) {
        if (version.contains(".")) {
            if (version.startsWith("1.")) {
                return "8";
            } else {
                return version.substring(0, version.indexOf("."));
            }
        }

        return version;
    }
}
