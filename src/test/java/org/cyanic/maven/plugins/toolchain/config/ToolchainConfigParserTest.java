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

package org.cyanic.maven.plugins.toolchain.config;

import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToolchainConfigParserTest {

    private static final String TOOLCHAINS_KEY = "toolchains";
    private static final String JDK_KEY = "jdk";
    private static final String TESTJDK_KEY = "testJdk";
    private static final String VERSION_KEY = "version";
    private static final String VENDOR_KEY = "vendor";

    private static final String VERSION = "17";
    private static final String TEST_VERSION = "21";
    private static final String VENDOR = "openjdk";
    private static final String TEST_VENDOR = "oracle";

    private final ToolchainConfigParser parser = new ToolchainConfigParser();

    @Test
    public void testCanConvert_WithToolchainConfigClass_ShouldReturnTrue() {
        boolean result = parser.canConvert(ToolchainConfig.class);

        assertTrue(result);
    }

    @Test
    public void testCanConvert_WithSubclassOfToolchainConfig_ShouldReturnTrue() {
        class TestToolchainConfig extends ToolchainConfig {
            TestToolchainConfig(Map<String, Map<String, String>> toolchains) {
                super(toolchains);
            }
        }

        boolean result = parser.canConvert(TestToolchainConfig.class);

        assertTrue(result);
    }

    @Test
    public void testCanConvert_WithOtherClass_ShouldReturnFalse() {
        boolean result = parser.canConvert(String.class);

        assertFalse(result);
    }

    @Test
    public void testFromConfiguration_WithValidJdkConfiguration_ShouldParseCorrectly() {
        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, generateJdkToolchainsConfig(VERSION, VENDOR), null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(1, toolchains.size());
        assertTrue(toolchains.containsKey(JDK_KEY));
        
        Map<String, String> jdkParams = toolchains.get(JDK_KEY);
        assertEquals(VERSION, jdkParams.get(VERSION_KEY));
        assertEquals(VENDOR, jdkParams.get(VENDOR_KEY));
    }

    @Test
    public void testFromConfiguration_WithMultipleToolchainTypes_ShouldParseAll() {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(TOOLCHAINS_KEY);
        XmlPlexusConfiguration jdkConfig = generateJdkConfig(JDK_KEY, VERSION, VENDOR);
        
        XmlPlexusConfiguration testJdkConfig = generateJdkConfig(TESTJDK_KEY, TEST_VERSION, TEST_VENDOR);

        config.addChild(jdkConfig);
        config.addChild(testJdkConfig);

        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, config, null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(2, toolchains.size());
        
        assertTrue(toolchains.containsKey(JDK_KEY));
        Map<String, String> jdkParams = toolchains.get(JDK_KEY);
        assertEquals(VERSION, jdkParams.get(VERSION_KEY));
        assertEquals(VENDOR, jdkParams.get(VENDOR_KEY));
        
        assertTrue(toolchains.containsKey(TESTJDK_KEY));
        Map<String, String> testJdkParams = toolchains.get(TESTJDK_KEY);
        assertEquals(TEST_VERSION, testJdkParams.get(VERSION_KEY));
        assertEquals(TEST_VENDOR, testJdkParams.get(VENDOR_KEY));
    }

    @Test
    public void testFromConfiguration_WithEmptyConfiguration_ShouldReturnEmptyConfig() {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(TOOLCHAINS_KEY);

        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, config, null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(0, toolchains.size());
    }

    @Test
    public void testFromConfiguration_WithToolchainWithoutParameters_ShouldHandleEmptyParams() {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(TOOLCHAINS_KEY);
        XmlPlexusConfiguration jdkConfig = new XmlPlexusConfiguration(JDK_KEY);
        config.addChild(jdkConfig);

        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, config, null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(1, toolchains.size());
        assertTrue(toolchains.containsKey(JDK_KEY));
        
        Map<String, String> jdkParams = toolchains.get(JDK_KEY);
        assertEquals(0, jdkParams.size());
    }

    @Test
    public void testFromConfiguration_WithNullValues_ShouldHandleNullValues() {
        XmlPlexusConfiguration config = generateJdkToolchainsConfig(null, null);

        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, config, null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(1, toolchains.size());
        
        Map<String, String> jdkParams = toolchains.get(JDK_KEY);
        assertEquals(2, jdkParams.size());
        assertNull(jdkParams.get(VERSION_KEY));
        assertNull(jdkParams.get(VENDOR_KEY));
    }

    @Test
    public void testFromConfiguration_WithDuplicateParameters_ShouldUseLastValue() {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(TOOLCHAINS_KEY);
        XmlPlexusConfiguration jdkConfig = new XmlPlexusConfiguration(JDK_KEY);
        jdkConfig.addChild(VERSION_KEY, VERSION);
        jdkConfig.addChild(VERSION_KEY, "21");
        config.addChild(jdkConfig);

        ToolchainConfig result = (ToolchainConfig) parser.fromConfiguration(null, config, null, null, null, null, null);

        assertNotNull(result);
        Map<String, Map<String, String>> toolchains = result.getToolchains();
        assertEquals(1, toolchains.size());
        
        Map<String, String> jdkParams = toolchains.get(JDK_KEY);
        assertEquals("21", jdkParams.get(VERSION_KEY));
    }

    private XmlPlexusConfiguration generateJdkToolchainsConfig(String version, String vendor) {
        XmlPlexusConfiguration config = new XmlPlexusConfiguration(TOOLCHAINS_KEY);
        XmlPlexusConfiguration jdkConfig = generateJdkConfig(JDK_KEY, version, vendor);

        config.addChild(jdkConfig);

        return config;
    }

    private XmlPlexusConfiguration generateJdkConfig(String type, String version, String vendor) {
        XmlPlexusConfiguration jdkConfig = new XmlPlexusConfiguration(type);

        jdkConfig.addChild(VERSION_KEY, version);
        jdkConfig.addChild(VENDOR_KEY, vendor);

        return jdkConfig;
    }
} 