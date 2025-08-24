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

package org.cyanic.maven.plugins.toolchain.foojay;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Proxy;
import org.apache.maven.toolchain.ToolchainPrivate;

import java.nio.file.Path;

import static org.cyanic.maven.plugins.toolchain.xml.ToolchainXmlHelper.addJDKToToolchains;

public final class FoojayHelper {

    private FoojayHelper() {}

    public static ToolchainPrivate getJdkFromFoojay(Log log, Proxy proxySettings, String version, String vendor) {
        try {
            Path jdkHome = FoojayService.downloadAndExtractJdk(log, proxySettings, version, vendor);

            if (jdkHome != null) {
                log.info("Adding JDK to toolchains.xml");

                return addJDKToToolchains(jdkHome, version, vendor);
            }
        } catch (Exception e) {
            log.error("Failed to download and install JDK", e);
        }

        log.info("Couldn't download JDK with Foojay");

        return null;
    }
}
