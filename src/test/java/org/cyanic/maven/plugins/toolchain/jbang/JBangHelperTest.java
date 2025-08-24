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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JBangHelperTest {

    private static final String JDK_VERSION = "17";

    private static final String JDK_VENDOR = "openjdk";

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
    void testGetJdkFromJbang_WhenVersionWithoutDots_ShouldReturnOriginalVersion() {
        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);
        Path jbangPathMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);
        File jbangPathFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        when(jbangHomeMock.resolve("bin")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jbang")).thenReturn(jbangPathMock);
        when(jbangPathMock.toFile()).thenReturn(jbangPathFileMock);
        when(jbangPathFileMock.exists()).thenReturn(true);
        when(jbangPathMock.toAbsolutePath()).thenReturn(jbangPathMock);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            JBangHelper.getJdkFromJbang(log, JDK_VERSION, JDK_VENDOR);

            verify(jbangHomeMock).resolve(JDK_VERSION);
        }
    }

    @Test
    void testGetJdkFromJbang_WhenVersionOneDot_ShouldReturnVersionFromDot() {
        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);
        Path jbangPathMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);
        File jbangPathFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("8")).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        when(jbangHomeMock.resolve("bin")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jbang")).thenReturn(jbangPathMock);
        when(jbangPathMock.toFile()).thenReturn(jbangPathFileMock);
        when(jbangPathFileMock.exists()).thenReturn(true);
        when(jbangPathMock.toAbsolutePath()).thenReturn(jbangPathMock);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            JBangHelper.getJdkFromJbang(log, "1.8", JDK_VENDOR);

            verify(jbangHomeMock).resolve("8");
        }
    }

    @Test
    void testGetJdkFromJbang_WhenVersionWithDot_ShouldReturnVersionBeforeDot() {
        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);
        Path jbangPathMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);
        File jbangPathFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        when(jbangHomeMock.resolve("bin")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jbang")).thenReturn(jbangPathMock);
        when(jbangPathMock.toFile()).thenReturn(jbangPathFileMock);
        when(jbangPathFileMock.exists()).thenReturn(true);
        when(jbangPathMock.toAbsolutePath()).thenReturn(jbangPathMock);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            JBangHelper.getJdkFromJbang(log, "17.0.1", JDK_VENDOR);

            verify(jbangHomeMock).resolve(JDK_VERSION);
        }
    }

    @Test
    void testGetJdkFromJbang_WhenJdkHomeExists_ShouldReturnNull() {
        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(true);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            ToolchainPrivate toolchain = JBangHelper.getJdkFromJbang(log, JDK_VERSION, JDK_VENDOR);

            assertNull(toolchain);
            verify(log).info("JDK not found in JBang");
        }
    }

    @Test
    void testGetJdkFromJbang_WhenWindowsOs_ShouldUseCmd() {
        String originalOsName = System.getProperty("os.name");
        System.setProperty("os.name", "windows");

        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);
        Path jbangPathMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);
        File jbangPathFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        when(jbangHomeMock.resolve("bin")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jbang.cmd")).thenReturn(jbangPathMock);
        when(jbangPathMock.toFile()).thenReturn(jbangPathFileMock);
        when(jbangPathFileMock.exists()).thenReturn(true);
        when(jbangPathMock.toAbsolutePath()).thenReturn(jbangPathMock);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            JBangHelper.getJdkFromJbang(log, JDK_VERSION, JDK_VENDOR);

            verify(jbangHomeMock).resolve("jbang.cmd");
        }

        System.setProperty("os.name", originalOsName);
    }

    @Test
    void testGetJdkFromJbang_WhenJBangPathDoesntExist_ShouldReturnNull() {
        Path userHomeMock = mock(Path.class);
        Path jbangHomeMock = mock(Path.class);
        Path jdkHomeMock = mock(Path.class);
        Path jbangPathMock = mock(Path.class);

        File jdkHomeFileMock = mock(File.class);
        File jbangPathFileMock = mock(File.class);

        when(userHomeMock.resolve(".jbang")).thenReturn(jbangHomeMock);

        when(jbangHomeMock.resolve("cache")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jdks")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve(JDK_VERSION)).thenReturn(jdkHomeMock);

        when(jdkHomeMock.toFile()).thenReturn(jdkHomeFileMock);
        when(jdkHomeFileMock.exists()).thenReturn(false);

        when(jbangHomeMock.resolve("bin")).thenReturn(jbangHomeMock);
        when(jbangHomeMock.resolve("jbang")).thenReturn(jbangPathMock);
        when(jbangPathMock.toFile()).thenReturn(jbangPathFileMock);
        when(jbangPathFileMock.exists()).thenReturn(false);

        try (MockedStatic<Paths> pathsMock = mockStatic(Paths.class)) {
            pathsMock.when(() -> Paths.get(TEST_USER_HOME)).thenReturn(userHomeMock);

            ToolchainPrivate toolchain = JBangHelper.getJdkFromJbang(log, JDK_VERSION, JDK_VENDOR);

            assertNull(toolchain);
        }
    }
} 