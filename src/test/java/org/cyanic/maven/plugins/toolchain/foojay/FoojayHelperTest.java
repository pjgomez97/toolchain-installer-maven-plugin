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
import org.cyanic.maven.plugins.toolchain.xml.ToolchainXmlHelper;
import org.apache.maven.settings.Proxy;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FoojayHelperTest {

    private static final String JDK_VERSION = "17";

    private static final String JDK_VENDOR = "openjdk";

    @Mock
    private Log log;

    @Mock
    private Proxy proxySettings;

    @Test
    void testGetJdkFromFoojay_AndJdkExists_ShouldReturnToolchain() {
        Path jdkHomeMock = mock(Path.class);

        try (MockedStatic<FoojayService> foojayServiceMock = mockStatic(FoojayService.class);
             MockedStatic<ToolchainXmlHelper> toolchainXmlHelperMock = mockStatic(ToolchainXmlHelper.class)) {
            foojayServiceMock.when(() -> FoojayService.downloadAndExtractJdk(log, proxySettings, JDK_VERSION, JDK_VENDOR)).thenReturn(jdkHomeMock);

            ToolchainPrivate toolchainPrivateMock = mock(ToolchainPrivate.class);
            toolchainXmlHelperMock.when(() -> ToolchainXmlHelper.addJDKToToolchains(jdkHomeMock, JDK_VERSION, JDK_VENDOR)).thenReturn(toolchainPrivateMock);

            ToolchainPrivate toolchain = FoojayHelper.getJdkFromFoojay(log, proxySettings, JDK_VERSION, JDK_VENDOR);

            assertEquals(toolchainPrivateMock, toolchain);
            verify(log).info("Adding JDK to toolchains.xml");
        }
    }

    @Test
    void testGetJdkFromFoojay_AndJdkDoesntExist_ShouldReturnNull() {
        try (MockedStatic<FoojayService> foojayServiceMock = mockStatic(FoojayService.class)) {
            foojayServiceMock.when(() -> FoojayService.downloadAndExtractJdk(log, proxySettings, JDK_VERSION, JDK_VENDOR)).thenReturn(null);

            ToolchainPrivate toolchain = FoojayHelper.getJdkFromFoojay(log, proxySettings, JDK_VERSION, JDK_VENDOR);

            assertNull(toolchain);
            verify(log).info("Couldn't download JDK with Foojay");
        }
    }

    @Test
    void testGetJdkFromFoojay_AndException_ShouldLogError() {
        try (MockedStatic<FoojayService> foojayServiceMock = mockStatic(FoojayService.class)) {
            foojayServiceMock.when(() -> FoojayService.downloadAndExtractJdk(log, proxySettings, JDK_VERSION, JDK_VENDOR)).thenThrow(new Exception());

            ToolchainPrivate toolchain = FoojayHelper.getJdkFromFoojay(log, proxySettings, JDK_VERSION, JDK_VENDOR);

            assertNull(toolchain);
            verify(log).error(eq("Failed to download and install JDK"), any(Exception.class));
            verify(log).info("Couldn't download JDK with Foojay");
        }
    }
}
