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
import org.cyanic.maven.plugins.toolchain.xml.ToolchainXmlHelper;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SdkmanHelperTest {

    private static final String JDK_VERSION = "17";

    private static final String JDK_VENDOR = "";

    private static final String ORIGINAL_USER_HOME = System.getProperty("user.home");

    private static final String TEST_USER_HOME = "/test/home";

    @Mock
    private Log log;

    @BeforeAll
    static void setUp() {
        System.setProperty("user.home", TEST_USER_HOME);
    }

    @AfterAll
    static void tearDown() {
        System.setProperty("user.home", ORIGINAL_USER_HOME);
    }

    @Test
    void testGetJdkFromSdkman_AndJdkExists_ShouldReturnToolchain() {
        Path userHomeMock = mock(Path.class);
        Path sdkmanJavaMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);

        File sdkmanJavaFileMock = mock(File.class);
        File jdkHomeFileMock = mock(File.class);

        when(userHomeMock.resolve(".sdkman")).thenReturn(sdkmanJavaMock);

        when(sdkmanJavaMock.resolve("candidates")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve("java")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(sdkmanJavaMock.toFile()).thenReturn(sdkmanJavaFileMock);
        when(sdkmanJavaFileMock.exists()).thenReturn(true);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(true);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
             MockedStatic<ToolchainXmlHelper> toolchainXmlHelperMock = mockStatic(ToolchainXmlHelper.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            ToolchainPrivate toolchainPrivateMock = mock(ToolchainPrivate.class);
            toolchainXmlHelperMock.when(() -> ToolchainXmlHelper.addJDKToToolchains(jdkHomeMock, JDK_VERSION, JDK_VENDOR)).thenReturn(toolchainPrivateMock);

            ToolchainPrivate toolchain = SdkmanHelper.getJdkFromSdkman(log, JDK_VERSION);

            assertEquals(toolchainPrivateMock, toolchain);
            verify(log).info("Found JDK in Sdkman: " + jdkHomeMock);
            verify(log).info("Adding JDK to toolchains.xml");
        }
    }

    @Test
    void testGetJdkFromSdkman_WhenSdkmanJavaDirDoesntExist_ShouldReturnNull() {
        Path userHomeMock = mock(Path.class);
        Path sdkmanJavaMock = mock(Path.class);

        File sdkmanJavaFileMock = mock(File.class);

        when(userHomeMock.resolve(".sdkman")).thenReturn(sdkmanJavaMock);

        when(sdkmanJavaMock.resolve("candidates")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve("java")).thenReturn(sdkmanJavaMock);

        when(sdkmanJavaMock.toFile()).thenReturn(sdkmanJavaFileMock);
        when(sdkmanJavaFileMock.exists()).thenReturn(false);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            ToolchainPrivate toolchain = SdkmanHelper.getJdkFromSdkman(log, JDK_VERSION);

            assertNull(toolchain);
        }
    }

    @Test
    void testGetJdkFromSdkman_WhenJdkHomeDoesntExist_ShouldReturnNull() {
        Path userHomeMock = mock(Path.class);
        Path sdkmanJavaMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);

        File sdkmanJavaFileMock = mock(File.class);
        File jdkHomeFileMock = mock(File.class);

        when(userHomeMock.resolve(".sdkman")).thenReturn(sdkmanJavaMock);

        when(sdkmanJavaMock.resolve("candidates")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve("java")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(sdkmanJavaMock.toFile()).thenReturn(sdkmanJavaFileMock);
        when(sdkmanJavaFileMock.exists()).thenReturn(true);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            ToolchainPrivate toolchain = SdkmanHelper.getJdkFromSdkman(log, JDK_VERSION);

            assertNull(toolchain);
            verify(log).info("JDK not found in Sdkman");
        }
    }

    @Test
    void testGetJdkFromSdkman_WhenErrorAddingToolchainToXml_ShouldReturnNull() {
        Path userHomeMock = mock(Path.class);
        Path sdkmanJavaMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);

        File sdkmanJavaFileMock = mock(File.class);

        when(userHomeMock.resolve(".sdkman")).thenReturn(sdkmanJavaMock);

        when(sdkmanJavaMock.resolve("candidates")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve("java")).thenReturn(sdkmanJavaMock);
        when(sdkmanJavaMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(sdkmanJavaMock.toFile()).thenReturn(sdkmanJavaFileMock);
        when(sdkmanJavaFileMock.exists()).thenReturn(true);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class);
             MockedStatic<ToolchainXmlHelper> toolchainXmlHelperMock = mockStatic(ToolchainXmlHelper.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            toolchainXmlHelperMock.when(() -> ToolchainXmlHelper.addJDKToToolchains(jdkHomeMock, JDK_VERSION, JDK_VENDOR)).thenThrow(new Exception());

            ToolchainPrivate toolchain = SdkmanHelper.getJdkFromSdkman(log, JDK_VERSION);

            assertNull(toolchain);
            verify(log).error(eq("Failed to find JDK from sdkman, please use `sdk install " + JDK_VERSION + "` to install JDK"), any(Exception.class));
        }
    }
} 