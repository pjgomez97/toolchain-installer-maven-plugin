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

package org.cyanic.maven.plugins.toolchain;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.cyanic.maven.plugins.toolchain.config.ToolchainConfig;
import org.cyanic.maven.plugins.toolchain.foojay.FoojayHelper;
import org.cyanic.maven.plugins.toolchain.jbang.JBangHelper;
import org.cyanic.maven.plugins.toolchain.sdkman.SdkmanHelper;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
import org.apache.maven.toolchain.MisconfiguredToolchainException;
import org.apache.maven.toolchain.ToolchainManagerPrivate;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolchainInstallerMojoTest {

    @Mock
    private ToolchainManagerPrivate toolchainManagerPrivate;

    @Mock
    private MavenSession session;

    @Mock
    private ToolchainConfig toolchains;

    @Mock
    private Log log;

    private ToolchainInstallerMojo mojo;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new ToolchainInstallerMojo();

        setField(mojo, "toolchainManagerPrivate", toolchainManagerPrivate);
        setField(mojo, "session", session);
        setField(mojo, "toolchains", toolchains);

        System.setProperty("toolchain.installer.skip", "false");
    }

    @Test
    void testExecute_WhenSkipIsTrue_ShouldSkipExecution() throws Exception {
        setField(mojo, "skip", true);

        mojo.execute();

        verify(log, never()).info(anyString());
        verify(toolchainManagerPrivate, never()).getToolchainsForType(anyString(), any());
    }

    @Test
    void testExecute_WhenSkipPropertyIsSet_ShouldSkipExecution() throws MojoExecutionException, MojoFailureException, MisconfiguredToolchainException {
        System.setProperty("toolchain.installer.skip", "true");

        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);

        lenient().when(toolchains.getToolchains()).thenReturn(toolchainMap);
        lenient().when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[]{mockToolchain});
        lenient().when(mockToolchain.getType()).thenReturn("jdk");
        lenient().when(mockToolchain.matchesRequirements(jdkParams)).thenReturn(true);

        mojo.execute();

        verify(log, never()).info(anyString());
        verify(toolchainManagerPrivate, never()).getToolchainsForType(anyString(), any());
    }

    @Test
    void testExecute_WhenAllToolchainsFound_ShouldSucceed() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[]{mockToolchain});
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.matchesRequirements(jdkParams)).thenReturn(true);

        mojo.execute();

        verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        verify(log, never()).error(anyString());
    }

    @Test
    void testExecute_WhenToolchainNotFound_ShouldThrowMojoFailureException() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        Settings mockSettings = mock(Settings.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session))
                .thenReturn(new ToolchainPrivate[0]);
        when(session.getSettings()).thenReturn(mockSettings);

        try (MockedStatic<FoojayHelper> foojayMock = mockStatic(FoojayHelper.class)) {
            foojayMock.when(() -> FoojayHelper.getJdkFromFoojay(any(), any(), eq("17"), eq("oracle_open_jdk"))).thenReturn(null);

            MojoFailureException exception = assertThrows(MojoFailureException.class, () -> mojo.execute());
            assertTrue(exception.getMessage().contains("Cannot find matching toolchain definitions"));
            assertTrue(exception.getMessage().contains("jdk"));
        }
    }

    @Test
    void testExecute_WhenTestJdkType_ShouldMapToJdkType() throws Exception {
        Map<String, String> testJdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("testJdk", testJdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[]{mockToolchain});
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.matchesRequirements(testJdkParams)).thenReturn(true);

        mojo.execute();

        verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
    }

    @Test
    void testExecute_WhenJdkNotFound_ShouldTrySdkman() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);
        MavenExecutionRequest mockRequest = mock(MavenExecutionRequest.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session))
                .thenReturn(new ToolchainPrivate[0]);
        when(session.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getToolchains()).thenReturn(new HashMap<>());

        try (MockedStatic<SdkmanHelper> sdkmanMock = mockStatic(SdkmanHelper.class)) {
            sdkmanMock.when(() -> SdkmanHelper.getJdkFromSdkman(any(), eq("17"))).thenReturn(mockToolchain);

            mojo.execute();

            verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        }
    }

    @Test
    void testExecute_WhenJdkNotFoundAndSdkmanFails_ShouldTryJbang() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);
        MavenExecutionRequest mockRequest = mock(MavenExecutionRequest.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[0]);
        when(session.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getToolchains()).thenReturn(new HashMap<>());

        try (MockedStatic<SdkmanHelper> sdkmanMock = mockStatic(SdkmanHelper.class);
             MockedStatic<JBangHelper> jbangMock = mockStatic(JBangHelper.class)) {
            
            sdkmanMock.when(() -> SdkmanHelper.getJdkFromSdkman(any(), eq("17"))).thenReturn(null);
            jbangMock.when(() -> JBangHelper.getJdkFromJbang(any(), eq("17"), eq("oracle_open_jdk"))).thenReturn(mockToolchain);

            mojo.execute();

            verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        }
    }

    @Test
    void testExecute_WhenJdkNotFoundAndSdkmanAndJbangFail_ShouldTryFoojay() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);
        Settings mockSettings = mock(Settings.class);
        Proxy mockProxy = mock(Proxy.class);
        MavenExecutionRequest mockRequest = mock(MavenExecutionRequest.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session))
                .thenReturn(new ToolchainPrivate[0]);
        when(session.getSettings()).thenReturn(mockSettings);
        when(mockSettings.getActiveProxy()).thenReturn(mockProxy);
        when(session.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getToolchains()).thenReturn(new HashMap<>());

        try (MockedStatic<SdkmanHelper> sdkmanMock = mockStatic(SdkmanHelper.class);
             MockedStatic<JBangHelper> jbangMock = mockStatic(JBangHelper.class);
             MockedStatic<FoojayHelper> foojayMock = mockStatic(FoojayHelper.class)) {
            
            sdkmanMock.when(() -> SdkmanHelper.getJdkFromSdkman(any(), eq("17"))).thenReturn(null);
            jbangMock.when(() -> JBangHelper.getJdkFromJbang(any(), eq("17"), eq("oracle_open_jdk"))).thenReturn(null);
            foojayMock.when(() -> FoojayHelper.getJdkFromFoojay(any(), eq(mockProxy), eq("17"), eq("oracle_open_jdk"))).thenReturn(mockToolchain);

            mojo.execute();

            verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        }
    }

    @Test
    void testExecute_WhenVendorSpecified_ShouldUseSpecifiedVendor() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17", "vendor", "openjdk");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);
        Settings mockSettings = mock(Settings.class);
        Proxy mockProxy = mock(Proxy.class);
        MavenExecutionRequest mockRequest = mock(MavenExecutionRequest.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session))
                .thenReturn(new ToolchainPrivate[0]);
        when(session.getSettings()).thenReturn(mockSettings);
        when(mockSettings.getActiveProxy()).thenReturn(mockProxy);
        when(session.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getToolchains()).thenReturn(new HashMap<>());

        try (MockedStatic<FoojayHelper> foojayMock = mockStatic(FoojayHelper.class)) {
            foojayMock.when(() -> FoojayHelper.getJdkFromFoojay(any(), eq(mockProxy), eq("17"), eq("openjdk"))).thenReturn(mockToolchain);

            mojo.execute();

            verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        }
    }

    @Test
    void testExecute_WhenMisconfiguredToolchainException_ShouldThrowMojoExecutionException() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17", "vendor", "openjdk");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenThrow(new MisconfiguredToolchainException("Test exception"));

        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute());

        assertTrue(exception.getMessage().contains("Misconfigured toolchains"));
    }

    @Test
    void testExecute_WhenNonJdkToolchainType_ShouldNotTryDownloaders() throws Exception {
        mojo.execute();

        verify(toolchainManagerPrivate, never()).storeToolchainToBuildContext(any(), any());
    }

    @Test
    void testGetToolchainRequirementAsString_ShouldBeFormattedCorrectly() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17", "vendor", "openjdk");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);

        Settings mockSettings = mock(Settings.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchains.getParams("jdk")).thenReturn(jdkParams);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[0]);
        when(session.getSettings()).thenReturn(mockSettings);

        try (MockedStatic<FoojayHelper> foojayMock = mockStatic(FoojayHelper.class)) {
            foojayMock.when(() -> FoojayHelper.getJdkFromFoojay(any(), any(), eq("17"), eq("openjdk"))).thenReturn(null);

            MojoFailureException exception = assertThrows(MojoFailureException.class, () -> mojo.execute());
            assertTrue(exception.getMessage().contains("jdk ["));
            assertTrue(exception.getMessage().contains("version = '17'"));
            assertTrue(exception.getMessage().contains("vendor = 'openjdk'"));
        }
    }

    @Test
    void testExecute_WhenMultipleToolchainTypes_ShouldProcessAll() throws Exception {
        Map<String, String> jdkParams = Map.of("version", "17", "vendor", "openjdk");
        Map<String, String> testJdkParams = Map.of("version", "21", "vendor", "zulu");
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams, "testJdk", testJdkParams);

        ToolchainPrivate mockToolchain = mock(ToolchainPrivate.class);
        ToolchainPrivate mockTestToolchain = mock(ToolchainPrivate.class);

        when(toolchains.getToolchains()).thenReturn(toolchainMap);
        when(toolchainManagerPrivate.getToolchainsForType("jdk", session)).thenReturn(new ToolchainPrivate[]{mockToolchain, mockTestToolchain});
        when(mockToolchain.getType()).thenReturn("jdk");
        when(mockTestToolchain.getType()).thenReturn("jdk");
        when(mockToolchain.matchesRequirements(jdkParams)).thenReturn(true);
        when(mockToolchain.matchesRequirements(testJdkParams)).thenReturn(false);
        when(mockTestToolchain.matchesRequirements(testJdkParams)).thenReturn(true);

        mojo.execute();

        verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockToolchain, session);
        verify(toolchainManagerPrivate).storeToolchainToBuildContext(mockTestToolchain, session);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
} 