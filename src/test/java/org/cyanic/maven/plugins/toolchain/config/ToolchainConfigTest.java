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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolchainConfigTest {

    private static final String JDK_VERSION = "17";

    private static final String JDK_VENDOR = "openjdk";

    @Test
    void testGetToolchains_ShouldReturnUnmodifiableMap() {
        Map<String, String> jdkParams = Map.of("version", JDK_VERSION);
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);
        
        ToolchainConfig config = new ToolchainConfig(toolchainMap);

        Map<String, Map<String, String>> result = config.getToolchains();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("jdk"));
        assertEquals(JDK_VERSION, result.get("jdk").get("version"));
        
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", new HashMap<>()));
    }

    @Test
    void testGetParams_ShouldReturnUnmodifiableMap() {
        Map<String, String> jdkParams = Map.of("version", JDK_VERSION, "vendor", JDK_VENDOR);
        Map<String, Map<String, String>> toolchainMap = Map.of("jdk", jdkParams);
        
        ToolchainConfig config = new ToolchainConfig(toolchainMap);

        Map<String, String> result = config.getParams("jdk");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(JDK_VERSION, result.get("version"));
        assertEquals(JDK_VENDOR, result.get("vendor"));
        
        assertThrows(UnsupportedOperationException.class, () -> result.put("test", "value"));
    }
} 