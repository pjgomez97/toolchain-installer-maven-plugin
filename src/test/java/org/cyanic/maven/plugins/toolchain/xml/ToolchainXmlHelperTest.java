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

package org.cyanic.maven.plugins.toolchain.xml;

import org.apache.commons.io.FileUtils;
import org.apache.maven.toolchain.ToolchainPrivate;
import org.apache.maven.toolchain.java.DefaultJavaToolChain;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolchainXmlHelperTest {

    private static final String JDK_VERSION = "17";
    private static final String JDK_VENDOR = "openjdk";

    private static final String ORIGINAL_USER_HOME = System.getProperty("user.home");
    private static final String TEST_USER_HOME = ORIGINAL_USER_HOME + "/test";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setUp() {
        System.setProperty("user.home", TEST_USER_HOME);
    }

    @AfterAll
    static void tearDown() throws IOException {
        System.setProperty("user.home", ORIGINAL_USER_HOME);

        FileUtils.deleteDirectory(new File(TEST_USER_HOME));
    }

    @BeforeEach
    void setUpTest() {
        File m2Dir = new File(new File(TEST_USER_HOME), ".m2");

        if (!m2Dir.exists()) {
            m2Dir.mkdirs();
        }

        File toolchainsFile = new File(new File(TEST_USER_HOME), ".m2/toolchains.xml");

        if (toolchainsFile.exists()) {
            toolchainsFile.delete();
        }
    }

    @Test
    void testAddJDKToToolchains_WhenToolchainsXmlDoesNotExist_ShouldCreateNewFile() throws Exception {
        Path jdkHome = tempDir.resolve("jdk-17");
        Files.createDirectories(jdkHome);

        ToolchainPrivate result = ToolchainXmlHelper.addJDKToToolchains(jdkHome, JDK_VERSION, JDK_VENDOR);

        thenToolchainHasExpectedValues(result, "jdk",  JDK_VERSION, JDK_VENDOR, jdkHome.toAbsolutePath().toString());
        
        File toolchainsFile = new File(new File(TEST_USER_HOME), ".m2/toolchains.xml");
        assertTrue(toolchainsFile.exists());
        
        Xpp3Dom toolchainsDom = Xpp3DomBuilder.build(new FileReader(toolchainsFile));
        assertEquals("toolchains", toolchainsDom.getName());
        assertEquals(1, toolchainsDom.getChildCount());
        
        Xpp3Dom toolchainDom = toolchainsDom.getChild(0);
        assertEquals("toolchain", toolchainDom.getName());
        
        thenToolchainXmlHasExpectedValues(toolchainDom, "jdk", JDK_VERSION, JDK_VENDOR, jdkHome.toAbsolutePath().toString());
    }

    @Test
    void testAddJDKToToolchains_WhenToolchainsXmlExists_ShouldAppendToExistingFile() throws Exception {
        File toolchainsFile = new File(new File(TEST_USER_HOME), ".m2/toolchains.xml");
        String existingContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <toolchains>
                <toolchain>
                    <type>jdk</type>
                    <provides>
                        <version>11</version>
                        <vendor>oracle</vendor>
                    </provides>
                    <configuration>
                        <jdkHome>/usr/lib/jvm/java-11-oracle</jdkHome>
                    </configuration>
                </toolchain>
            </toolchains>
            """;
        Files.write(toolchainsFile.toPath(), existingContent.getBytes());

        Path jdkHome = tempDir.resolve("jdk-17");
        Files.createDirectories(jdkHome);

        ToolchainPrivate result = ToolchainXmlHelper.addJDKToToolchains(jdkHome, JDK_VERSION, JDK_VENDOR);

        thenToolchainHasExpectedValues(result, "jdk",  JDK_VERSION, JDK_VENDOR, jdkHome.toAbsolutePath().toString());
        
        assertTrue(toolchainsFile.exists());
        
        Xpp3Dom toolchainsDom = Xpp3DomBuilder.build(new FileReader(toolchainsFile));
        assertEquals("toolchains", toolchainsDom.getName());
        assertEquals(2, toolchainsDom.getChildCount());
        
        Xpp3Dom firstToolchain = toolchainsDom.getChild(0);
        assertEquals("toolchain", firstToolchain.getName());

        thenToolchainXmlHasExpectedValues(firstToolchain,  "jdk", "11", "oracle", "/usr/lib/jvm/java-11-oracle");
        
        Xpp3Dom secondToolchain = toolchainsDom.getChild(1);
        assertEquals("toolchain", secondToolchain.getName());

        thenToolchainXmlHasExpectedValues(secondToolchain, "jdk", JDK_VERSION, JDK_VENDOR, jdkHome.toAbsolutePath().toString());
    }

    private static void thenToolchainHasExpectedValues(ToolchainPrivate result, String type, String jdkVersion, String jdkVendor, String jdkHome) {
        assertNotNull(result);
        assertInstanceOf(DefaultJavaToolChain.class, result);

        DefaultJavaToolChain javaToolChain = (DefaultJavaToolChain) result;
        assertEquals(jdkHome, javaToolChain.getJavaHome());

        ToolchainModel model = javaToolChain.getModel();
        assertEquals(type, model.getType());

        Properties provides = model.getProvides();
        assertEquals(jdkVersion, provides.getProperty("version"));
        assertEquals(jdkVendor, provides.getProperty("vendor"));

        Xpp3Dom config = (Xpp3Dom) model.getConfiguration();
        assertNotNull(config);
        assertEquals("configuration", config.getName());
        assertEquals(1, config.getChildCount());

        Xpp3Dom jdkHomeConfig = config.getChild("jdkHome");
        assertEquals(jdkHome, jdkHomeConfig.getValue());
    }

    private static void thenToolchainXmlHasExpectedValues(Xpp3Dom toolchainDom, String type, String jdkVersion, String jdkVendor, String jdkHome) {
        Xpp3Dom typeDom = toolchainDom.getChild("type");
        assertEquals(type, typeDom.getValue());

        Xpp3Dom providesDom = toolchainDom.getChild("provides");
        assertEquals(2, providesDom.getChildCount());

        Xpp3Dom versionDom = providesDom.getChild("version");
        assertEquals(jdkVersion, versionDom.getValue());

        Xpp3Dom vendorDom = providesDom.getChild("vendor");
        assertEquals(jdkVendor, vendorDom.getValue());

        Xpp3Dom configDom = toolchainDom.getChild("configuration");
        assertEquals(1, configDom.getChildCount());

        Xpp3Dom jdkHomeDom = configDom.getChild("jdkHome");
        assertEquals(jdkHome, jdkHomeDom.getValue());
    }
}
